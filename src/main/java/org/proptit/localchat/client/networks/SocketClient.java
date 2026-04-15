package org.proptit.localchat.client.networks;


import javafx.scene.control.Alert;
import org.proptit.localchat.client.controller.LoginController;
import org.proptit.localchat.client.controller.MainWindowController;
import org.proptit.localchat.common.enums.TypeDataPacket;
import javafx.application.Platform;
import org.proptit.localchat.common.models.DataPacket;
import org.proptit.localchat.common.models.User;
import org.proptit.localchat.common.models.call.CallSignal;
import org.proptit.localchat.common.models.message.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

public class SocketClient implements Runnable {
    private String host;
    private int port;
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private User user;
    private boolean isRunning = true;
    private MainWindowController controller;

    private LoginController loginController;

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


            if (user != null) {
                sendData(user);
            }

            Object response;
            while (isRunning && (response = in.readObject()) != null) {
                handleServerPacket((DataPacket) response);
            }
        }
        catch (IOException | ClassNotFoundException e) {
            System.err.println("Can not connect to Server: " + e.getMessage());
        } finally {
            closeEverything();
        }
    }

        private void handleServerPacket(DataPacket data) {
        switch (data.getTypeDataPacket()) {
            case TypeDataPacket.LOGIN_SUCCESS:
                this.user = (User) data.getData();

                loginController.handleLoginResult(true, this.user);
                break;
            case TypeDataPacket.LOGIN_FAILED:
                loginController.handleLoginResult(false, data.getData());
                break;
            case TypeDataPacket.CHAT_MESSAGE:
                Message msg = (Message) data.getData();
                if (controller != null) {
                    Platform.runLater(() -> {
                        controller.receiveMessage(msg);
                    });
                } else {
                    System.out.println("The interface is not ready yet: " + msg.toString());
                }
                break;
            case TypeDataPacket.RETURN_ALL_USERS:
                controller.updateMemberList((List<User>) data.getData());
                break;
            case TypeDataPacket.RETURN_ONLINE_USERS:
                if (controller != null) {
                    controller.updateOnlinePeople((List<User>) data.getData());
                }
                break;
            case TypeDataPacket.ADD_ACCOUNT_SUCCESS:
                User user = (User)data.getData();
                Platform.runLater(() -> {
                    controller.addMemberToUI(user);
                });
                break;

            case TypeDataPacket.ADD_ACCOUNT_FAILURE:
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Create account failure...");
                    alert.setHeaderText(null);
                    alert.setGraphic(null);
                    alert.showAndWait();
                });
                break;
            case TypeDataPacket.CALL_SIGNAL:
                if (controller != null) {
                    CallSignal signal = (CallSignal) data.getData();
                    Platform.runLater(() -> controller.receiveCallSignal(signal));
                }
                break;

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

    public void setLoginController(LoginController loginController) {
        this.loginController = loginController;
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

    public void setController(MainWindowController controller) {
        this.controller = controller;
    }


}
