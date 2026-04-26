package org.proptit.localchat.server.services;

import org.proptit.localchat.server.config.StorageConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class StorageFileService {


    public String saveFile(byte[] fileData, String originalName) throws IOException {

        String extension = "";
        int i = originalName.lastIndexOf('.');
        if (i > 0) {
            extension = originalName.substring(i);
        }


        String fileName = UUID.randomUUID().toString() + extension;


        Path filePath = Paths.get(StorageConfig.UPLOAD_DIR + fileName);


        Files.write(filePath, fileData);

        System.out.println(">>> [FILE] Đã lưu file thành công: " + fileName);


        return fileName;
    }
}