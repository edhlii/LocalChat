package org.proptit.localchat.server.services;

import org.proptit.localchat.common.enums.TypeDataPacket;
import org.proptit.localchat.common.models.DataPacket;
import org.proptit.localchat.common.models.User;
import org.proptit.localchat.server.controller.ClientHandler;
import org.proptit.localchat.server.dao.UserDao;

public class AuthService {
    private UserDao userDao;

    public AuthService(UserDao userDao)
    {
        this.userDao = userDao;
    }

    public void handleLogin(ClientHandler handler, User loginInfo) {

        User validatedUser = userDao.getUserByLogin(loginInfo.getUserame(), loginInfo.getPassword());

        if (validatedUser != null) {
            handler.setUser(validatedUser);
            handler.sendMessage(new DataPacket(TypeDataPacket.LOGIN_SUCCESS, validatedUser));
        } else {
            handler.sendMessage(new DataPacket(TypeDataPacket.LOGIN_FAILED, null));
        }
    }
}