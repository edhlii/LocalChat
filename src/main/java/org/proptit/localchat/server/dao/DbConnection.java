package org.proptit.localchat.server.dao;


import org.proptit.localchat.server.utils.ServerLogger;

import java.sql.Connection;
import java.sql.DriverManager;

public class DbConnection {
    private static Connection conn = null;

    public static Connection openConnection() {
        try {
            if (conn == null || conn.isClosed()) {
                Class.forName(DbConfig.driver);
                conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
                ServerLogger.info("Database", "Connection established successfully.");
            }
        } catch (Exception e) {
            ServerLogger.error("Database", "Connection failed: " + e.getMessage());
            e.printStackTrace();
        }
        return conn;
    }
}