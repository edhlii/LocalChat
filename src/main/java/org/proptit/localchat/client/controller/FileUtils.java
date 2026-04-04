package org.proptit.localchat.client.controller;

import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class FileUtils {
    public static byte[] chooseImageAndReadBytes(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh để gửi");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            try {
                return Files.readAllBytes(selectedFile.toPath());
            } catch (IOException e) {
                System.err.println("Lỗi khi đọc file: " + e.getMessage());
            }
        }
        return null;
    }

    public static Image bytesToImage(byte[] imageData) {
        if (imageData == null || imageData.length == 0) return null;
        return new Image(new ByteArrayInputStream(imageData));
    }

    public static String getFileUrl(File file) {
        return file.toURI().toString();
    }

    
}
