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
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.proptit.localchat.client.networks.SocketClient;
import org.proptit.localchat.common.enums.TypeDataPacket;
import org.proptit.localchat.common.enums.TypeMessage;
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
import java.util.*;
import java.util.stream.Collectors;

public class ChatController {
    private SocketClient client;
    private User me;

    private final Map<String, User> conversationUserMap = new HashMap<>();

    private List<User> allMembers = new ArrayList<>();
    private final Set<Integer> onlineUserIds = new HashSet<>();

    private User selectedConversationUser;
    private final Map<String, Button> pendingFileButtons = new HashMap<>();
    private static final String ANNOUNCEMENT_LABEL = "Thông báo chung 📢";

    @FXML private VBox vboxMessage;
    @FXML private ScrollPane scrollPane;
    @FXML private TextArea messageInput;
    @FXML private ListView<String> lvOnlinePeople;
    @FXML private ListView<String> lvChatList;


    public void init(SocketClient client, User me) {
        this.client = client;
        this.me = me;

        vboxMessage.heightProperty().addListener((observable, oldValue, newValue) -> {
            scrollPane.setVvalue(1.0);
        });

        lvChatList.setCellFactory(param -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else if (item.equals(ANNOUNCEMENT_LABEL)) {
                    setText(item);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #E67E22;");
                } else {
                    User u = conversationUserMap.get(item);
                    if (u != null && onlineUserIds.contains(u.getId())) {
                        setText("● " + item);
                        setStyle("-fx-text-fill: #2ECC71; -fx-font-weight: bold;");
                    } else {
                        setText("○ " + item);
                        setStyle("-fx-text-fill: #95A5A6;");
                    }
                }
            }
        });


        if (lvChatList != null) {
            lvChatList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == null) return;

                if (newValue.equals(ANNOUNCEMENT_LABEL)) {
                    selectedConversationUser = null;
                    clearMessageArea();


                    boolean canSend = me.isManager();
                    messageInput.getParent().setVisible(canSend);
                    messageInput.getParent().setManaged(canSend);

                    System.out.println("DEBUG: Xem thông báo chung");
                    client.sendData(new DataPacket(TypeDataPacket.GET_HISTORY_REQUEST, null));
                    return;
                }

                User newUser = conversationUserMap.get(newValue);
                if (newUser != null)
                {
                    messageInput.getParent().setVisible(true);
                    messageInput.getParent().setManaged(true);
                    if(selectedConversationUser == null || newUser.getId() != selectedConversationUser.getId())
                    {
                        selectedConversationUser = newUser;
                        clearMessageArea();
                        System.out.println("DEBUG: Load lịch sử với " + selectedConversationUser.getNickname());
                        client.sendData(new DataPacket(TypeDataPacket.GET_HISTORY_REQUEST, selectedConversationUser.getId()));
                    }

                }
            });
        }

        client.sendData(new DataPacket(TypeDataPacket.GET_CHAT_CONTACTS, null));
    }

    public void setAllMembers(List<User> members) {
        Platform.runLater(() -> {
            this.allMembers = members;

            lvChatList.getItems().clear();
            conversationUserMap.clear();


            lvChatList.getItems().add(ANNOUNCEMENT_LABEL);


            for (User u : allMembers) {
                if (u.getId() == me.getId()) continue;
                String label = u.getNickname() + " (@" + u.getUsername() + ")";
                conversationUserMap.put(label, u);
                lvChatList.getItems().add(label);
            }
        });
    }

    @FXML
    void onSendButtonClick(ActionEvent event) {
        String messageText = messageInput.getText();
        if (!messageText.trim().isEmpty()) {

            Message msg;
            String selectedItem = lvChatList.getSelectionModel().getSelectedItem();

            if (selectedItem.equals(ANNOUNCEMENT_LABEL)) {
                msg = TextMessage.createBroadcast(me, messageText);
                addMessageToScreen(messageText, "Me", true, msg.getSentAt());

            } else {
                msg = TextMessage.createPrivate(me, selectedConversationUser, messageText);
                addMessageToScreen(messageText, "Me -> " + selectedConversationUser.getNickname(), true, msg.getSentAt());
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

    public void loadHistory(List<Message> history) {
        Platform.runLater(() -> {
            vboxMessage.getChildren().clear();

            for (Message msg : history) {
                boolean isMe = msg.getSender().getId().equals(me.getId());
                String senderName = isMe ? "Me" : msg.getSender().getNickname();

                if (msg.getTypeMessage() == TypeMessage.TEXT)
                    addMessageToScreen(msg.getContent(), senderName, isMe, msg.getSentAt());
                else if(msg.getTypeMessage() == TypeMessage.IMAGE)
                {
                    ImageView imageView = new ImageView();
                    imageView.setFitWidth(250);
                    imageView.setPreserveRatio(true);

                    addImageToScreen(imageView, msg.getSender().getNickname(), isMe, msg.getSentAt());
                    client.sendRequestDownload(msg.getContent(), imageView);
                }
                else
                {
                    addFileToScreen(msg.getContent(), msg.getFileName(), null,senderName, isMe, msg.getSentAt());
                }
            }
        });
    }


    private void addMessageToScreen(String text, String senderName, boolean isMe, String time) {
        Label lblMessage = new Label(text);
        lblMessage.setWrapText(true);
        lblMessage.setMaxWidth(400);

        Label lblTime = new Label(time);
        lblTime.setStyle("-fx-font-size: 10px; -fx-text-fill: #919191;");

        if (isMe) {
            lblMessage.setStyle("-fx-background-color: #0084FF; -fx-text-fill: white; -fx-background-radius: 15px; -fx-padding: 8px 12px;");
        } else {
            lblMessage.setStyle("-fx-background-color: #E4E6EB; -fx-text-fill: black; -fx-background-radius: 15px; -fx-padding: 8px 12px;");
        }

        VBox messageGroup = new VBox(3);

        if (!isMe) {
            Label lblSender = new Label(senderName);
            lblSender.setStyle("-fx-font-size: 11px; -fx-text-fill: #65676B; -fx-padding: 0 0 0 5px;");

            HBox header = new HBox(8, lblSender, lblTime);
            header.setAlignment(Pos.BOTTOM_LEFT);

            messageGroup.getChildren().add(header);
            messageGroup.setAlignment(Pos.TOP_LEFT);

            lblMessage.setMaxWidth(Region.USE_PREF_SIZE);
            lblMessage.setMinWidth(Region.USE_PREF_SIZE);

            VBox.setMargin(lblMessage, new Insets(0));
        } else {
            messageGroup.getChildren().add(lblTime);
            messageGroup.setAlignment(Pos.TOP_RIGHT);

            lblMessage.setMaxWidth(Region.USE_PREF_SIZE);
            lblMessage.setPrefWidth(Region.USE_COMPUTED_SIZE);
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

    private void addImageToScreen(ImageView imageView, String senderName, boolean isMe, String time) {

        imageView.setFitWidth(250);
        imageView.setPreserveRatio(true);

        Label lblTime = new Label(time);
        lblTime.setStyle("-fx-font-size: 10px; -fx-text-fill: #919191;");

        VBox messageGroup = new VBox(3);

        if (!isMe) {
            Label lblSender = new Label(senderName);
            lblSender.setStyle("-fx-font-size: 11px; -fx-text-fill: #65676B; -fx-padding: 0 0 0 5px;");

            HBox header = new HBox(8, lblSender, lblTime);

            header.setAlignment(Pos.BOTTOM_LEFT);
            messageGroup.getChildren().add(header);
            messageGroup.setAlignment(Pos.TOP_LEFT);
        } else {
            messageGroup.getChildren().add(lblTime);
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

            String selectedItem = lvChatList.getSelectionModel().getSelectedItem();

            boolean isAnnouncementTab = selectedItem.equals(ANNOUNCEMENT_LABEL);
            boolean isChattingWithSender = (selectedConversationUser != null &&
                    msg.getSender().getId().equals(selectedConversationUser.getId()));

            String senderName = msg.getSender().getNickname();

            if ((msg.isBroadcast() && isAnnouncementTab) || (!msg.isBroadcast() && isChattingWithSender)) {
                if (msg instanceof ImageMessage) {
                    ImageMessage imgMsg = (ImageMessage) msg;
                    Image img = new Image(new ByteArrayInputStream(imgMsg.getImageData()));
                    ImageView imageView = new ImageView(img);
                    addImageToScreen(imageView, senderName, false, msg.getSentAt());

                } else if (msg instanceof FileMessage) {
                    FileMessage fileMsg = (FileMessage) msg;
                    addFileToScreen(fileMsg.getContent(),fileMsg.getFileName(), fileMsg.getFileData(), senderName, false, msg.getSentAt());
                } else {
                    addMessageToScreen(msg.getContent(), senderName, false, msg.getSentAt());
                }
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

                String selectedItem = lvChatList.getSelectionModel().getSelectedItem();

                Message msg;
                if (isImage) {
                    if (selectedItem.equals(ANNOUNCEMENT_LABEL)) {

                        msg = ImageMessage.createBroadcast(me, fileBytes, extension);
                    } else {
                        msg = ImageMessage.createPrivate(me, selectedConversationUser, fileBytes, extension);
                    }
                    addImageToScreen(new ImageView(new Image(new ByteArrayInputStream(fileBytes))), "Me", true, msg.getSentAt());
                } else {
                    if (selectedItem.equals(ANNOUNCEMENT_LABEL)) {
                        msg = FileMessage.createBroadcast(me, fileBytes, fileName, extension);

                    } else {
                        msg = FileMessage.createPrivate(me, selectedConversationUser, fileBytes, fileName, extension);
                    }
                    addFileToScreen(null,fileName, fileBytes, "Me", true, msg.getSentAt());
                }

                client.sendData(new DataPacket(TypeDataPacket.CHAT_MESSAGE, msg));

            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Lỗi khi đọc file!");
            }
        }
    }

    private void addFileToScreen(String serverUUID, String fileName, byte[] fileData, String senderName, boolean isMe, String time) {

        Label lblTime = new Label(time);
        lblTime.setStyle("-fx-font-size: 10px; -fx-text-fill: #919191;");

        VBox messageGroup = new VBox(3);

        if (!isMe) {
            Label lblSender = new Label(senderName);
            lblSender.setStyle("-fx-font-size: 11px; -fx-text-fill: #65676B; -fx-padding: 0 0 0 5px;");
            HBox header = new HBox(8, lblSender, lblTime);
            header.setAlignment(Pos.BOTTOM_LEFT);
            messageGroup.getChildren().add(header);
            messageGroup.setAlignment(Pos.TOP_LEFT);
        } else {
            messageGroup.getChildren().add(lblTime);
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

        btnDownload.setOnAction(e -> {
            if(fileData != null)
            {
                System.out.println("hello");
                downloadFile(fileName, fileData);
            }
            else
            {
                btnDownload.setText("Đang lấy...");
                btnDownload.setDisable(true);
                pendingFileButtons.put(serverUUID, btnDownload);
                client.sendData(new DataPacket(TypeDataPacket.DOWNLOAD_FILE_REQUEST, serverUUID));
            }

        });




        fileBox.getChildren().addAll(lblFileName, btnDownload);
        messageGroup.getChildren().add(fileBox);

        HBox hboxContainer = new HBox(messageGroup);
        hboxContainer.setPadding(new Insets(5, 10, 5, 10));
        if (isMe) {
            hboxContainer.setAlignment(Pos.CENTER_RIGHT);
        } else {
            hboxContainer.setAlignment(Pos.CENTER_LEFT);
        }
//        Platform.runLater(() -> {
//            vboxMessage.getChildren().add(hboxContainer);
//        });
        vboxMessage.getChildren().add(hboxContainer);
    }
    private void downloadFile(String fileName, byte[] fileData) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Lưu file");
        fileChooser.setInitialFileName(fileName);

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

    public void updateOnlinePeople(List<User> newUsers) {
        Platform.runLater(() -> {
            if (lvOnlinePeople == null || lvChatList == null) return;


            List<String> onlineNames = newUsers.stream()
                    .map(user -> user.getNickname() + " (@" + user.getUsername() + ")")
                    .collect(Collectors.toList());
            lvOnlinePeople.getItems().setAll(onlineNames);

            this.onlineUserIds.clear();
            for (User u : newUsers) {
                this.onlineUserIds.add(u.getId());
            }

            lvChatList.refresh();
        });
    }

    public void handleFileDownloadResponse(String fileName, byte[] fileData) {
        Platform.runLater(() -> {
            Button btn = pendingFileButtons.get(fileName);
            if (btn != null) {
                btn.setText("Tải về");
                btn.setDisable(false);
                pendingFileButtons.remove(fileName);
                downloadFile(fileName, fileData);
            }
        });
    }



}
