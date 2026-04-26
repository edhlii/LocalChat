package org.proptit.localchat.server.config;

import java.io.File;

public class StorageConfig {

    private static final String PROJECT_PATH = System.getProperty("user.dir");


    public static final String UPLOAD_DIR = PROJECT_PATH + File.separator + "server_data" + File.separator + "uploads" + File.separator;

    static {

        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                System.out.println(">>> [STORAGE] Đã tạo thư mục lưu trữ thành công tại: " + UPLOAD_DIR);
            } else {
                System.err.println(">>> [STORAGE] LỖI: Không thể tạo thư mục lưu trữ!");
            }
        } else {
            System.out.println(">>> [STORAGE] Thư mục lưu trữ đã sẵn sàng tại: " + UPLOAD_DIR);
        }
    }
}