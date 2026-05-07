package org.proptit.localchat.server.config;

import org.proptit.localchat.server.utils.ServerLogger;

import java.io.File;

public class StorageConfig {
    private static final String PROJECT_PATH = System.getProperty("user.dir");
    public static final String UPLOAD_DIR = PROJECT_PATH + File.separator + "server_data" + File.separator + "uploads" + File.separator;

    static {
        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                ServerLogger.info("Storage", "Storage directory created at: " + UPLOAD_DIR);
            } else {
                ServerLogger.error("Storage", "Could not create storage directory!");
            }
        } else {
            ServerLogger.info("Storage", "Storage system is ready at: " + UPLOAD_DIR);
        }
    }
}