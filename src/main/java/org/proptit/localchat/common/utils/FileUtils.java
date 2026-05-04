package org.proptit.localchat.common.utils;

import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class FileUtils {
    public static File chooseFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn file để gửi");
        return fileChooser.showOpenDialog(stage);
    }

    public static Image bytesToImage(byte[] imageData) {
        if (imageData == null || imageData.length == 0) return null;
        return new Image(new ByteArrayInputStream(imageData));
    }

    public static String getFileUrl(File file) {
        return file.toURI().toString();
    }


}
