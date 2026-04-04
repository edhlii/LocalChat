package org.proptit.localchat.server.controller;

import org.proptit.localchat.common.enums.TypeDataPacket;
import org.proptit.localchat.common.models.DataPacket;
import org.proptit.localchat.common.models.message.Message;
import org.proptit.localchat.common.models.User;
import org.proptit.localchat.server.dao.UserDao;
import org.proptit.localchat.server.networks.SocketServer;
import org.proptit.localchat.server.services.AuthService;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private SocketServer server;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private User user;

    public ClientHandler(Socket socket, SocketServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            AuthService authService = new AuthService(new UserDao());

            Object receivedData;
            while ((receivedData = in.readObject()) != null)
            {
                DataPacket data = (DataPacket)receivedData;
                switch (data.getTypeDataPacket())
                {
                    case TypeDataPacket.LOGIN_REQUEST:
                        authService.handleLogin(this, (User)data.getData());
                        break;
                    case TypeDataPacket.CHAT_MESSAGE:
                        System.out.println("hello");
                        Message msg = (Message) data.getData();
                        server.getChatService().processMessage(this, msg);
                        break;
                }
            }


        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Log: Connection lost with " + (user != null ? user.getNickname() : "unknown"));
        } finally {
            closeEverything();
        }
    }

    public void sendMessage(Object obj) {
        try {
            out.writeObject(obj);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeEverything() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public User getUser() {
        return this.user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public SocketServer getServer() {
        return server;
    }
}