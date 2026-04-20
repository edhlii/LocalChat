package org.proptit.localchat.client.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import org.proptit.localchat.client.networks.SocketClient;
import org.proptit.localchat.common.enums.TypeDataPacket;
import org.proptit.localchat.common.models.DataPacket;
import org.proptit.localchat.common.models.User;
import org.proptit.localchat.common.models.call.CallSignal;
import org.proptit.localchat.common.models.message.FileMessage;
import org.proptit.localchat.common.models.message.ImageMessage;
import org.proptit.localchat.common.models.message.Message;
import org.proptit.localchat.common.models.message.TextMessage;
import org.proptit.localchat.common.utils.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ChatController implements ChatCallView {
    private SocketClient client;
    private User me;
    private List<User> onlineUsers = new ArrayList<>();
    private final Map<String, User> conversationUserMap = new HashMap<>();
    private User selectedConversationUser;
    private ChatCallManager callManager;
    private Stage callStage;
    private CallWindowController callWindowController;

    @FXML
    private VBox vboxMessage;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private TextField messageInput;
    @FXML
    private ListView<String> lvOnlinePeople;
    @FXML
    private ListView<String> lvChatList;
    @FXML
    private Button sendMessageAllButton;
    @FXML
    public Label contactNameTopBar;

    public void init(SocketClient client, User me) {
        this.client = client;
        this.me = me;
        this.callManager = new ChatCallManager(client, me, this);

        vboxMessage.heightProperty().addListener((observable, oldValue, newValue) -> {
            scrollPane.setVvalue((Double) newValue);
        });

        // Gọi hàm cấu hình giao diện cho ListView
        setupListViewCustomCells();

        // get the target conversation user.
        if (lvChatList != null) {
            lvChatList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null && !newValue.equals("No conversations") && !newValue.equals("No one online")) {
                    selectedConversationUser = conversationUserMap.get(newValue);
                    if (selectedConversationUser != null) {
                        contactNameTopBar.setText(selectedConversationUser.getNickname());
                        clearMessageArea();
                    }
                }
            });
        }
        boolean isManager = me.isManager();
        if (sendMessageAllButton != null) {
            sendMessageAllButton.setVisible(isManager);
            sendMessageAllButton.setManaged(isManager);
        }

        if (lvOnlinePeople != null) {
            lvOnlinePeople.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && conversationUserMap.containsKey(newVal)) {
                    // Tự động chọn người đó bên danh sách Conversations
                    lvChatList.getSelectionModel().select(newVal);

                    // Cập nhật giao diện top bar và xóa vùng chat cũ
                    selectedConversationUser = conversationUserMap.get(newVal);
                    contactNameTopBar.setText(selectedConversationUser.getNickname());
                    clearMessageArea();
                }
            });
        }
    }

    private void setupListViewCustomCells() {
        if (lvOnlinePeople != null) {
            lvOnlinePeople.setOrientation(Orientation.HORIZONTAL);
            lvOnlinePeople.setPrefHeight(105);
            lvOnlinePeople.setMinHeight(105);
            lvOnlinePeople.setMaxHeight(105);

            lvOnlinePeople.setCellFactory(param -> new ListCell<String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null || item.equals("No one online")) {
                        setGraphic(null);
                        setText(null);
                        setStyle("-fx-background-color: transparent;");
                    } else {
                        VBox root = new VBox(5);
                        root.setAlignment(Pos.CENTER);
                        root.setPrefWidth(75);

                        StackPane avatarContainer = new StackPane();
                        avatarContainer.setMaxSize(52, 52);

                        Circle avatarBg = new Circle(26, Color.web("#2A3042"));
                        String initialLetter = item.substring(0, 1).toUpperCase();
                        Label initialLabel = new Label(initialLetter);
                        initialLabel.setTextFill(Color.WHITE);
                        initialLabel.setFont(Font.font("System", FontWeight.BOLD, 22));

                        Circle onlineDot = new Circle(7, Color.web("#23A559"));
                        onlineDot.setStroke(Color.web("#0B0F19"));
                        onlineDot.setStrokeWidth(2.5);
                        StackPane.setAlignment(onlineDot, Pos.BOTTOM_RIGHT);

                        avatarContainer.getChildren().addAll(avatarBg, initialLabel, onlineDot);

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
                }
            });
        }

        if (lvChatList != null) {
            lvChatList.setCellFactory(param -> new ListCell<String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null || item.equals("No conversations")) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        HBox root = new HBox(12);
                        root.setAlignment(Pos.CENTER_LEFT);
                        root.setPadding(new Insets(8, 12, 8, 12));

                        Circle avatar = new Circle(20, Color.web("#2A3042"));
                        VBox textInfo = new VBox(2);

                        String name = item.contains("(@") ? item.substring(0, item.indexOf("(@")).trim() : item;
                        Label nameLbl = new Label(name);
                        nameLbl.setTextFill(Color.WHITE);
                        nameLbl.setFont(Font.font("System", FontWeight.BOLD, 14));

                        Label lastMsg = new Label("Bấm để bắt đầu chat...");
                        lastMsg.setTextFill(Color.web("#8B92A5"));
                        lastMsg.setFont(Font.font("System", 11));

                        textInfo.getChildren().addAll(nameLbl, lastMsg);
                        root.getChildren().addAll(avatar, textInfo);
                        setGraphic(root);
                        setText(null);
                    }
                }
            });
        }
    }

    @FXML
    void onSendButtonClick(ActionEvent event) {
        String messageText = messageInput.getText();
        if (!messageText.trim().isEmpty()) {
            boolean isSendAll = event.getSource() == sendMessageAllButton;
            Message msg;

            if (!isSendAll && selectedConversationUser != null) {
                msg = TextMessage.createPrivate(me, selectedConversationUser.getNickname(), messageText);
                addMessageToScreen(messageText, "Me -> " + selectedConversationUser.getNickname(), true);
            } else {
                msg = TextMessage.createBroadcast(me, messageText);
                addMessageToScreen(messageText, "Me", true);
            }

            DataPacket packet = new DataPacket(TypeDataPacket.CHAT_MESSAGE, msg);
            if (client != null) {
                System.out.println("CLIENT GUI: Da dong goi va bat dau gui di...");
                client.sendData(packet);
            } else {
                System.out.println("Don't connect to internet");
            }
            messageInput.clear();
        }
    }

    private void addMessageToScreen(String text, String senderName, boolean isMe) {
        Label lblMessage = new Label(text);
        lblMessage.setWrapText(true);
        lblMessage.setMaxWidth(400);

        if (isMe) {
            lblMessage.setStyle("-fx-background-color: #AD7BFF; -fx-text-fill: black; -fx-background-radius: 15px; -fx-padding: 8px 12px;");
        } else {
            lblMessage.setStyle("-fx-background-color: #1E2435; -fx-text-fill: white; -fx-background-radius: 15px; -fx-padding: 8px 12px;");
        }

        VBox messageGroup = new VBox(3);

        if (!isMe) {
            Label lblSender = new Label(senderName);
            lblSender.setStyle("-fx-font-size: 11px; -fx-text-fill: #8B92A5; -fx-padding: 0 0 0 5px;");
            messageGroup.getChildren().add(lblSender);
            messageGroup.setAlignment(Pos.TOP_LEFT);
        } else {
            messageGroup.setAlignment(Pos.TOP_RIGHT);
        }

        messageGroup.getChildren().add(lblMessage);
        HBox hboxContainer = new HBox(messageGroup);

        hboxContainer.setPadding(new Insets(5, 10, 5, 10));
        if (isMe) {
            hboxContainer.setAlignment(Pos.CENTER_RIGHT);
        } else {
            hboxContainer.setAlignment(Pos.CENTER_LEFT);
        }
        vboxMessage.getChildren().add(hboxContainer);
    }

    private void addImageToScreen(Image img, String senderName, boolean isMe) {
        ImageView imageView = new ImageView(img);
        imageView.setFitWidth(250);
        imageView.setPreserveRatio(true);

        ContextMenu imageMenu = new ContextMenu();
        MenuItem saveImageItem = new MenuItem("Tải ảnh xuống");

        saveImageItem.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Lưu ảnh tải về");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("PNG Files", "*.png"),
                    new FileChooser.ExtensionFilter("JPG Files", "*.jpg")
            );
            fileChooser.setInitialFileName("downloaded_image.png");

            Stage stage = (Stage) imageView.getScene().getWindow();
            File file = fileChooser.showSaveDialog(stage);

            if (file != null) {
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
            }
        });

        imageMenu.getItems().add(saveImageItem);

        imageView.setOnContextMenuRequested(e -> {
            imageMenu.show(imageView, e.getScreenX(), e.getScreenY());
        });
        VBox messageGroup = new VBox(3);

        if (!isMe) {
            Label lblSender = new Label(senderName);
            lblSender.setStyle("-fx-font-size: 11px; -fx-text-fill: #8B92A5; -fx-padding: 0 0 0 5px;");
            messageGroup.getChildren().add(lblSender);
            messageGroup.setAlignment(Pos.TOP_LEFT);
        } else {
            messageGroup.setAlignment(Pos.TOP_RIGHT);
        }

        messageGroup.getChildren().add(imageView);

        HBox hboxContainer = new HBox(messageGroup);
        hboxContainer.setPadding(new Insets(5, 10, 5, 10));
        if (isMe) {
            hboxContainer.setAlignment(Pos.CENTER_RIGHT);
        } else {
            hboxContainer.setAlignment(Pos.CENTER_LEFT);
        }
        vboxMessage.getChildren().add(hboxContainer);
    }

    private void clearMessageArea() {
        if (vboxMessage != null) {
            vboxMessage.getChildren().clear();
        }
    }

    public void receiveMessage(Message msg) {
        Platform.runLater(() -> {
            String senderName = msg.getSender().getNickname();

            if (msg instanceof ImageMessage) {
                ImageMessage imgMsg = (ImageMessage) msg;
                Image img = new Image(new ByteArrayInputStream(imgMsg.getImageData()));
                addImageToScreen(img, senderName, false);

            } else if (msg instanceof FileMessage) {
                FileMessage fileMsg = (FileMessage) msg;
                addFileToScreen(fileMsg.getFileName(), fileMsg.getFileData(), senderName, false);
            } else {
                String content = msg.toString();
                addMessageToScreen(content, senderName, false);
            }
        });
    }

    @FXML
    void onFileButtonClick(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        File file = FileUtils.chooseFile(stage);

        if (file != null) {
            try {
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                String fileName = file.getName();

                String extension = "";
                int i = fileName.lastIndexOf('.');
                if (i > 0) {
                    extension = fileName.substring(i + 1).toLowerCase();
                }

                boolean isImage = extension.matches("(png|jpg|jpeg|gif)");

                Message msg;
                if (isImage) {
                    if (selectedConversationUser != null) {
                        msg = ImageMessage.createPrivate(me, selectedConversationUser.getNickname(), fileBytes, extension);
                    } else {
                        msg = ImageMessage.createBroadcast(me, fileBytes, extension);
                    }
                    addImageToScreen(new Image(new ByteArrayInputStream(fileBytes)), "Me", true);
                } else {
                    if (selectedConversationUser != null) {
                        msg = FileMessage.createPrivate(me, selectedConversationUser.getNickname(), fileBytes, fileName, extension);
                    } else {
                        msg = FileMessage.createBroadcast(me, fileBytes, fileName, extension);
                    }
                    addFileToScreen(fileName, fileBytes, "Me", true);
                }

                client.sendData(new DataPacket(TypeDataPacket.CHAT_MESSAGE, msg));

            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Lỗi khi đọc file!");
            }
        }
    }

    private void addFileToScreen(String fileName, byte[] fileData, String senderName, boolean isMe) {
        VBox messageGroup = new VBox(3);

        if (!isMe) {
            Label lblSender = new Label(senderName);
            lblSender.setStyle("-fx-font-size: 11px; -fx-text-fill: #8B92A5; -fx-padding: 0 0 0 5px;");
            messageGroup.getChildren().add(lblSender);
            messageGroup.setAlignment(Pos.TOP_LEFT);
        } else {
            messageGroup.setAlignment(Pos.TOP_RIGHT);
        }

        HBox fileBox = new HBox(10);
        fileBox.setAlignment(Pos.CENTER_LEFT);
        fileBox.setStyle("-fx-background-color: #1E2435; -fx-background-radius: 10px; -fx-padding: 10px; -fx-border-color: #2A3042; -fx-border-radius: 10px;");

        Label lblFileName = new Label(fileName);
        lblFileName.setWrapText(true);
        lblFileName.setMaxWidth(200);
        lblFileName.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");

        Button btnDownload = new Button("Tải về");
        btnDownload.setStyle("-fx-background-color: #AD7BFF; -fx-text-fill: black; -fx-background-radius: 5px; -fx-cursor: hand;");

        btnDownload.setOnAction(e -> downloadFile(fileName, fileData));

        fileBox.getChildren().addAll(lblFileName, btnDownload);
        messageGroup.getChildren().add(fileBox);

        HBox hboxContainer = new HBox(messageGroup);
        hboxContainer.setPadding(new Insets(5, 10, 5, 10));
        if (isMe) {
            hboxContainer.setAlignment(Pos.CENTER_RIGHT);
        } else {
            hboxContainer.setAlignment(Pos.CENTER_LEFT);
        }
        Platform.runLater(() -> {
            vboxMessage.getChildren().add(hboxContainer);
        });
    }

    private void downloadFile(String defaultFileName, byte[] fileData) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Lưu file");
        fileChooser.setInitialFileName(defaultFileName);

        Stage stage = (Stage) vboxMessage.getScene().getWindow();
        File saveFile = fileChooser.showSaveDialog(stage);

        if (saveFile != null) {
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
    }

    public void updateOnlinePeople(List<User> users) {
        Platform.runLater(() -> {
            if (lvOnlinePeople == null || lvChatList == null) {
                return;
            }

            onlineUsers = users == null ? new ArrayList<>() : new ArrayList<>(users);

            lvOnlinePeople.getItems().clear();
            lvChatList.getItems().clear();
            conversationUserMap.clear();

            if (onlineUsers.isEmpty()) {
                lvOnlinePeople.getItems().add("No one online");
                lvChatList.getItems().add("No conversations");
                selectedConversationUser = null;
                return;
            }

            List<String> onlineNames = onlineUsers.stream()
                    .map(user -> user.getNickname() + " (@" + user.getUsername() + ")")
                    .collect(Collectors.toList());
            lvOnlinePeople.getItems().addAll(onlineNames);

            List<User> availableConversations = onlineUsers.stream()
                    .filter(user -> me == null || !user.getUsername().equalsIgnoreCase(me.getUsername()))
                    .collect(Collectors.toList());

            if (availableConversations.isEmpty()) {
                lvChatList.getItems().add("No conversations");
                selectedConversationUser = null;
                return;
            }

            for (User user : availableConversations) {
                String label = user.getNickname() + " (@" + user.getUsername() + ")";
                conversationUserMap.put(label, user);
                lvChatList.getItems().add(label);
            }

            if (selectedConversationUser != null) {
                String selectedUsername = selectedConversationUser.getUsername();
                boolean stillOnline = availableConversations.stream()
                        .anyMatch(user -> user.getUsername().equalsIgnoreCase(selectedUsername));
                if (!stillOnline) {
                    selectedConversationUser = null;
                    lvChatList.getSelectionModel().clearSelection();
                }
            }
        });
    }

    public void onCallButtonClick(ActionEvent actionEvent) {
        if (callManager != null) {
            callManager.startOutgoingCall(selectedConversationUser);
        }
    }

    public void receiveCallSignal(CallSignal signal) {
        if (callManager != null) {
            callManager.receiveCallSignal(signal);
        }
    }

    @Override
    public void showCallWindow(User peer, String statusText) {
        try {
            if (callStage != null && callStage.isShowing()) {
                if (callWindowController != null) {
                    callWindowController.init(peer);
                    callWindowController.updateCallStatus(statusText);
                    callWindowController.setMicMuted(false);
                }
                callStage.toFront();
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/proptit/localchat/call_window.fxml"));
            Parent root = loader.load();

            callWindowController = loader.getController();
            callWindowController.init(peer);
            callWindowController.updateCallStatus(statusText);
            callWindowController.setMicMuted(false);
            callWindowController.setOnMuteChanged(muted -> {
                if (callManager != null) {
                    callManager.setMuted(muted);
                }
            });
            callWindowController.setOnScreenShareChanged(sharing -> {
                if (callManager != null) {
                    callManager.setScreenSharing(sharing);
                }
            });
            callWindowController.setOnEndCall(() -> {
                if (callManager != null) {
                    callManager.endCall(true);
                }
            });

            callStage = new Stage();
            callStage.setTitle("Call - " + peer.getNickname());
            callStage.setScene(new Scene(root));
            callStage.setOnCloseRequest(event -> {
                event.consume();
                if (callManager != null) {
                    callManager.endCall(true);
                }
                closeCallWindow();
            });
            callStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Unable to open call window.");
        }
    }

    @Override
    public void updateCallStatus(String statusText) {
        Platform.runLater(() -> {
            if (callWindowController != null) {
                callWindowController.updateCallStatus(statusText);
            }
        });
    }

    @Override
    public void updateScreenShareButton(boolean sharing) {
        Platform.runLater(() -> {
            if (callWindowController != null) {
                callWindowController.setScreenSharingActive(sharing);
            }
        });
    }

    @Override
    public void showRemoteScreenFrame(byte[] frameBytes) {
        Platform.runLater(() -> {
            if (callWindowController != null) {
                callWindowController.showRemoteScreenFrame(frameBytes);
            }
        });
    }

    @Override
    public void clearRemoteScreenFrame() {
        Platform.runLater(() -> {
            if (callWindowController != null) {
                callWindowController.clearRemoteScreenFrame();
            }
        });
    }

    @Override
    public void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setHeaderText(null);
        alert.show();
    }

    @Override
    public void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(null);
        alert.show();
    }

    @Override
    public boolean confirmIncomingCall(User caller) {
        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                caller.getNickname() + " is calling you.",
                ButtonType.YES,
                ButtonType.NO
        );
        confirm.setTitle("Incoming Call");
        confirm.setHeaderText("Accept voice call?");
        return confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    @Override
    public User resolveUser(String username, String nickname) {
        if (username != null) {
            for (User onlineUser : onlineUsers) {
                if (onlineUser.getUsername().equalsIgnoreCase(username)) {
                    return onlineUser;
                }
            }
        }

        User user = new User(username);
        user.setNickname(nickname != null && !nickname.isBlank() ? nickname : username);
        return user;
    }

    @Override
    public String resolveLocalAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ex) {
            return "127.0.0.1";
        }
    }

    @Override
    public void closeCallWindow() {
        if (callStage != null) {
            callStage.setOnCloseRequest(null);
            if (callStage.isShowing()) {
                callStage.close();
            }
            callStage = null;
            callWindowController = null;
        }
    }

    public void onVideoCallButtonClick(ActionEvent actionEvent) {
    }
}