package org.proptit.localchat;

import org.proptit.localchat.common.models.User;
import org.proptit.localchat.server.dao.UserDao;

public class Main {
    public static void main(String[] args)
    {
        UserDao dao = new UserDao();


        User u = dao.getUserByLogin("ducanh", "123");

        if (u != null) {
            System.out.println("test connect DB ok");
        }
        else
        {
            System.out.println("that bai");
        }

    }
}
