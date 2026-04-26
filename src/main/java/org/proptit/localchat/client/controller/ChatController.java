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

import org.proptit.localchat.common.models.ChatGroup;
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
    private static ChatController instance;
    public ChatController() { instance = this; }
    public static ChatController getInstance() { return instance; }
    private SocketClient client;
    private User me;
    private final Map<String, User> conversationUserMap = new HashMap<>();

    private List<User> allMembers = new ArrayList<>();
    private final Set<Integer> onlineUserIds = new HashSet<>();

    private User selectedConversationUser;

    private final Map<String, Button> pendingFileButtons = new HashMap<>();



    private List<ChatGroup> myGroupsList = new ArrayList<>();
    private final Map<String, ChatGroup> conversationGroupMap = new HashMap<>();

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
    @FXML private Button btnTabAll;
    @FXML private Button btnTabGroups;
    @FXML
    private TextField txtSearchPeopleChat;

    private boolean isGroupMode = false;
    private ChatGroup selectedConversationGroup;

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
                    selectedConversationGroup = null;
                    clearMessageArea();

                    contactNameTopBar.setText(ANNOUNCEMENT_LABEL);

                    boolean canSend = me.isManager();
                    messageInput.getParent().setVisible(canSend);
                    messageInput.getParent().setManaged(canSend);

                    System.out.println("DEBUG: Xem thông báo chung");
                    client.sendData(new DataPacket(TypeDataPacket.GET_HISTORY_REQUEST, null));
                    return;
                }

                messageInput.getParent().setVisible(true);
                messageInput.getParent().setManaged(true);

                if (isGroupMode) {
                    ChatGroup newGroup = conversationGroupMap.get(newValue);
                    if (newGroup != null) {
                        contactNameTopBar.setText(newGroup.getName());

                        if (selectedConversationGroup == null || newGroup.getId() != selectedConversationGroup.getId()) {
                            selectedConversationGroup = newGroup;
                            selectedConversationUser = null;
                            clearMessageArea();
                            System.out.println("DEBUG: Load lịch sử nhóm " + selectedConversationGroup.getName());

                            client.sendData(new DataPacket(TypeDataPacket.GET_GROUP_HISTORY_REQUEST, selectedConversationGroup.getId()));
                        }
                    }
                }
                else
                {
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

        messageInput.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                onSendButtonClick(new ActionEvent());
                event.consume();
            }
        });


        client.sendData(new DataPacket(TypeDataPacket.GET_CHAT_CONTACTS, null));

        client.sendData(new DataPacket(TypeDataPacket.GET_MY_GROUPS_REQUEST, me.getId()));


        client.sendData(new DataPacket(TypeDataPacket.GET_OFFLINE_NOTIFICATIONS, null));
    }



    private void renderChatListByKeyword(String keyword) {
        String normalizedKeyword = (keyword == null) ? "" : keyword.trim().toLowerCase();


        lvChatList.getItems().clear();
        lvChatList.getItems().clear();


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
            this.allMembers = members;
            if (!isGroupMode) {
                lvChatList.getItems().clear();
                conversationUserMap.clear();
                lvChatList.getItems().add(ANNOUNCEMENT_LABEL);

                for (User u : allMembers) {
                    if (u.getId() == me.getId()) continue;
                    String label = u.getNickname() + " (@" + u.getUsername() + ")";
                    conversationUserMap.put(label, u);
                    lvChatList.getItems().add(label);
                }
            } else {
                conversationUserMap.clear();
                for (User u : allMembers) {
                    if (u.getId() == me.getId()) continue;
                    String label = u.getNickname() + " (@" + u.getUsername() + ")";
                    conversationUserMap.put(label, u);
                }
            }
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

                        Circle avatarCircle = new Circle(26, Color.web("#2A3042"));
                        avatarCircle.setStroke(Color.WHITE);
                        avatarCircle.setStrokeWidth(2);


                        String name = item.contains("(@") ? item.substring(0, item.indexOf("(@")).trim() : item;
                        User u = conversationUserMap.get(item);

                        if (u != null && u.getAvatar() != null && u.getAvatar().length != 0) {
                            try {
                                Image img = new Image(new ByteArrayInputStream(u.getAvatar()));

                                if (!img.isError()) {
                                    avatarCircle.setFill(new javafx.scene.paint.ImagePattern(img));
                                    //avatarContainer.getChildren().clear();
                                    avatarContainer.getChildren().add(avatarCircle);
                                } else {
                                    setDefaultAvatar(avatarContainer, avatarCircle, name, 22);
                                }
                            } catch (Exception e) {

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
                        setTooltip(null);
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
                        Circle avatarCircle = new Circle(20, Color.web("#2A3042"));
                        avatarCircle.setStroke(Color.WHITE);
                        avatarCircle.setStrokeWidth(1);

                        VBox textInfo = new VBox(2);
                        textInfo.setAlignment(Pos.CENTER_LEFT);




                        if (isGroupMode) {
                            ChatGroup g = conversationGroupMap.get(item);
                            String displayName = (g != null) ? g.getName() : item.replace("👥", "").trim();

                            // Vẽ icon mặc định cho nhóm (Dùng chữ cái đầu tên nhóm)
                            setDefaultAvatar(avatarStack, avatarCircle, displayName, 14);

                            Label nameLbl = new Label(displayName);
                            nameLbl.setTextFill(Color.WHITE);
                            nameLbl.setFont(Font.font("System", FontWeight.BOLD, 14));
                            textInfo.getChildren().add(nameLbl);

                            setTooltip(null); // Group hiện tại không cần tooltip username
                        }
                        else
                        {
                            String name = item.contains("(@") ? item.substring(0, item.indexOf("(@")).trim() : item;
                            User u = conversationUserMap.get(item);
                            if (u != null && u.getAvatar() != null && u.getAvatar().length != 0) {
                                try {
                                    Image img = new Image(new ByteArrayInputStream(u.getAvatar()));

                                    if (!img.isError()) {
                                        avatarCircle.setFill(new javafx.scene.paint.ImagePattern(img));
                                        avatarStack.getChildren().clear();
                                        avatarStack.getChildren().add(avatarCircle);
                                    } else {
                                        setDefaultAvatar(avatarStack, avatarCircle, name, 14);
                                    }
                                } catch (Exception e) {
                                    setDefaultAvatar(avatarStack, avatarCircle, name, 14);
                                }
                            } else {
                                setDefaultAvatar(avatarStack, avatarCircle, name, 14);
                            }




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

                            Tooltip tip = new Tooltip(u.getUsername());
                            tip.setShowDelay(javafx.util.Duration.millis(200));

                            setTooltip(tip);


                        }
                        root.getChildren().addAll(avatarStack, textInfo);
                        setGraphic(root);
                        setText(null);
                    }
                }
            });
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

    @FXML
    void onSendButtonClick(ActionEvent event) {
        String messageText = messageInput.getText();
        if (!messageText.trim().isEmpty()) {
            Message msg = null;
            String selectedItem = lvChatList.getSelectionModel().getSelectedItem();
            if (selectedItem == null) return;
            if (selectedItem.equals(ANNOUNCEMENT_LABEL)) {
                msg = TextMessage.createBroadcast(me, messageText);
                addMessageToScreen(messageText, true, msg.getSentAt());

            } else if (isGroupMode) {
                if (selectedConversationGroup == null) return;
                msg = TextMessage.createGroup(me, selectedConversationGroup, messageText);
                addMessageToScreen(messageText, true, msg.getSentAt());


            } else {
                if (selectedConversationUser == null) return;

                msg = TextMessage.createPrivate(me, selectedConversationUser, messageText);
                addMessageToScreen(messageText, true, msg.getSentAt());
            }

            if (msg != null) {
                DataPacket packet = new DataPacket(TypeDataPacket.CHAT_MESSAGE, msg);
                if (client != null) {
                    System.out.println("CLIENT GUI: Đã đóng gói và bắt đầu gửi đi...");
                    client.sendData(packet);
                } else {
                    System.out.println("Không có kết nối mạng");
                }
                messageInput.clear();
            }
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
            if (me != null && msg.getSender().getId().equals(me.getId())) {
                return;
            }

            String selectedItem = lvChatList.getSelectionModel().getSelectedItem();
            if (selectedItem == null) return;




            boolean isRealBroadcast = msg.isBroadcast() && msg.getGroupId() == null;

            boolean isGroupMsg = msg.getGroupId() != null;


            boolean isPrivateMsg = !msg.isBroadcast() && msg.getGroupId() == null;


            boolean isShowingAnnouncement = selectedItem.equals(ANNOUNCEMENT_LABEL) && isRealBroadcast;


            boolean isChattingWithSender = (!isGroupMode && selectedConversationUser != null &&
                    isPrivateMsg && msg.getSender().getId().equals(selectedConversationUser.getId()));


            boolean isChattingInGroup = (isGroupMode && selectedConversationGroup != null &&
                    isGroupMsg && msg.getGroupId().equals(selectedConversationGroup.getId()));


            boolean isCurrentConversation = isShowingAnnouncement || isChattingWithSender || isChattingInGroup;


            if (!isCurrentConversation) {
                if (isRealBroadcast) {
                    usersWithNewMessages.add(0);
                } else if (isPrivateMsg) {
                    usersWithNewMessages.add(msg.getSender().getId());
                } else if (isGroupMsg) {
                    //
                }
                lvChatList.refresh();
            }

            if (isCurrentConversation) {
                if (msg instanceof ImageMessage) {
                    ImageMessage imgMsg = (ImageMessage) msg;
                    Image img = new Image(new ByteArrayInputStream(imgMsg.getImageData()));
                    ImageView imageView = new ImageView(img);
                    addImageToScreen(imageView, false, msg.getSentAt());

                } else if (msg instanceof FileMessage) {
                    FileMessage fileMsg = (FileMessage) msg;
                    addFileToScreen(fileMsg.getContent(), fileMsg.getFileName(), fileMsg.getFileData(), false, msg.getSentAt());
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
                if (selectedItem == null) return;

                Message msg = null;

                if (isImage) {
                    if (selectedItem.equals(ANNOUNCEMENT_LABEL)) {
                        msg = ImageMessage.createBroadcast(me, fileBytes, extension);
                    } else if (isGroupMode) {
                        if (selectedConversationGroup == null) return;
                        msg = ImageMessage.createGroup(me, selectedConversationGroup, fileBytes, extension);
                    } else {
                        if (selectedConversationUser == null) return;
                        msg = ImageMessage.createPrivate(me, selectedConversationUser, fileBytes, extension);
                    }
                    addImageToScreen(new ImageView(new Image(new ByteArrayInputStream(fileBytes))), true, msg.getSentAt());
                } else {
                    if (selectedItem.equals(ANNOUNCEMENT_LABEL)) {
                        msg = FileMessage.createBroadcast(me, fileBytes, fileName, extension);
                    } else if (isGroupMode) {
                        if (selectedConversationGroup == null) return;
                        msg = FileMessage.createGroup(me, selectedConversationGroup, fileBytes, fileName, extension);
                    } else {
                        if (selectedConversationUser == null) return;
                        msg = FileMessage.createPrivate(me, selectedConversationUser, fileBytes, fileName, extension);
                    }
                    addFileToScreen(null, fileName, fileBytes, true, msg.getSentAt());
                }
                if (msg != null) {
                    client.sendData(new DataPacket(TypeDataPacket.CHAT_MESSAGE, msg));

                }
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
                    .map(user -> {
                        String label = user.getNickname() + " (@" + user.getUsername() + ")";
                        conversationUserMap.put(label, user);
                        return label;
                    })
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

    @FXML
    void onTabAllClick(ActionEvent event) {
        isGroupMode = false;
        btnTabAll.setStyle("-fx-background-color: #0084FF; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand;");
        btnTabGroups.setStyle("-fx-background-color: transparent; -fx-text-fill: #B0B3B8; -fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand;");

        selectedConversationGroup = null;
        conversationUserMap.clear();
        lvChatList.getItems().clear();

        lvChatList.getItems().add(ANNOUNCEMENT_LABEL);
        Collection<User> usersToDisplay = allMembers;
        if (usersToDisplay == null || usersToDisplay.isEmpty()) {
            usersToDisplay = conversationUserMap.values();
        }

        List<User> availableConversations = usersToDisplay.stream()
                .filter(user -> me == null || !user.getUsername().equalsIgnoreCase(me.getUsername()))
                .collect(Collectors.toList());

        if (availableConversations.isEmpty()) {
            lvChatList.getItems().add("No conversations");
        } else {
            for (User user : availableConversations) {
                String label = user.getNickname() + " (@" + user.getUsername() + ")";
                conversationUserMap.put(label, user);
                lvChatList.getItems().add(label);
            }
        }
    }

    @FXML
    void onTabGroupsClick(ActionEvent event) {
        isGroupMode = true;

        btnTabGroups.setStyle("-fx-background-color: #0084FF; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold;");
        btnTabAll.setStyle("-fx-background-color: transparent; -fx-text-fill: #65676B; -fx-background-radius: 6; -fx-font-weight: bold;");

        selectedConversationUser = null;

        lvChatList.getItems().clear();
        conversationGroupMap.clear();


        if (myGroupsList.isEmpty()) {
            lvChatList.getItems().add("Chưa có nhóm nào");
            return;
        }

        for (ChatGroup group : myGroupsList) {
            String label = "👥" + group.getName();
            conversationGroupMap.put(label, group);
            lvChatList.getItems().add(label);
        }
    }

    @FXML
    void onCreateGroupClick(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/proptit/localchat/create_group.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Tạo Nhóm Mới");
            stage.setScene(new Scene(loader.load()));

            CreateGroupController controller = loader.getController();
            controller.setup(this.client, this.me, this.allMembers);

            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onGroupCreatedSuccess(ChatGroup newGroup) {
        Platform.runLater(() -> {
            myGroupsList.add(newGroup);

            if (isGroupMode) {
                lvChatList.getItems().add("👥 " + newGroup.getName());
                conversationGroupMap.put("👥 " + newGroup.getName(), newGroup);
            }

            javafx.stage.Window.getWindows().stream()
                    .filter(w -> w instanceof Stage)
                    .map(w -> (Stage) w)
                    .filter(stage -> "Tạo Nhóm Mới".equals(stage.getTitle()))
                    .findFirst()
                    .ifPresent(Stage::close);

            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Đã tạo nhóm: " + newGroup.getName());
            alert.setHeaderText(null);
            alert.show();
        });
    }

    public void setMyGroupsList(List<ChatGroup> groups) {
        Platform.runLater(() -> {
            this.myGroupsList = groups;
            if (isGroupMode) {
                onTabGroupsClick(null);
            }
        });
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