package org.proptit.localchat.client.networks;

import org.proptit.localchat.client.controller.LoginController;
import org.proptit.localchat.common.enums.TypeDataPacket;
import org.proptit.localchat.common.models.DataPacket;
import org.proptit.localchat.common.models.User;

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

        } catch (IOException | ClassNotFoundException e) {
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

}
