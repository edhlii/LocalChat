package org.proptit.localchat.server.dao;


import java.sql.Connection;
import java.sql.DriverManager;

public class DbConnection {
    private static Connection conn = null;

    public static Connection openConnection() {
        try {
            if (conn == null || conn.isClosed()) {
                Class.forName(DbConfig.driver);

                conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            }
        } catch (Exception e) {
            System.err.println("Error connect Database!");
            e.printStackTrace();
        }
        return conn;
    }
}