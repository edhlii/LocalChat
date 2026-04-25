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
import javafx.scene.layout.Region;
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
import org.proptit.localchat.common.enums.TypeMessage;
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
import java.util.*;
import java.util.stream.Collectors;

public class ChatController implements ChatCallView {
    private SocketClient client;
    private User me;

    private final Map<String, User> conversationUserMap = new HashMap<>();

    private List<User> allMembers = new ArrayList<>();
    private final Set<Integer> onlineUserIds = new HashSet<>();

    private User selectedConversationUser;

    private final Map<String, Button> pendingFileButtons = new HashMap<>();
    private static final String ANNOUNCEMENT_LABEL = "Thông báo chung";
    private final Set<Integer> usersWithNewMessages = new HashSet<>();


    private ChatCallManager callManager;
    private Stage callStage;
    private CallWindowController callWindowController;
    private List<User> allMembersForSearch = new ArrayList<>();

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
    @FXML
    private TextField txtSearchPeopleChat;


    public void init(SocketClient client, User me) {
        this.client = client;
        this.me = me;
        this.callManager = new ChatCallManager(client, me, this);

        vboxMessage.heightProperty().addListener((observable, oldValue, newValue) -> {
            scrollPane.setVvalue(1.0);
        });

        setupListViewCustomCells();

        if (lvChatList != null) {
            lvChatList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == null) return;

                if (newValue.equals(ANNOUNCEMENT_LABEL)) {
                    usersWithNewMessages.remove(0);
                    lvChatList.refresh();

                    client.sendData(new DataPacket(TypeDataPacket.MARK_AS_READ, 0));

                    selectedConversationUser = null;
                    clearMessageArea();

                    contactNameTopBar.setText(ANNOUNCEMENT_LABEL);

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
                    contactNameTopBar.setText(newUser.getNickname());
                    messageInput.getParent().setVisible(true);
                    messageInput.getParent().setManaged(true);
                    if(selectedConversationUser == null || newUser.getId() != selectedConversationUser.getId())
                    {
                        usersWithNewMessages.remove(newUser.getId());
                        lvChatList.refresh();

                        client.sendData(new DataPacket(TypeDataPacket.MARK_AS_READ, newUser.getId()));

                        selectedConversationUser = newUser;
                        clearMessageArea();
                        System.out.println("DEBUG: Load lịch sử với " + selectedConversationUser.getNickname());
                        client.sendData(new DataPacket(TypeDataPacket.GET_HISTORY_REQUEST, selectedConversationUser.getId()));

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

                    lvChatList.getSelectionModel().select(newVal);


                    selectedConversationUser = conversationUserMap.get(newVal);
                    contactNameTopBar.setText(selectedConversationUser.getNickname());
                    clearMessageArea();
                }
            });
        }

        if (txtSearchPeopleChat != null) {
            txtSearchPeopleChat.textProperty().addListener((observable, oldValue, newValue) -> {
                renderChatListByKeyword(newValue);
            });
        }


        client.sendData(new DataPacket(TypeDataPacket.GET_CHAT_CONTACTS, null));

        client.sendData(new DataPacket(TypeDataPacket.GET_OFFLINE_NOTIFICATIONS, null));
    }
    private void renderChatListByKeyword(String keyword) {
        String normalizedKeyword = (keyword == null) ? "" : keyword.trim().toLowerCase();


        lvChatList.getItems().clear();
        conversationUserMap.clear();


        lvChatList.getItems().add(ANNOUNCEMENT_LABEL);


        for (User user : allMembers) {
            if (user.getId().equals(me.getId())) continue;

            String username = user.getUsername() == null ? "" : user.getUsername().toLowerCase();
            String nickname = user.getNickname() == null ? "" : user.getNickname().toLowerCase();

            if (normalizedKeyword.isEmpty() || username.contains(normalizedKeyword) || nickname.contains(normalizedKeyword)) {
                String label = user.getNickname() + " (@" + user.getUsername() + ")";


                conversationUserMap.put(label, user);
                lvChatList.getItems().add(label);
            }
        }


        lvChatList.refresh();
    }

    public void setAllMembers(List<User> members) {
        Platform.runLater(() -> {

            this.allMembers = (members == null) ? new ArrayList<>() : new ArrayList<>(members);


            renderChatListByKeyword(txtSearchPeopleChat != null ? txtSearchPeopleChat.getText() : "");

            if (!lvChatList.getItems().isEmpty() && (txtSearchPeopleChat == null || txtSearchPeopleChat.getText().isEmpty())) {
                lvChatList.getSelectionModel().select(0);
            }
        });

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
                    }
                    else if(item.equals(ANNOUNCEMENT_LABEL))
                    {
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

                            textInfo.getChildren().add(newMsgNotify);

                        }

                        root.getChildren().addAll(icon, textInfo);
                        setGraphic(root);
                        setText(null);
                    }
                    else {
                        HBox root = new HBox(12);
                        root.setAlignment(Pos.CENTER_LEFT);
                        root.setPadding(new Insets(8, 12, 8, 12));

                        StackPane avatarStack = new StackPane();
                        Circle avatar = new Circle(20, Color.web("#2A3042"));

                        VBox textInfo = new VBox(2);
                        textInfo.setAlignment(Pos.CENTER_LEFT);


                        String name = item.contains("(@") ? item.substring(0, item.indexOf("(@")).trim() : item;
                        String initialLetter = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();

                        Label initialLabel = new Label(initialLetter);
                        initialLabel.setTextFill(Color.WHITE);
                        initialLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

                        avatarStack.getChildren().addAll(avatar, initialLabel);

                        User u = conversationUserMap.get(item);
                        if (u != null && onlineUserIds.contains(u.getId())) {
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


                        if (u != null && usersWithNewMessages.contains(u.getId())) {
                            Label newMsgNotify = new Label("Có tin nhắn mới");
                            newMsgNotify.setFont(Font.font("System", FontWeight.BOLD, 11));
                            textInfo.getChildren().add(newMsgNotify);
                            nameLbl.setTextFill(Color.web("#AD7BFF"));
                        }

                        root.getChildren().addAll(avatarStack, textInfo);
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

            Message msg;
            String selectedItem = lvChatList.getSelectionModel().getSelectedItem();

            if (selectedItem.equals(ANNOUNCEMENT_LABEL)) {
                msg = TextMessage.createBroadcast(me, messageText);
                addMessageToScreen(messageText, true, msg.getSentAt());

            } else {
                msg = TextMessage.createPrivate(me, selectedConversationUser, messageText);
                addMessageToScreen(messageText, true, msg.getSentAt());
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
                    addMessageToScreen(msg.getContent(), isMe, msg.getSentAt());
                else if(msg.getTypeMessage() == TypeMessage.IMAGE)
                {
                    ImageView imageView = new ImageView();
                    imageView.setFitWidth(250);
                    imageView.setPreserveRatio(true);

                    addImageToScreen(imageView, isMe, msg.getSentAt());
                    client.sendRequestDownload(msg.getContent(), imageView);
                }
                else
                {
                    addFileToScreen(msg.getContent(), msg.getFileName(), null, isMe, msg.getSentAt());
                }
            }
        });
    }


    private void addMessageToScreen(String text, boolean isMe, String time) {
        Label lblMessage = new Label(text);
        lblMessage.setWrapText(true);
        lblMessage.setMaxWidth(400);
        lblMessage.setFont(Font.font("System", 16));

        Label lblTime = new Label(time);
        lblTime.getStyleClass().add("chat-time");

        if (isMe) {
            lblMessage.setStyle("-fx-background-color: #AD7BFF; -fx-text-fill: white; -fx-background-radius: 15px; -fx-padding: 8px 12px;");
        } else {
            lblMessage.setStyle("-fx-background-color: #1E2435; -fx-text-fill: white; -fx-background-radius: 15px; -fx-padding: 8px 12px;");
        }

        VBox messageGroup = new VBox(3);

        if (!isMe) {
            messageGroup.getChildren().add(lblTime);

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

    private void addImageToScreen(ImageView imageView, boolean isMe, String time) {

        imageView.setFitWidth(250);
        imageView.setPreserveRatio(true);


        Label lblTime = new Label(time);
        lblTime.getStyleClass().add("chat-time");


        ContextMenu imageMenu = new ContextMenu();
        MenuItem saveImageItem = new MenuItem("Tải ảnh xuống");

        saveImageItem.setOnAction(e -> {
            if (imageView.getImage() == null) return;
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
            messageGroup.getChildren().add(lblTime);
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

            User sender = msg.getSender();
            String selectedItem = lvChatList.getSelectionModel().getSelectedItem();

            boolean isAnnouncementTab = selectedItem.equals(ANNOUNCEMENT_LABEL);
            boolean isChattingWithSender = (selectedConversationUser != null &&
                    sender.getId().equals(selectedConversationUser.getId()));

            boolean isCurrentTab = false;
            if (msg.isBroadcast()) {
                isCurrentTab = ANNOUNCEMENT_LABEL.equals(selectedItem);
            } else {
                isCurrentTab = (selectedConversationUser != null && sender.getId().equals(selectedConversationUser.getId()));
            }


            if (!isCurrentTab && !sender.getId().equals(me.getId())) {
                if (msg.isBroadcast()) {
                    usersWithNewMessages.add(0);
                } else {
                    usersWithNewMessages.add(sender.getId());
                }
                lvChatList.refresh();
            }

            if ((msg.isBroadcast() && isAnnouncementTab) || (!msg.isBroadcast() && isChattingWithSender)) {
                if (msg instanceof ImageMessage) {
                    ImageMessage imgMsg = (ImageMessage) msg;
                    Image img = new Image(new ByteArrayInputStream(imgMsg.getImageData()));
                    ImageView imageView = new ImageView(img);
                    addImageToScreen(imageView, false, msg.getSentAt());

                } else if (msg instanceof FileMessage) {
                    FileMessage fileMsg = (FileMessage) msg;
                    addFileToScreen(fileMsg.getContent(),fileMsg.getFileName(), fileMsg.getFileData(), false, msg.getSentAt());
                } else {
                    addMessageToScreen(msg.getContent(), false, msg.getSentAt());
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
                    addImageToScreen(new ImageView(new Image(new ByteArrayInputStream(fileBytes))), true, msg.getSentAt());
                } else {
                    if (selectedItem.equals(ANNOUNCEMENT_LABEL)) {
                        msg = FileMessage.createBroadcast(me, fileBytes, fileName, extension);

                    } else {
                        msg = FileMessage.createPrivate(me, selectedConversationUser, fileBytes, fileName, extension);
                    }
                    addFileToScreen(null,fileName, fileBytes, true, msg.getSentAt());
                }

                client.sendData(new DataPacket(TypeDataPacket.CHAT_MESSAGE, msg));

            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Lỗi khi đọc file!");
            }
        }
    }

    private void addFileToScreen(String serverUUID, String fileName, byte[] fileData, boolean isMe, String time) {

        Label lblTime = new Label(time);
        lblTime.setStyle("-fx-font-size: 10px; -fx-text-fill: #919191;");

        VBox messageGroup = new VBox(3);

        if (!isMe) {


            messageGroup.getChildren().add(lblTime);

            messageGroup.setAlignment(Pos.TOP_LEFT);
        } else {
            messageGroup.getChildren().add(lblTime);
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


    public void updateOnlinePeople(List<User> users) {
        Platform.runLater(() -> {
            if (lvOnlinePeople == null || lvChatList == null) return;


            List<String> onlineNames = users.stream()
                    .map(user -> user.getNickname() + " (@" + user.getUsername() + ")")
                    .collect(Collectors.toList());
            lvOnlinePeople.getItems().setAll(onlineNames);

            this.onlineUserIds.clear();
            for (User u : users) {
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

        String searchKey = nickname + " (@" + username + ")";


        User onlineUser = conversationUserMap.get(searchKey);

        if (onlineUser != null) {
            return onlineUser;
        }


        User user = new User(username);
        user.setNickname((nickname != null && !nickname.isBlank()) ? nickname : username);
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
    public void setOfflineMessages(List<Integer> unreadIds) {
        Platform.runLater(() -> {
            if (unreadIds != null && !unreadIds.isEmpty()) {
                usersWithNewMessages.addAll(unreadIds);
                lvChatList.refresh();
            }
        });
    }
}