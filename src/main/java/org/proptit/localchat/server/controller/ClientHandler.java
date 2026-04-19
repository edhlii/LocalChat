package org.proptit.localchat.server.controller;

import org.proptit.localchat.common.enums.TypeDataPacket;
import org.proptit.localchat.common.models.DataPacket;
import org.proptit.localchat.common.models.call.CallSignal;
import org.proptit.localchat.common.models.message.Message;
import org.proptit.localchat.common.models.User;
import org.proptit.localchat.common.utils.PasswordUtils;
import org.proptit.localchat.server.dao.UserDao;
import org.proptit.localchat.server.networks.SocketServer;
import org.proptit.localchat.server.services.AuthService;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {
    private Socket socket;
    private SocketServer server;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private User user;
    private UserDao userDao = new UserDao();

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
                        Message msg = (Message) data.getData();
                        server.getChatService().processMessage(this, msg);
                        break;
                    case TypeDataPacket.GET_ALL_USERS:
                        List<User> allUsers = userDao.getAllUsers();
                        DataPacket responseData = new DataPacket(TypeDataPacket.RETURN_ALL_USERS, allUsers);
                        sendData(responseData);
                        break;
                    case TypeDataPacket.GET_ONLINE_USERS:
                        List<User> onlineUsers = server.getOnlineUsers();
                        sendData(new DataPacket(TypeDataPacket.RETURN_ONLINE_USERS, onlineUsers));
                        break;
                    case TypeDataPacket.DELETE_USER_REQUEST:
                        int id = (int)data.getData();
                        userDao.deleteUser(id);
                        break;
                    case TypeDataPacket.ADD_USER_REQUEST:
                        User user = (User)data.getData();
                        if(userDao.findByUsername(user.getUsername()) == null)
                        {
                            user.setPassword(PasswordUtils.hashPassword(user.getPassword()));
                            userDao.addUser(user);
                            DataPacket errorPacket = new DataPacket(TypeDataPacket.ADD_ACCOUNT_SUCCESS, userDao.findByUsername(user.getUsername()));
                            sendData(errorPacket);
                        }
                        else
                        {
                            DataPacket errorPacket = new DataPacket(TypeDataPacket.ADD_ACCOUNT_FAILURE, null);
                            sendData(errorPacket);
                        }
                        break;
                    case TypeDataPacket.CALL_SIGNAL:
                        CallSignal signal = (CallSignal) data.getData();
                        server.forwardCallSignal(signal);
                        break;
                    case TypeDataPacket.UPDATE_PROFILE_REQUEST:
                        User updateUser = (User) data.getData();
                        updateUser.setPassword(PasswordUtils.hashPassword(updateUser.getPassword()));

                        boolean isUpdated = userDao.updateUser(updateUser);
                        if(isUpdated) {
                            this.user.setNickname(updateUser.getNickname());
                            this.user.setPassword(updateUser.getPassword());

                            sendData(new DataPacket(TypeDataPacket.UPDATE_PROFILE_SUCCESS, this.user));
                            server.broadcastOnlineUsers();
                        }
                        else {
                            sendData(new DataPacket(TypeDataPacket.UPDATE_PROFILE_FAILURE, "Cập nhật thất bại!"));
                        }
                        break;
                }
            }


        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Log: Connection lost with " + (user != null ? user.getNickname() : "unknown"));
        } finally {
            closeEverything();
        }
    }

    public void sendData(Object obj) {
        try {
            out.writeObject(obj);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeEverything() {
        try {
            server.removeClient(this);
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