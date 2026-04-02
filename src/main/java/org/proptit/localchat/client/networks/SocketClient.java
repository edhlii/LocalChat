package org.proptit.localchat.client.networks;

import javafx.application.Platform;
import org.proptit.localchat.client.controller.ChatWindowController;
import org.proptit.localchat.common.models.DataPacket;
import org.proptit.localchat.common.models.User;
import org.proptit.localchat.common.models.message.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SocketClient implements Runnable {
    private String host;
    private int port;
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private User user;
    private boolean isRunning = true;
    private ChatWindowController controller;

    public SocketClient(String host, int port, User user) {
        this.host = host;
        this.port = port;
        this.user = user;
    }

    @Override
    public void run() {
        try {
            socket = new Socket(host, port);

            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            out.writeObject(user);
            out.flush();

            Object response;
            while (isRunning && (response = in.readObject()) != null) {
                if (response instanceof DataPacket packet) {
                    System.out.println("CLIENT NHẬN: Đã nhận được tin nhắn từ Server và chuẩn bị vẽ lên UI!");
                    switch (packet.getTypeDataPacket()) {
                        case CHAT_MESSAGE:
                            Message msg = (Message) packet.getData();
                            if (controller != null) {
                                Platform.runLater(() -> {
                                    controller.receiveMessage(msg);
                                });
                            } else {
                                System.out.println("The interface is not ready yet: " + msg.toString());
                            }
                            break;
                        default:
                            System.out.println("Received unsupported data type: " + packet.getTypeDataPacket());
                            break;
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Can not connect to Server: " + e.getMessage());
        } finally {
            closeEverything();
        }
    }

    public void sendData(Object data) {
        try {
            if (out != null) {
                out.writeObject(data);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeEverything() {
        try {
            isRunning = false;
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setController(ChatWindowController controller) {
        this.controller = controller;
    }
}
