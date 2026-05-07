package org.proptit.localchat.server.services;

import org.proptit.localchat.common.enums.TypeDataPacket;
import org.proptit.localchat.common.models.DataPacket;
import org.proptit.localchat.common.models.User;
import org.proptit.localchat.common.utils.PasswordUtils;
import org.proptit.localchat.server.controller.ClientHandler;
import org.proptit.localchat.server.dao.MessageDao;
import org.proptit.localchat.server.dao.UserDao;
import org.proptit.localchat.server.utils.ServerLogger;

import java.util.List;

public class AuthService {
    private UserDao userDao;
    private MessageDao messageDao;


    public AuthService(UserDao userDao, MessageDao messageDao) {
        this.userDao = userDao;
        this.messageDao = messageDao;
    }

    public void handleLogin(ClientHandler handler, User loginInfo) {

        if (loginInfo == null || loginInfo.getUsername() == null || loginInfo.getPassword() == null) {
            handler.sendData(new DataPacket(TypeDataPacket.LOGIN_FAILED, null));
            ServerLogger.warn("Auth", "Login failed: Missing credentials from connection " + handler.getServer().getClients().indexOf(handler));
            return;
        }
        User validatedUser = userDao.findByUsername(loginInfo.getUsername());


        if (validatedUser != null && PasswordUtils.checkPassword(loginInfo.getPassword(), validatedUser.getPassword())) {
            handler.setUser(validatedUser);
            ServerLogger.info("Auth", "User [" + validatedUser.getUsername() + "] logged in successfully. Role: " + validatedUser.getRole());
            handler.sendData(new DataPacket(TypeDataPacket.LOGIN_SUCCESS, validatedUser));
            handler.getServer().broadcastOnlineUsers();
        } else {
            ServerLogger.warn("Auth", "Login failed for username: [" + loginInfo.getUsername() + "] - Incorrect password or account does not exist.");
            handler.sendData(new DataPacket(TypeDataPacket.LOGIN_FAILED, null));
        }
    }
}