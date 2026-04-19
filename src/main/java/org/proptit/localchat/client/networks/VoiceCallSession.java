package org.proptit.localchat.client.networks;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class VoiceCallSession {
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(16000.0f, 16, 1, true, false);
    private static final int FRAME_BYTES = 640;

    private DatagramSocket socket;
    private TargetDataLine micLine;
    private SourceDataLine speakerLine;
    private Thread captureThread;
    private Thread playbackThread;
    private volatile boolean running;
    private volatile boolean muted;

    public synchronized int open() throws SocketException, LineUnavailableException {
        if (socket != null && !socket.isClosed()) {
            return socket.getLocalPort();
        }

        socket = new DatagramSocket(0);
        socket.setSoTimeout(500);

        DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, AUDIO_FORMAT);
        micLine = (TargetDataLine) AudioSystem.getLine(micInfo);
        micLine.open(AUDIO_FORMAT);

        DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, AUDIO_FORMAT);
        speakerLine = (SourceDataLine) AudioSystem.getLine(speakerInfo);
        speakerLine.open(AUDIO_FORMAT);

        return socket.getLocalPort();
    }

    public synchronized void start(String remoteHost, int remotePort) throws IOException {
        if (running) {
            return;
        }
        if (socket == null || micLine == null || speakerLine == null) {
            throw new IllegalStateException("Voice call resources are not opened");
        }

        running = true;
        InetAddress remoteAddress = InetAddress.getByName(remoteHost);

        micLine.start();
        speakerLine.start();

        captureThread = new Thread(() -> captureLoop(remoteAddress, remotePort), "voice-capture");
        playbackThread = new Thread(this::playbackLoop, "voice-playback");
        captureThread.setDaemon(true);
        playbackThread.setDaemon(true);
        captureThread.start();
        playbackThread.start();
    }

    private void captureLoop(InetAddress remoteAddress, int remotePort) {
        try {
            byte[] buffer = new byte[FRAME_BYTES];
            while (running) {
                int bytesRead = micLine.read(buffer, 0, buffer.length);
                if (bytesRead <= 0) {
                    continue;
                }

                byte[] payload = new byte[bytesRead];
                System.arraycopy(buffer, 0, payload, 0, bytesRead);
                if (muted) {
                    for (int i = 0; i < payload.length; i++) {
                        payload[i] = 0;
                    }
                }

                DatagramPacket packet = new DatagramPacket(payload, payload.length, remoteAddress, remotePort);
                socket.send(packet);
            }
        } catch (Exception ex) {
            if (running) {
                ex.printStackTrace();
            }
        }
    }

    private void playbackLoop() {
        byte[] buffer = new byte[2048];
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                speakerLine.write(packet.getData(), 0, packet.getLength());
            } catch (SocketTimeoutException ignore) {
                // timeout helps break receive loop quickly when stopping
            } catch (IOException ex) {
                if (running) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public synchronized void stop() {
        running = false;

        if (captureThread != null) {
            captureThread.interrupt();
            captureThread = null;
        }
        if (playbackThread != null) {
            playbackThread.interrupt();
            playbackThread = null;
        }

        if (micLine != null) {
            micLine.stop();
            micLine.close();
            micLine = null;
        }
        if (speakerLine != null) {
            speakerLine.drain();
            speakerLine.stop();
            speakerLine.close();
            speakerLine = null;
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
            socket = null;
        }
    }
}
