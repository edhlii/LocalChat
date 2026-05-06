package org.proptit.localchat.client.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.proptit.localchat.common.models.User;

import java.io.ByteArrayInputStream;

/**
 * Builds message display components for the chat UI
 */
public class ChatMessageBuilder {
    
    /**
     * Creates a container for a single message (text, image, or file)
     */
    public static HBox buildMessageContainer(Node content, User sender, boolean isMe, String time) {
        VBox messageGroup = new VBox(ChatUiConstants.SPACING_SMALL);
        
        Label timeLabel = new Label(isMe ? time : (sender.getNickname() + " | " + time));
        timeLabel.setStyle(ChatUiConstants.STYLE_TIME_LABEL);
        
        messageGroup.getChildren().addAll(timeLabel, content);
        messageGroup.setAlignment(isMe ? Pos.TOP_RIGHT : Pos.TOP_LEFT);
        messageGroup.setFillWidth(false);
        
        HBox container = new HBox(ChatUiConstants.SPACING_LARGE);
        container.setPadding(new Insets(ChatUiConstants.PADDING_SMALL, ChatUiConstants.PADDING_XLARGE, 
                                        ChatUiConstants.PADDING_SMALL, ChatUiConstants.PADDING_XLARGE));
        
        if (isMe) {
            container.getChildren().add(messageGroup);
            container.setAlignment(Pos.CENTER_RIGHT);
        } else {
            StackPane avatarPane = ChatAvatarManager.createAvatarPane(sender, ChatUiConstants.AVATAR_SIZE_SMALL);
            container.getChildren().addAll(avatarPane, messageGroup);
            container.setAlignment(Pos.CENTER_LEFT);
        }
        
        return container;
    }
    
    /**
     * Creates a text message node with styling
     */
    public static Label buildTextMessage(String text, boolean isMe) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(ChatUiConstants.MESSAGE_MAX_WIDTH);
        label.setFont(Font.font("System", ChatUiConstants.FONT_SIZE_LARGE));
        label.setStyle(isMe ? ChatUiConstants.STYLE_MESSAGE_SENT : ChatUiConstants.STYLE_MESSAGE_RECEIVED);
        
        // Add copy context menu
        ContextMenu contextMenu = new ContextMenu();
        MenuItem copyItem = new MenuItem("Copy tin nhắn");
        copyItem.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(label.getText());
            clipboard.setContent(content);
        });
        contextMenu.getItems().add(copyItem);
        label.setContextMenu(contextMenu);
        
        return label;
    }
    
    /**
     * Creates an image display node
     */
    public static ImageView buildImageMessage(byte[] imageData) {
        Image image = new Image(new ByteArrayInputStream(imageData));
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(ChatUiConstants.IMAGE_DISPLAY_WIDTH);
        imageView.setPreserveRatio(true);
        return imageView;
    }
    
    /**
     * Creates a file display node with download button
     */
    public static HBox buildFileMessage(String fileName, byte[] fileData, String serverUUID, 
                                       ChatFileDownloadListener listener) {
        HBox fileBox = new HBox(ChatUiConstants.SPACING_LARGE);
        fileBox.setAlignment(Pos.CENTER_LEFT);
        fileBox.setStyle(ChatUiConstants.STYLE_FILE_BOX);
        
        Label fileNameLabel = new Label(fileName);
        fileNameLabel.setWrapText(true);
        fileNameLabel.setMaxWidth(200);
        fileNameLabel.setStyle(ChatUiConstants.STYLE_FILE_NAME);
        
        Label downloadBtn = createDownloadButton(fileName, fileData, serverUUID, listener);
        
        fileBox.getChildren().addAll(fileNameLabel, downloadBtn);
        return fileBox;
    }
    
    /**
     * Creates download button for files
     */
    private static Label createDownloadButton(String fileName, byte[] fileData, String serverUUID,
                                             ChatFileDownloadListener listener) {
        Label btn = new Label("Tải về");
        btn.setStyle(ChatUiConstants.STYLE_BUTTON_DOWNLOAD);
        btn.setCursor(javafx.scene.Cursor.HAND);
        
        btn.setOnMouseClicked(e -> {
            if (fileData != null) {
                listener.onFileReady(fileName, fileData);
            } else {
                btn.setText("Đang lấy...");
                btn.setDisable(true);
                listener.onRequestDownload(serverUUID, btn);
            }
        });
        
        return btn;
    }
    
    /**
     * Adds save image context menu to ImageView
     */
    public static void addImageSaveMenu(ImageView imageView, javafx.stage.Stage stage) {
        ContextMenu imageMenu = new ContextMenu();
        MenuItem saveImageItem = new MenuItem("Tải ảnh xuống");
        saveImageItem.setOnAction(e -> ChatFileManager.saveImage(imageView.getImage(), stage));
        imageMenu.getItems().add(saveImageItem);
        
        imageView.setOnContextMenuRequested(e -> {
            imageMenu.show(imageView, e.getScreenX(), e.getScreenY());
        });
    }
    
    /**
     * Listener interface for file download actions
     */
    public interface ChatFileDownloadListener {
        void onFileReady(String fileName, byte[] fileData);
        void onRequestDownload(String serverUUID, Label button);
    }
}
