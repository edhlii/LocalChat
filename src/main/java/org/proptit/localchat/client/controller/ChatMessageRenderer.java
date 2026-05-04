package org.proptit.localchat.client.controller;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;

import org.proptit.localchat.client.networks.SocketClient;
import org.proptit.localchat.common.models.ChatGroup;
import org.proptit.localchat.common.models.User;
import org.proptit.localchat.common.models.message.Message;
import org.proptit.localchat.common.models.message.TextMessage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

final class ChatMessageRenderer {
    private static final String NO_ONE_ONLINE = "No one online";
    private static final String NO_CONVERSATIONS = "No conversations";
    private final VBox vboxMessage;
    private final ScrollPane scrollPane;
    private final SocketClient client;
    private final Map<String, Button> pendingFileButtons = new HashMap<>();

    ChatMessageRenderer(VBox vboxMessage, ScrollPane scrollPane, SocketClient client) {
        this.vboxMessage = vboxMessage;
        this.scrollPane = scrollPane;
        this.client = client;
    }

    void setupListViewCustomCells(ListView<String> lvOnlinePeople,
                                  ListView<String> lvChatList,
                                  Map<String, User> conversationUserMap,
                                  Map<String, ChatGroup> conversationGroupMap,
                                  Set<Integer> usersWithNewMessages,
                                  Set<Integer> onlineUserIds,
                                  BooleanSupplier groupModeSupplier) {
        if (lvOnlinePeople != null) {
            lvOnlinePeople.setCellFactory(param -> new ListCell<String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null || item.equals(NO_ONE_ONLINE)) {
                        setGraphic(null);
                        setText(null);
                        setStyle("-fx-background-color: transparent;");
                        return;
                    }

                    VBox root = new VBox(5);
                    root.setAlignment(Pos.CENTER);
                    root.setPrefWidth(75);

                    StackPane avatarContainer = new StackPane();
                    avatarContainer.setMaxSize(52, 52);

                    Circle avatarCircle = new Circle(26, Color.web("#2A3042"));
                    avatarCircle.setStroke(Color.WHITE);
                    avatarCircle.setStrokeWidth(2);

                    String name = item.contains("(@") ? item.substring(0, item.indexOf("(@")).trim() : item;
                    User user = conversationUserMap.get(item);

                    if (user != null && user.getAvatar() != null && user.getAvatar().length != 0) {
                        try {
                            Image img = new Image(new ByteArrayInputStream(user.getAvatar()));
                            if (!img.isError()) {
                                avatarCircle.setFill(new javafx.scene.paint.ImagePattern(img));
                                avatarContainer.getChildren().add(avatarCircle);
                            } else {
                                setDefaultAvatar(avatarContainer, avatarCircle, name, 22);
                            }
                        } catch (Exception ex) {
                            setDefaultAvatar(avatarContainer, avatarCircle, name, 22);
                        }
                    } else {
                        setDefaultAvatar(avatarContainer, avatarCircle, name, 22);
                    }

                    Circle onlineDot = new Circle(7, Color.web("#23A559"));
                    onlineDot.setStroke(Color.web("#0B0F19"));
                    onlineDot.setStrokeWidth(2.5);
                    StackPane.setAlignment(onlineDot, Pos.BOTTOM_RIGHT);
                    avatarContainer.getChildren().add(onlineDot);

                    String nickname = item.contains("(@") ? item.substring(0, item.indexOf("(@")).trim() : item;
                    if (nickname.length() > 10) {
                        nickname = nickname.substring(0, 9) + "...";
                    }

                    Label nickLabel = new Label(nickname);
                    nickLabel.setTextFill(Color.web("#E4E6EB"));
                    nickLabel.setFont(Font.font("System", 12));
                    nickLabel.setAlignment(Pos.CENTER);

                    root.getChildren().addAll(avatarContainer, nickLabel);
                    setGraphic(root);
                    setText(null);
                }
            });
        }

        if (lvChatList != null) {
            lvChatList.setCellFactory(param -> new ListCell<String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null || item.equals(NO_CONVERSATIONS)) {
                        setGraphic(null);
                        setText(null);
                        setTooltip(null);
                        return;
                    }

                    if (item.equals(ChatConversationManager.ANNOUNCEMENT_LABEL)) {
                        HBox root = new HBox(12);
                        root.setAlignment(Pos.CENTER_LEFT);
                        root.setPadding(new Insets(8, 12, 8, 12));

                        Label icon = new Label("\uD83D\uDCE2");
                        icon.setStyle("-fx-font-size: 20px; -fx-text-fill: #FFFFFF;");

                        VBox textInfo = new VBox(2);
                        textInfo.setAlignment(Pos.CENTER_LEFT);

                        Label nameLbl = new Label("Thông báo chung");
                        nameLbl.setTextFill(javafx.scene.paint.Color.web("#E67E22"));
                        nameLbl.setFont(javafx.scene.text.Font.font("System", FontWeight.BOLD, 14));

                        textInfo.getChildren().add(nameLbl);

                        if (usersWithNewMessages.contains(0)) {
                            Label newMsgNotify = new Label("Có tin nhắn mới");
                            newMsgNotify.setFont(javafx.scene.text.Font.font("System", FontWeight.BOLD, 11));
                            newMsgNotify.setTextFill(javafx.scene.paint.Color.WHITE);
                            textInfo.getChildren().add(newMsgNotify);
                        }

                        root.getChildren().addAll(icon, textInfo);
                        setGraphic(root);
                        setText(null);
                        return;
                    }

                    HBox root = new HBox(12);
                    root.setAlignment(Pos.CENTER_LEFT);
                    root.setPadding(new Insets(8, 12, 8, 12));

                    StackPane avatarStack = new StackPane();
                    Circle avatarCircle = new Circle(20, Color.web("#2A3042"));
                    avatarCircle.setStroke(Color.WHITE);
                    avatarCircle.setStrokeWidth(1);

                    VBox textInfo = new VBox(2);
                    textInfo.setAlignment(Pos.CENTER_LEFT);

                    if (groupModeSupplier.getAsBoolean()) {
                        ChatGroup group = conversationGroupMap.get(item);
                        String name = item.contains("@") ? item.substring(item.indexOf("@") + 1).trim() : item;

                        setDefaultAvatar(avatarStack, avatarCircle, name, 14);

                        Label nameLbl = new Label(name);
                        nameLbl.setTextFill(Color.WHITE);
                        nameLbl.setFont(Font.font("System", FontWeight.BOLD, 14));

                        if (group != null && usersWithNewMessages.contains(-group.getId())) {
                            Label newMsgNotify = new Label("Có tin nhắn mới");
                            newMsgNotify.setFont(Font.font("System", FontWeight.BOLD, 11));
                            newMsgNotify.setTextFill(Color.WHITE);
                            textInfo.getChildren().add(newMsgNotify);
                        }

                        textInfo.getChildren().add(0, nameLbl);
                    } else {
                        String name = item.contains("(@") ? item.substring(0, item.indexOf("(@")).trim() : item;
                        User user = conversationUserMap.get(item);

                        if (user != null && user.getAvatar() != null && user.getAvatar().length != 0) {
                            try {
                                Image img = new Image(new ByteArrayInputStream(user.getAvatar()));
                                if (!img.isError()) {
                                    avatarCircle.setFill(new javafx.scene.paint.ImagePattern(img));
                                    avatarStack.getChildren().clear();
                                    avatarStack.getChildren().add(avatarCircle);
                                } else {
                                    setDefaultAvatar(avatarStack, avatarCircle, name, 14);
                                }
                            } catch (Exception ex) {
                                setDefaultAvatar(avatarStack, avatarCircle, name, 14);
                            }
                        } else {
                            setDefaultAvatar(avatarStack, avatarCircle, name, 14);
                        }

                        if (user != null && onlineUserIds.contains(user.getId())) {
                            Circle onlineDot = new Circle(6, Color.web("#23A559"));
                            onlineDot.setStroke(Color.web("#1E2435"));
                            onlineDot.setStrokeWidth(2);
                            StackPane.setAlignment(onlineDot, Pos.BOTTOM_RIGHT);
                            avatarStack.getChildren().add(onlineDot);
                        }

                        Label nameLbl = new Label(name);
                        nameLbl.setTextFill(Color.WHITE);
                        nameLbl.setFont(Font.font("System", FontWeight.BOLD, 14));
                        textInfo.getChildren().add(nameLbl);

                        if (user != null && usersWithNewMessages.contains(user.getId())) {
                            Label newMsgNotify = new Label("Có tin nhắn mới");
                            newMsgNotify.setFont(Font.font("System", FontWeight.BOLD, 11));
                            newMsgNotify.setTextFill(javafx.scene.paint.Color.WHITE);
                            textInfo.getChildren().add(newMsgNotify);
                            nameLbl.setTextFill(Color.web("#AD7BFF"));
                        }

                        if (user != null) {
                            Tooltip tip = new Tooltip(user.getUsername());
                            tip.setShowDelay(javafx.util.Duration.millis(200));
                            setTooltip(tip);
                        }
                    }

                    root.getChildren().addAll(avatarStack, textInfo);
                    setGraphic(root);
                    setText(null);
                }
            });
        }
    }

    void clearMessageArea() {
        if (vboxMessage != null) {
            vboxMessage.getChildren().clear();
        }
    }

    void loadHistory(List<Message> history, User me) {
        clearMessageArea();
        if (history == null) {
            return;
        }

        for (Message msg : history) {
            boolean isMe = msg.getSender().getId().equals(me.getId());
            if (msg.getTypeMessage() == org.proptit.localchat.common.enums.TypeMessage.TEXT) {
                addMessageToScreen(msg.getContent(), isMe, msg.getSentAt());
            } else if (msg.getTypeMessage() == org.proptit.localchat.common.enums.TypeMessage.IMAGE) {
                ImageView imageView = new ImageView();
                imageView.setFitWidth(250);
                imageView.setPreserveRatio(true);
                addImageToScreen(imageView, isMe, msg.getSentAt(), msg.getSender());
                client.sendRequestDownload(msg.getContent(), imageView);
            } else {
                addFileToScreen(msg.getContent(), msg.getFileName(), null, isMe, msg.getSentAt(), msg.getSender());
            }
        }
    }

    void addMessageToScreen(String text, boolean isMe, String time) {
        Label lblMessage = new Label(text);
        lblMessage.setWrapText(true);
        lblMessage.setMaxWidth(400);
        lblMessage.setMinHeight(Region.USE_PREF_SIZE);
        lblMessage.setFont(Font.font("System", 16));

        Label lblTime = new Label(time);
        lblTime.getStyleClass().add("chat-time");

        lblMessage.setStyle(isMe
                ? "-fx-background-color: #AD7BFF; -fx-text-fill: white; -fx-background-radius: 15px; -fx-padding: 8px 12px;"
                : "-fx-background-color: #1E2435; -fx-text-fill: white; -fx-background-radius: 15px; -fx-padding: 8px 12px;");

        VBox messageGroup = new VBox(3);
        messageGroup.setFillWidth(false);
        messageGroup.getChildren().add(lblTime);
        messageGroup.setAlignment(isMe ? Pos.TOP_RIGHT : Pos.TOP_LEFT);
        messageGroup.getChildren().add(lblMessage);

        HBox hboxContainer = new HBox(messageGroup);
        hboxContainer.setPadding(new Insets(5, 10, 5, 10));
        hboxContainer.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        vboxMessage.getChildren().add(hboxContainer);
        scrollPane.setVvalue(1.0);
    }

    void addImageToScreen(ImageView imageView, boolean isMe, String time, User sender) {
        imageView.setFitWidth(250);
        imageView.setPreserveRatio(true);

        Label lblTime = new Label(isMe ? time : (sender.getNickname() + " | " + time));
        lblTime.getStyleClass().add("chat-time");

        ContextMenu imageMenu = new ContextMenu();
        MenuItem saveImageItem = new MenuItem("Tải ảnh xuống");
        saveImageItem.setOnAction(e -> {
            if (imageView.getImage() == null) {
                return;
            }

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Lưu ảnh tải về");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("PNG Files", "*.png"),
                    new FileChooser.ExtensionFilter("JPG Files", "*.jpg"));
            fileChooser.setInitialFileName("downloaded_image.png");

            Stage stage = (Stage) imageView.getScene().getWindow();
            File file = fileChooser.showSaveDialog(stage);
            if (file == null) {
                return;
            }

            try {
                ImageIO.write(SwingFXUtils.fromFXImage(imageView.getImage(), null), "png", file);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Đã lưu ảnh thành công!");
                alert.setHeaderText(null);
                alert.show();
            } catch (IOException ex) {
                ex.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR, "Lỗi khi lưu ảnh!");
                alert.setHeaderText(null);
                alert.show();
            }
        });
        imageMenu.getItems().add(saveImageItem);
        imageView.setOnContextMenuRequested(e -> imageMenu.show(imageView, e.getScreenX(), e.getScreenY()));

        VBox messageGroup = new VBox(3);
        HBox hboxContainer = new HBox(10);
        hboxContainer.setPadding(new Insets(5, 10, 5, 10));

        if (!isMe) {
            StackPane avatarPane = new StackPane();
            Circle avatarCircle = new Circle(16, Color.web("#2A3042"));
            avatarCircle.setStroke(Color.WHITE);
            avatarCircle.setStrokeWidth(1);

            if (sender != null && sender.getAvatar() != null && sender.getAvatar().length > 0) {
                Image img = new Image(new ByteArrayInputStream(sender.getAvatar()));
                avatarCircle.setFill(new javafx.scene.paint.ImagePattern(img));
                avatarPane.getChildren().add(avatarCircle);
            } else {
                setDefaultAvatar(avatarPane, avatarCircle, sender.getNickname(), 12);
            }

            messageGroup.getChildren().addAll(lblTime, imageView);
            messageGroup.setAlignment(Pos.TOP_LEFT);
            hboxContainer.getChildren().addAll(avatarPane, messageGroup);
            hboxContainer.setAlignment(Pos.CENTER_LEFT);
        } else {
            messageGroup.getChildren().addAll(lblTime, imageView);
            messageGroup.setAlignment(Pos.TOP_RIGHT);
            hboxContainer.getChildren().add(messageGroup);
            hboxContainer.setAlignment(Pos.CENTER_RIGHT);
        }

        vboxMessage.getChildren().add(hboxContainer);
        scrollPane.setVvalue(1.0);
    }

    void addFileToScreen(String serverUUID, String fileName, byte[] fileData, boolean isMe, String time, User sender) {
        Label lblTime = new Label(isMe ? time : (sender.getNickname() + " | " + time));
        lblTime.setStyle("-fx-font-size: 10px; -fx-text-fill: #919191;");

        HBox fileBox = new HBox(10);
        fileBox.setAlignment(Pos.CENTER_LEFT);
        fileBox.setStyle("-fx-background-color: #1E2435; -fx-background-radius: 10px; -fx-padding: 10px; -fx-border-color: #2A3042; -fx-border-radius: 10px;");

        Label lblFileName = new Label(fileName);
        lblFileName.setWrapText(true);
        lblFileName.setMaxWidth(200);
        lblFileName.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");

        Button btnDownload = new Button("Tải về");
        btnDownload.setStyle("-fx-background-color: #AD7BFF; -fx-text-fill: black; -fx-background-radius: 5px; -fx-cursor: hand;");
        btnDownload.setOnAction(e -> {
            if (fileData != null) {
                downloadFile(fileName, fileData);
            } else {
                btnDownload.setText("Đang lấy...");
                btnDownload.setDisable(true);
                pendingFileButtons.put(serverUUID, btnDownload);
                client.sendData(new org.proptit.localchat.common.models.DataPacket(org.proptit.localchat.common.enums.TypeDataPacket.DOWNLOAD_FILE_REQUEST, serverUUID));
            }
        });

        fileBox.getChildren().addAll(lblFileName, btnDownload);

        VBox messageGroup = new VBox(3);
        HBox hboxContainer = new HBox(10);
        hboxContainer.setPadding(new Insets(5, 10, 5, 10));

        if (!isMe) {
            StackPane avatarPane = new StackPane();
            Circle avatarCircle = new Circle(16, Color.web("#2A3042"));
            avatarCircle.setStroke(Color.WHITE);
            avatarCircle.setStrokeWidth(1);

            if (sender != null && sender.getAvatar() != null && sender.getAvatar().length > 0) {
                Image img = new Image(new ByteArrayInputStream(sender.getAvatar()));
                avatarCircle.setFill(new javafx.scene.paint.ImagePattern(img));
                avatarPane.getChildren().add(avatarCircle);
            } else {
                setDefaultAvatar(avatarPane, avatarCircle, sender.getNickname(), 12);
            }

            messageGroup.getChildren().addAll(lblTime, fileBox);
            messageGroup.setAlignment(Pos.TOP_LEFT);
            hboxContainer.getChildren().addAll(avatarPane, messageGroup);
            hboxContainer.setAlignment(Pos.CENTER_LEFT);
        } else {
            messageGroup.getChildren().addAll(lblTime, fileBox);
            messageGroup.setAlignment(Pos.TOP_RIGHT);
            hboxContainer.getChildren().add(messageGroup);
            hboxContainer.setAlignment(Pos.CENTER_RIGHT);
        }

        vboxMessage.getChildren().add(hboxContainer);
        scrollPane.setVvalue(1.0);
    }

    void handleFileDownloadResponse(String fileName, byte[] fileData) {
        Button btn = pendingFileButtons.get(fileName);
        if (btn != null) {
            btn.setText("Tải về");
            btn.setDisable(false);
            pendingFileButtons.remove(fileName);
            downloadFile(fileName, fileData);
        }
    }

    private void downloadFile(String fileName, byte[] fileData) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Lưu file");
        fileChooser.setInitialFileName(fileName);

        Stage stage = (Stage) vboxMessage.getScene().getWindow();
        File saveFile = fileChooser.showSaveDialog(stage);
        if (saveFile == null) {
            return;
        }

        try {
            Files.write(saveFile.toPath(), fileData);
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Đã lưu file thành công!");
            alert.setHeaderText(null);
            alert.show();
        } catch (IOException ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Lỗi khi lưu file!");
            alert.setHeaderText(null);
            alert.show();
        }
    }

    private void setDefaultAvatar(StackPane stack, Circle circle, String name, int fontSize) {
        stack.getChildren().clear();
        circle.setFill(Color.web("#2A3042"));
        String initial = (name == null || name.isEmpty()) ? "?" : name.substring(0, 1).toUpperCase();
        Label initialLabel = new Label(initial);
        initialLabel.setTextFill(Color.WHITE);
        initialLabel.setFont(Font.font("System", FontWeight.BOLD, fontSize));
        stack.getChildren().addAll(circle, initialLabel);
    }
}