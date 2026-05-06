package org.proptit.localchat.client.controller;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.embed.swing.SwingFXUtils;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.Alert;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Manages file operations for chat (download, upload, save)
 */
public class ChatFileManager {
    
    /**
     * Opens file chooser and reads file bytes
     */
    public static byte[] chooseAndReadFile(Stage stage) throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn file");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("All Files", "*.*"),
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"),
            new FileChooser.ExtensionFilter("Document Files", "*.pdf", "*.doc", "*.docx", "*.txt")
        );
        
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            return Files.readAllBytes(file.toPath());
        }
        return null;
    }
    
    /**
     * Gets file name from file path
     */
    public static String getFileName(File file) {
        return file.getName();
    }
    
    /**
     * Gets file extension
     */
    public static String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }
    
    /**
     * Checks if file is an image based on extension
     */
    public static boolean isImage(String fileName) {
        String ext = getFileExtension(fileName);
        return ext.matches("(png|jpg|jpeg|gif)");
    }
    
    /**
     * Saves image to disk
     */
    public static void saveImage(Image image, Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Lưu ảnh tải về");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("PNG Files", "*.png"),
            new FileChooser.ExtensionFilter("JPG Files", "*.jpg")
        );
        fileChooser.setInitialFileName("downloaded_image.png");
        
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
                showSuccess("Đã lưu ảnh thành công!");
            } catch (IOException ex) {
                showError("Lỗi khi lưu ảnh!");
                ex.printStackTrace();
            }
        }
    }
    
    /**
     * Saves file to disk
     */
    public static void saveFile(String fileName, byte[] fileData, Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Lưu file");
        fileChooser.setInitialFileName(fileName);
        
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                Files.write(file.toPath(), fileData);
                showSuccess("Đã lưu file thành công!");
            } catch (IOException ex) {
                showError("Lỗi khi lưu file!");
                ex.printStackTrace();
            }
        }
    }
    
    /**
     * Shows success alert
     */
    private static void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setHeaderText(null);
        alert.show();
    }
    
    /**
     * Shows error alert
     */
    private static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(null);
        alert.show();
    }
}
