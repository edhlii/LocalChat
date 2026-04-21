package org.proptit.localchat.client.networks;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.imageio.ImageIO;
import javax.imageio.IIOImage;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

public class ScreenShareSession {
    private static final int CHUNK_SIZE = 1200;
    private static final int HEADER_SIZE = 12;
    private static final int RECEIVE_BUFFER = CHUNK_SIZE + HEADER_SIZE;
    private static final float JPEG_QUALITY = 0.95f;
    private static final int FRAME_INTERVAL_MS = 8;

    private DatagramSocket socket;
    private Thread senderThread;
    private Thread receiverThread;

    private volatile boolean opened;
    private volatile boolean sending;

    private final AtomicInteger frameCounter = new AtomicInteger(1);

    public synchronized int open(Consumer<byte[]> onFrameReceived) throws SocketException {
        if (socket != null && !socket.isClosed()) {
            return socket.getLocalPort();
        }

        socket = new DatagramSocket(0);
        socket.setSoTimeout(500);
        opened = true;

        receiverThread = new Thread(() -> receiverLoop(onFrameReceived), "screen-share-receiver");
        receiverThread.setDaemon(true);
        receiverThread.start();

        return socket.getLocalPort();
    }

    public synchronized void startSending(String remoteHost, int remotePort) {
        if (!opened || sending || remoteHost == null || remoteHost.isBlank() || remotePort <= 0) {
            return;
        }

        sending = true;
        senderThread = new Thread(() -> senderLoop(remoteHost, remotePort), "screen-share-sender");
        senderThread.setDaemon(true);
        senderThread.start();
    }

    public synchronized void stopSending() {
        sending = false;
        if (senderThread != null) {
            senderThread.interrupt();
            senderThread = null;
        }
    }

    public synchronized void stop() {
        opened = false;
        stopSending();

        if (receiverThread != null) {
            receiverThread.interrupt();
            receiverThread = null;
        }

        if (socket != null && !socket.isClosed()) {
            socket.close();
            socket = null;
        }
    }

    private void senderLoop(String remoteHost, int remotePort) {
        try {
            Robot robot = new Robot(resolveCaptureDevice());
            Rectangle captureArea = resolveCaptureArea();
            InetAddress remoteAddress = InetAddress.getByName(remoteHost);

            while (opened && sending) {
                byte[] frame = captureFrame(robot, captureArea);
                sendFrame(remoteAddress, remotePort, frame);
                Thread.sleep(FRAME_INTERVAL_MS);
            }
        } catch (InterruptedException ignore) {
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            if (opened && sending) {
                ex.printStackTrace();
            }
        } finally {
            sending = false;
        }
    }

    private void receiverLoop(Consumer<byte[]> onFrameReceived) {
        Map<Integer, FrameAssembly> frameMap = new HashMap<>();
        byte[] packetBuffer = new byte[RECEIVE_BUFFER];

        while (opened) {
            try {
                DatagramPacket packet = new DatagramPacket(packetBuffer, packetBuffer.length);
                socket.receive(packet);

                if (packet.getLength() <= HEADER_SIZE) {
                    continue;
                }

                ByteBuffer header = ByteBuffer.wrap(packet.getData(), 0, HEADER_SIZE);
                int frameId = header.getInt();
                int totalChunks = header.getInt();
                int chunkIndex = header.getInt();

                if (totalChunks <= 0 || chunkIndex < 0 || chunkIndex >= totalChunks) {
                    continue;
                }

                int payloadLength = packet.getLength() - HEADER_SIZE;
                byte[] payload = new byte[payloadLength];
                System.arraycopy(packet.getData(), HEADER_SIZE, payload, 0, payloadLength);

                FrameAssembly assembly = frameMap.computeIfAbsent(frameId, id -> new FrameAssembly(totalChunks));
                assembly.addChunk(chunkIndex, payload);

                if (assembly.isComplete()) {
                    if (onFrameReceived != null) {
                        onFrameReceived.accept(assembly.combine());
                    }
                    frameMap.clear();
                } else {
                    frameMap.entrySet().removeIf(entry -> entry.getKey() < frameId - 2);
                }
            } catch (SocketTimeoutException ignore) {
                // timeout keeps the loop responsive while waiting for data
            } catch (IOException ex) {
                if (opened) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private GraphicsDevice resolveCaptureDevice() {
        GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] devices = graphicsEnvironment.getScreenDevices();
        if (devices.length > 0) {
            return devices[0];
        }
        return graphicsEnvironment.getDefaultScreenDevice();
    }

    private Rectangle resolveCaptureArea() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice defaultDevice = ge.getDefaultScreenDevice();
        Rectangle bounds = defaultDevice.getDefaultConfiguration().getBounds();

        if (bounds.width > 0 && bounds.height > 0) {
            return bounds;
        }

        // fallback
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        return new Rectangle(0, 0, screenSize.width, screenSize.height);
    }

    private byte[] captureFrame(Robot robot, Rectangle captureArea) throws IOException {
        BufferedImage capture = robot.createScreenCapture(captureArea);
        BufferedImage image = new BufferedImage(capture.getWidth(), capture.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.BLACK);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.drawImage(capture, 0, 0, null);
        } finally {
            graphics.dispose();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam writeParam = writer.getDefaultWriteParam();
        if (writeParam.canWriteCompressed()) {
            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            writeParam.setCompressionQuality(JPEG_QUALITY);
        }

        try (MemoryCacheImageOutputStream outputStream = new MemoryCacheImageOutputStream(baos)) {
            writer.setOutput(outputStream);
            writer.write(null, new IIOImage(image, null, null), writeParam);
            outputStream.flush();
        } finally {
            writer.dispose();
        }

        return baos.toByteArray();
    }

    private void sendFrame(InetAddress remoteAddress, int remotePort, byte[] frameBytes) throws IOException {
        if (frameBytes == null || frameBytes.length == 0) {
            return;
        }

        int frameId = frameCounter.getAndIncrement();
        int totalChunks = Math.max(1, (int) Math.ceil(frameBytes.length / (double) CHUNK_SIZE));

        for (int chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
            int offset = chunkIndex * CHUNK_SIZE;
            int chunkLength = Math.min(CHUNK_SIZE, frameBytes.length - offset);

            ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE + chunkLength);
            buffer.putInt(frameId);
            buffer.putInt(totalChunks);
            buffer.putInt(chunkIndex);
            buffer.put(frameBytes, offset, chunkLength);

            DatagramPacket packet = new DatagramPacket(buffer.array(), buffer.position(), remoteAddress, remotePort);
            socket.send(packet);
        }
    }

    private static class FrameAssembly {
        private final byte[][] chunks;
        private int receivedCount;

        private FrameAssembly(int totalChunks) {
            this.chunks = new byte[totalChunks][];
        }

        private void addChunk(int index, byte[] data) {
            if (index < 0 || index >= chunks.length || chunks[index] != null) {
                return;
            }
            chunks[index] = data;
            receivedCount++;
        }

        private boolean isComplete() {
            return receivedCount == chunks.length;
        }

        private byte[] combine() {
            int totalLength = 0;
            for (byte[] chunk : chunks) {
                if (chunk != null) {
                    totalLength += chunk.length;
                }
            }

            byte[] combined = new byte[totalLength];
            int offset = 0;
            for (byte[] chunk : chunks) {
                if (chunk == null) {
                    continue;
                }
                System.arraycopy(chunk, 0, combined, offset, chunk.length);
                offset += chunk.length;
            }
            return combined;
        }
    }
}