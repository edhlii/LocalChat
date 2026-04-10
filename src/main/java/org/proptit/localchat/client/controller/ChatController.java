package org.proptit.localchat.client.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.proptit.localchat.client.networks.SocketClient;
import org.proptit.localchat.common.enums.TypeDataPacket;
import org.proptit.localchat.common.models.DataPacket;
import org.proptit.localchat.common.models.User;
import org.proptit.localchat.common.models.message.FileMessage;
import org.proptit.localchat.common.models.message.ImageMessage;
import org.proptit.localchat.common.models.message.Message;
import org.proptit.localchat.common.models.message.TextMessage;
import org.proptit.localchat.common.utils.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ChatController {
    private SocketClient client;
    private User me;
    private List<User> onlineUsers = new ArrayList<>();
    private final Map<String, User> conversationUserMap = new HashMap<>();
    private User selectedConversationUser;

    @FXML private VBox vboxMessage;
    @FXML private ScrollPane scrollPane;
    @FXML private TextArea messageInput;
    @FXML private ListView<String> lvOnlinePeople;
    @FXML private ListView<String> lvChatList;
    @FXML private Button sendMessageAllButton;

    public void init(SocketClient client, User me) {
        this.client = client;
        this.me = me;

        vboxMessage.heightProperty().addListener((observable, oldValue, newValue) -> {
            scrollPane.setVvalue((Double) newValue);
        });

        if (lvChatList != null) {
            lvChatList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                selectedConversationUser = conversationUserMap.get(newValue);
                clearMessageArea();
            });
        }
        boolean isManager = me.isManager();
        if (sendMessageAllButton != null) {
            sendMessageAllButton.setVisible(isManager);
            sendMessageAllButton.setManaged(isManager);
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
            lblMessage.setStyle("-fx-background-color: #0084FF; -fx-text-fill: white; -fx-background-radius: 15px; -fx-padding: 8px 12px;");
        } else {
            lblMessage.setStyle("-fx-background-color: #E4E6EB; -fx-text-fill: black; -fx-background-radius: 15px; -fx-padding: 8px 12px;");
        }

        VBox messageGroup = new VBox(3);

        if (!isMe) {
            Label lblSender = new Label(senderName);
            lblSender.setStyle("-fx-font-size: 11px; -fx-text-fill: #65676B; -fx-padding: 0 0 0 5px;");
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

        VBox messageGroup = new VBox(3);

        if (!isMe) {
            Label lblSender = new Label(senderName);
            lblSender.setStyle("-fx-font-size: 11px; -fx-text-fill: #65676B; -fx-padding: 0 0 0 5px;");
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
            lblSender.setStyle("-fx-font-size: 11px; -fx-text-fill: #65676B; -fx-padding: 0 0 0 5px;");
            messageGroup.getChildren().add(lblSender);
            messageGroup.setAlignment(Pos.TOP_LEFT);
        } else {
            messageGroup.setAlignment(Pos.TOP_RIGHT);
        }

        HBox fileBox = new HBox(10);
        fileBox.setAlignment(Pos.CENTER_LEFT);
        fileBox.setStyle("-fx-background-color: #F0F2F5; -fx-background-radius: 10px; -fx-padding: 10px; -fx-border-color: #CCD0D5; -fx-border-radius: 10px;");

        Label lblFileName = new Label(fileName);
        lblFileName.setWrapText(true);
        lblFileName.setMaxWidth(200);
        lblFileName.setStyle("-fx-font-weight: bold; -fx-text-fill: #050505;");

        Button btnDownload = new Button("Tải về");
        btnDownload.setStyle("-fx-background-color: #0084FF; -fx-text-fill: white; -fx-background-radius: 5px; -fx-cursor: hand;");

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



}
