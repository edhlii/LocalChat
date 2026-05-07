package org.proptit.localchat.server.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServerLogger {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static void printLog(String level, String category, String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        System.out.printf("[%s] [%s] [%s] - %s%n", timestamp, level, category, message);
    }

    public static void info(String category, String message) {
        printLog("INFO", category, message);
    }

    public static void error(String category, String message) {
        printLog("ERROR", category, message);
    }

    public static void warn(String category, String message) {
        printLog("WARNING", category, message);
    }
}