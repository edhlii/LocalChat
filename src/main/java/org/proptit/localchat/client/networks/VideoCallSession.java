package org.proptit.localchat.client.networks;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
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

public class VideoCallSession {
    private static final int CHUNK_SIZE = 1200;
    private static final int HEADER_SIZE = 12;
    private static final int RECEIVE_BUFFER = CHUNK_SIZE + HEADER_SIZE;
    private static final float JPEG_QUALITY = 0.7f;
    private static final int FRAME_INTERVAL_MS = 60;

    private DatagramSocket socket;
    private Thread senderThread;
    private Thread receiverThread;
    private Webcam webcam;

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

        receiverThread = new Thread(() -> receiverLoop(onFrameReceived), "video-call-receiver");
        receiverThread.setDaemon(true);
        receiverThread.start();

        return socket.getLocalPort();
    }

    public synchronized void start(String remoteHost, int remotePort, Consumer<byte[]> onLocalFrameCaptured) throws IOException {
        if (sending) {
            return;
        }
        if (socket == null) {
            throw new IllegalStateException("Video call resources are not opened");
        }
        if (remoteHost == null || remoteHost.isBlank() || remotePort <= 0) {
            throw new IllegalArgumentException("Remote video destination is not ready");
        }

        webcam = ensureWebcamOpen();
        if (webcam == null) {
            throw new IOException("No webcam available");
        }

        sending = true;
        InetAddress remoteAddress = InetAddress.getByName(remoteHost);
        senderThread = new Thread(() -> senderLoop(remoteAddress, remotePort, onLocalFrameCaptured), "video-call-sender");
        senderThread.setDaemon(true);
        senderThread.start();
    }

    public synchronized void stopSending() {
        sending = false;
        if (senderThread != null) {
            senderThread.interrupt();
            senderThread = null;
        }
        if (webcam != null) {
            webcam.close();
            webcam = null;
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

    private Webcam ensureWebcamOpen() {
        Webcam selected = Webcam.getDefault();
        if (selected == null && !Webcam.getWebcams().isEmpty()) {
            selected = Webcam.getWebcams().get(0);
        }

        if (selected == null) {
            return null;
        }

        if (!selected.isOpen()) {
            Dimension viewSize = WebcamResolution.QVGA.getSize();
            selected.setViewSize(viewSize);
            selected.open();
        }

        return selected;
    }

    private void senderLoop(InetAddress remoteAddress, int remotePort, Consumer<byte[]> onLocalFrameCaptured) {
        try {
            while (opened && sending) {
                BufferedImage capture = webcam.getImage();
                if (capture == null) {
                    continue;
                }

                byte[] frame = encodeFrame(capture);
                if (onLocalFrameCaptured != null) {
                    onLocalFrameCaptured.accept(frame);
                }
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
                // keep the loop responsive while waiting for data
            } catch (IOException ex) {
                if (opened) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void sendFrame(InetAddress remoteAddress, int remotePort, byte[] frame) throws IOException {
        int totalChunks = (int) Math.ceil((double) frame.length / CHUNK_SIZE);
        int frameId = frameCounter.getAndIncrement();

        for (int chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
            int offset = chunkIndex * CHUNK_SIZE;
            int length = Math.min(CHUNK_SIZE, frame.length - offset);

            ByteBuffer packetBuffer = ByteBuffer.allocate(HEADER_SIZE + length);
            packetBuffer.putInt(frameId);
            packetBuffer.putInt(totalChunks);
            packetBuffer.putInt(chunkIndex);
            packetBuffer.put(frame, offset, length);

            DatagramPacket packet = new DatagramPacket(packetBuffer.array(), packetBuffer.position(), remoteAddress, remotePort);
            socket.send(packet);
        }
    }

    private byte[] encodeFrame(BufferedImage capture) throws IOException {
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

    private static class FrameAssembly {
        private final byte[][] chunks;

        private FrameAssembly(int totalChunks) {
            this.chunks = new byte[totalChunks][];
        }

        private void addChunk(int chunkIndex, byte[] payload) {
            if (chunkIndex >= 0 && chunkIndex < chunks.length) {
                chunks[chunkIndex] = payload;
            }
        }

        private boolean isComplete() {
            for (byte[] chunk : chunks) {
                if (chunk == null) {
                    return false;
                }
            }
            return true;
        }

        private byte[] combine() {
            int totalLength = 0;
            for (byte[] chunk : chunks) {
                totalLength += chunk.length;
            }

            byte[] combined = new byte[totalLength];
            int offset = 0;
            for (byte[] chunk : chunks) {
                System.arraycopy(chunk, 0, combined, offset, chunk.length);
                offset += chunk.length;
            }
            return combined;
        }
    }
}