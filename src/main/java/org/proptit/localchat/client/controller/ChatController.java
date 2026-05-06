package org.proptit.localchat.client.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
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
import javafx.stage.Modality;
import javafx.stage.Stage;

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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.stream.Collectors;

public class ChatController implements ChatCallView {
    private static ChatController instance;

    public ChatController() {
        instance = this;
    }

    public static ChatController getInstance() {
        return instance;
    }

    private SocketClient client;
    private User me;
    private final Map<String, User> conversationUserMap = new HashMap<>();
    private List<User> allMembers = new ArrayList<>();
    private final Set<Integer> onlineUserIds = new HashSet<>();
    private User selectedConversationUser;
    private final Map<String, Button> pendingFileButtons = new HashMap<>();
    private List<ChatGroup> myGroupsList = new ArrayList<>();
    private final Map<String, ChatGroup> conversationGroupMap = new HashMap<>();
    private final Set<Integer> usersWithNewMessages = new HashSet<>();
    private ChatCallManager callManager;
    private Stage callStage;
    private CallWindowController callWindowController;
    private boolean videoCallAvailable;
    private boolean videoCallActive;

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
    private Button btnTabAll;
    @FXML
    private Button btnTabGroups;
    @FXML
    private TextField txtSearchPeopleChat;
    @FXML
    private Button btnManageGroup;
    @FXML
    private Button btnGroupInfo;

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

        btnManageGroup.setVisible(false);
        btnManageGroup.setManaged(false);

        if (lvChatList != null) {
            lvChatList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == null) return;
                if (newValue.equals(ChatUiConstants.LABEL_ANNOUNCEMENT)) {
                    usersWithNewMessages.remove(0);
                    lvChatList.refresh();

                    client.sendData(new DataPacket(TypeDataPacket.MARK_AS_READ, 0));

                    selectedConversationUser = null;
                    selectedConversationGroup = null;
                    clearMessageArea();

                    contactNameTopBar.setText(ChatUiConstants.LABEL_ANNOUNCEMENT);
                    btnGroupInfo.setVisible(false);
                    btnGroupInfo.setManaged(false);

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
                        btnGroupInfo.setVisible(true);
                        btnGroupInfo.setManaged(true);

                        if (usersWithNewMessages.contains(-newGroup.getId())) {
                            usersWithNewMessages.remove(-newGroup.getId());
                            lvChatList.refresh();
                            client.sendData(new DataPacket(TypeDataPacket.MARK_AS_READ, -newGroup.getId()));
                        }

                        if (selectedConversationGroup == null || newGroup.getId() != selectedConversationGroup.getId()) {
                            selectedConversationGroup = newGroup;
                            selectedConversationUser = null;
                            clearMessageArea();
                            System.out.println("DEBUG: Load lịch sử nhóm " + selectedConversationGroup.getName());

                            client.sendData(new DataPacket(TypeDataPacket.GET_GROUP_HISTORY_REQUEST, selectedConversationGroup.getId()));
                        }

                        selectedConversationGroup = newGroup;
                        updateManageGroupButtonVisibility();
                    }
                } else {
                    selectedConversationGroup = null;
                    updateManageGroupButtonVisibility();
                    User newUser = conversationUserMap.get(newValue);
                    if (newUser != null) {
                        contactNameTopBar.setText(newUser.getNickname());
                        messageInput.getParent().setVisible(true);
                        messageInput.getParent().setManaged(true);
                        btnGroupInfo.setVisible(false);
                        btnGroupInfo.setManaged(false);

                        if (selectedConversationUser == null || newUser.getId() != selectedConversationUser.getId()) {
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

        lvChatList.getItems().add(ChatUiConstants.LABEL_ANNOUNCEMENT);


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
                lvChatList.getItems().add(ChatUiConstants.LABEL_ANNOUNCEMENT);

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
                lvChatList.getSelectionModel().select(ChatUiConstants.LABEL_ANNOUNCEMENT);
            }
        });
    }

    private void setupListViewCustomCells() {
        if (lvOnlinePeople != null) {
            lvOnlinePeople.setOrientation(Orientation.HORIZONTAL);
            lvOnlinePeople.setPrefHeight(105);
            lvOnlinePeople.setMinHeight(105);
            lvOnlinePeople.setMaxHeight(105);

            lvOnlinePeople.setCellFactory(ChatCellFactory.createOnlinePeopleCellFactory(conversationUserMap, onlineUserIds));
        }

        if (lvChatList != null) {
            lvChatList.setCellFactory(ChatCellFactory.createChatListCellFactory(
                conversationUserMap, conversationGroupMap, usersWithNewMessages, onlineUserIds, isGroupMode));
        }
    }

    @FXML
    void onSendButtonClick(ActionEvent event) {
        String messageText = messageInput.getText().trim();
        if (!messageText.isEmpty()) {
            Message msg = null;
            String selectedItem = lvChatList.getSelectionModel().getSelectedItem();
            if (selectedItem == null) return;
            if (selectedItem.equals(ChatUiConstants.LABEL_ANNOUNCEMENT)) {
                msg = TextMessage.createBroadcast(me, messageText);
                addMessageToScreen(messageText, true, msg.getSentAt(), me);

            } else if (isGroupMode) {
                if (selectedConversationGroup == null) return;
                msg = TextMessage.createGroup(me, selectedConversationGroup, messageText);
                addMessageToScreen(messageText, true, msg.getSentAt(), me);

            } else {
                if (selectedConversationUser == null) return;
                msg = TextMessage.createPrivate(me, selectedConversationUser, messageText);
                addMessageToScreen(messageText, true, msg.getSentAt(), me);
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
                    addMessageToScreen(msg.getContent(), isMe, msg.getSentAt(), msg.getSender());
                else if (msg.getTypeMessage() == TypeMessage.IMAGE) {
                    ImageView imageView = new ImageView();
                    imageView.setFitWidth(250);
                    imageView.setPreserveRatio(true);

                    addImageToScreen(imageView, isMe, msg.getSentAt(), msg.getSender());
                    client.sendRequestDownload(msg.getContent(), imageView);
                } else {
                    addFileToScreen(msg.getContent(), msg.getFileName(), null, isMe, msg.getSentAt(), msg.getSender());
                }
            }
        });
    }


    private void addMessageToScreen(String text, boolean isMe, String time, User sender) {
        Label messageNode = ChatMessageBuilder.buildTextMessage(text, isMe);
        HBox container = ChatMessageBuilder.buildMessageContainer(messageNode, sender, isMe, time);
        vboxMessage.getChildren().add(container);
    }

    private void addImageToScreen(ImageView imageView, boolean isMe, String time, User sender) {
        imageView.setFitWidth(ChatUiConstants.IMAGE_DISPLAY_WIDTH);
        imageView.setPreserveRatio(true);
        
        Stage stage = (Stage) imageView.getScene().getWindow();
        ChatMessageBuilder.addImageSaveMenu(imageView, stage);
        
        HBox container = ChatMessageBuilder.buildMessageContainer(imageView, sender, isMe, time);
        vboxMessage.getChildren().add(container);
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
            boolean isGroupMsg = msg.getGroupId() != null;
            boolean isBroadcastMsg = msg.isBroadcast() && !isGroupMsg;
            boolean isPrivateMsg = !msg.isBroadcast() && !isGroupMsg;


            boolean isCurrent = false;
            if (selectedItem != null) {
                if (isBroadcastMsg && selectedItem.equals(ChatUiConstants.LABEL_ANNOUNCEMENT)) {
                    isCurrent = true;
                } else if (isPrivateMsg && !isGroupMode && selectedConversationUser != null && msg.getSender().getId().equals(selectedConversationUser.getId())) {
                    isCurrent = true;
                } else if (isGroupMsg && isGroupMode && selectedConversationGroup != null && msg.getGroupId().equals(selectedConversationGroup.getId())) {
                    isCurrent = true;
                }
            }


            if (isCurrent) {
                if (msg.getTypeMessage() == TypeMessage.IMAGE) {
                    ImageMessage imgMsg = (ImageMessage) msg;
                    Image img = new Image(new ByteArrayInputStream(imgMsg.getImageData()));
                    addImageToScreen(new ImageView(img), false, msg.getSentAt(), msg.getSender());
                } else if (msg instanceof FileMessage) {
                    FileMessage fileMsg = (FileMessage) msg;
                    addFileToScreen(fileMsg.getContent(), fileMsg.getFileName(), fileMsg.getFileData(), false, msg.getSentAt(), msg.getSender());
                } else {
                    addMessageToScreen(msg.getContent(), false, msg.getSentAt(), msg.getSender());
                }
                scrollPane.setVvalue(1.0);
            } else {
                if (isBroadcastMsg) {
                    usersWithNewMessages.add(0);
                    int idx = lvChatList.getItems().indexOf(ChatUiConstants.LABEL_ANNOUNCEMENT);
                    if (idx != -1) lvChatList.getItems().set(idx, ChatUiConstants.LABEL_ANNOUNCEMENT);

                } else if (isPrivateMsg) {
                    usersWithNewMessages.add(msg.getSender().getId());

                    String label = msg.getSender().getNickname() + " (@" + msg.getSender().getUsername() + ")";

                    if (!isGroupMode) {

                        int idx = lvChatList.getItems().indexOf(label);
                        if (idx != -1) {
                            lvChatList.getItems().set(idx, label);
                        } else {

                            lvChatList.getItems().add(label);
                            conversationUserMap.put(label, msg.getSender());
                        }
                    }
                } else if (isGroupMsg) {


                    usersWithNewMessages.add(-msg.getGroupId());

                    if (isGroupMode) {
                        String groupLabel = null;
                        for (String key : conversationGroupMap.keySet()) {
                            if (conversationGroupMap.get(key).getId().equals(msg.getGroupId())) {
                                groupLabel = key;
                                break;
                            }
                        }

                        if (groupLabel != null) {
                            int idx = lvChatList.getItems().indexOf(groupLabel);
                            if (idx != -1) {
                                lvChatList.getItems().set(idx, groupLabel);
                            }
                        }
                    }
                }
            }


            lvChatList.refresh();
        });
    }

    @FXML
    void onFileButtonClick(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        try {
            byte[] fileBytes = ChatFileManager.chooseAndReadFile(stage);
            if (fileBytes == null) return;

            String fileName = "file";
            String extension = "";

            String selectedItem = lvChatList.getSelectionModel().getSelectedItem();
            if (selectedItem == null) return;

            boolean isImage = ChatFileManager.isImage(fileName);

            Message msg = null;

            if (isImage) {
                if (selectedItem.equals(ChatUiConstants.LABEL_ANNOUNCEMENT)) {
                    msg = ImageMessage.createBroadcast(me, fileBytes, extension);
                } else if (isGroupMode) {
                    if (selectedConversationGroup == null) return;
                    msg = ImageMessage.createGroup(me, selectedConversationGroup, fileBytes, extension);
                } else {
                    if (selectedConversationUser == null) return;
                    msg = ImageMessage.createPrivate(me, selectedConversationUser, fileBytes, extension);
                }
                ImageView imageView = ChatMessageBuilder.buildImageMessage(fileBytes);
                ChatMessageBuilder.addImageSaveMenu(imageView, stage);
                addImageToScreen(imageView, true, msg.getSentAt(), me);
            } else {
                if (selectedItem.equals(ChatUiConstants.LABEL_ANNOUNCEMENT)) {
                    msg = FileMessage.createBroadcast(me, fileBytes, fileName, extension);
                } else if (isGroupMode) {
                    if (selectedConversationGroup == null) return;
                    msg = FileMessage.createGroup(me, selectedConversationGroup, fileBytes, fileName, extension);
                } else {
                    if (selectedConversationUser == null) return;
                    msg = FileMessage.createPrivate(me, selectedConversationUser, fileBytes, fileName, extension);
                }
                addFileToScreen(null, fileName, fileBytes, true, msg.getSentAt(), me);
            }
            if (msg != null) {
                client.sendData(new DataPacket(TypeDataPacket.CHAT_MESSAGE, msg));
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Lỗi khi đọc file!");
        }
    }

    private void addFileToScreen(String serverUUID, String fileName, byte[] fileData, boolean isMe, String time, User sender) {
        HBox fileBox = ChatMessageBuilder.buildFileMessage(fileName, fileData, serverUUID, new ChatMessageBuilder.ChatFileDownloadListener() {
            @Override
            public void onFileReady(String name, byte[] data) {
                Stage stage = (Stage) vboxMessage.getScene().getWindow();
                ChatFileManager.saveFile(name, data, stage);
            }

            @Override
            public void onRequestDownload(String uuid, Label button) {
                pendingFileButtons.put(uuid, null);
                client.sendData(new DataPacket(TypeDataPacket.DOWNLOAD_FILE_REQUEST, uuid));
            }
        });
        
        HBox container = ChatMessageBuilder.buildMessageContainer(fileBox, sender, isMe, time);
        vboxMessage.getChildren().add(container);
    }

    private void downloadFile(String fileName, byte[] fileData) {
        Stage stage = (Stage) vboxMessage.getScene().getWindow();
        ChatFileManager.saveFile(fileName, fileData, stage);
    }


    public void updateOnlinePeople(List<User> users) {
        Platform.runLater(() -> {
            if (lvOnlinePeople == null || lvChatList == null) return;


            List<String> onlineNames = users.stream().map(user -> {
                String label = user.getNickname() + " (@" + user.getUsername() + ")";
                conversationUserMap.put(label, user);
                return label;
            }).collect(Collectors.toList());
            lvOnlinePeople.getItems().setAll(onlineNames);

            this.onlineUserIds.clear();
            for (User u : users) {
                this.onlineUserIds.add(u.getId());
            }
            lvOnlinePeople.refresh();
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

    public void onVideoCallButtonClick(ActionEvent actionEvent) {
        if (callManager != null) {
            callManager.startOutgoingVideoCall(selectedConversationUser);
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
                    callWindowController.setVideoCallAvailable(videoCallAvailable);
                    callWindowController.setVideoCallActive(videoCallActive);
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
            callWindowController.setVideoCallAvailable(videoCallAvailable);
            callWindowController.setVideoCallActive(videoCallActive);
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
            callWindowController.setOnVideoChanged(active -> {
                if (callManager != null) {
                    callManager.setVideoStreaming(active);
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
    public void setVideoCallAvailable(boolean available) {
        videoCallAvailable = available;
        Platform.runLater(() -> {
            if (callWindowController != null) {
                callWindowController.setVideoCallAvailable(available);
            }
        });
    }

    @Override
    public void setVideoCallActive(boolean active) {
        videoCallActive = active;
        Platform.runLater(() -> {
            if (callWindowController != null) {
                callWindowController.setVideoCallActive(active);
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
    public void showRemoteVideoFrame(byte[] frameBytes) {
        Platform.runLater(() -> {
            if (callWindowController != null) {
                callWindowController.showRemoteVideoFrame(frameBytes);
            }
        });
    }

    @Override
    public void showLocalVideoFrame(byte[] frameBytes) {
        Platform.runLater(() -> {
            if (callWindowController != null) {
                callWindowController.showLocalVideoFrame(frameBytes);
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
    public void clearLocalVideoFrame() {
        Platform.runLater(() -> {
            if (callWindowController != null) {
                callWindowController.clearLocalVideoFrame();
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
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, caller.getNickname() + " is calling you.", ButtonType.YES, ButtonType.NO);
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
        videoCallAvailable = false;
        videoCallActive = false;
    }

    @FXML
    void onTabAllClick(ActionEvent event) {
        isGroupMode = false;
        selectedConversationGroup = null;
        updateManageGroupButtonVisibility();

        if (txtSearchPeopleChat != null) {
            txtSearchPeopleChat.setVisible(true);
            txtSearchPeopleChat.setManaged(true);
        }

        isGroupMode = false;

        btnTabAll.getStyleClass().removeAll("toggle-btn", "toggle-btn-active");
        btnTabAll.getStyleClass().add("toggle-btn-active");

        btnTabGroups.getStyleClass().removeAll("toggle-btn", "toggle-btn-active");
        btnTabGroups.getStyleClass().add("toggle-btn");

        selectedConversationGroup = null;
        selectedConversationUser = null;
        conversationUserMap.clear();
        clearMessageArea();
        lvChatList.getItems().clear();

        lvChatList.getItems().add(ChatUiConstants.LABEL_ANNOUNCEMENT);

        Collection<User> usersToDisplay = allMembers;
        List<User> availableConversations = new ArrayList<>();
        for (User user : usersToDisplay) {
            if (me == null || !user.getUsername().equalsIgnoreCase(me.getUsername())) {
                availableConversations.add(user);
            }
        }

        if (availableConversations.isEmpty()) {
            lvChatList.getItems().add(ChatUiConstants.LABEL_NO_CONVERSATIONS);
        } else {
            for (User user : availableConversations) {
                String label = user.getNickname() + " (@" + user.getUsername() + ")";
                conversationUserMap.put(label, user);
                lvChatList.getItems().add(label);
            }
        }
        Platform.runLater(() -> {
            lvChatList.getSelectionModel().select(ChatUiConstants.LABEL_ANNOUNCEMENT);
        });
    }

    @FXML
    void onTabGroupsClick(ActionEvent event) {
        isGroupMode = true;
        selectedConversationGroup = null;
        updateManageGroupButtonVisibility();

        if (txtSearchPeopleChat != null) {
            txtSearchPeopleChat.setVisible(false);
            txtSearchPeopleChat.setManaged(false);
        }

        isGroupMode = true;

        btnTabGroups.getStyleClass().removeAll("toggle-btn", "toggle-btn-active");
        btnTabGroups.getStyleClass().add("toggle-btn-active");

        btnTabAll.getStyleClass().removeAll("toggle-btn", "toggle-btn-active");
        btnTabAll.getStyleClass().add("toggle-btn");


        lvChatList.getItems().clear();
        conversationGroupMap.clear();

        selectedConversationUser = null;
        selectedConversationGroup = null;
        clearMessageArea();

        contactNameTopBar.setText("");

        messageInput.getParent().setVisible(false);
        messageInput.getParent().setManaged(false);


        if (myGroupsList.isEmpty()) {
            return;
        }

        for (ChatGroup group : myGroupsList) {
            String label = group.getId() + "@" + group.getName();
            conversationGroupMap.put(label, group);
            lvChatList.getItems().add(label);
        }

        lvChatList.getSelectionModel().clearSelection();
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
                lvChatList.getItems().add(newGroup.getId() + "@" + newGroup.getName());
                conversationGroupMap.put(newGroup.getId() + "@" + newGroup.getName(), newGroup);
            }

            javafx.stage.Window.getWindows().stream().filter(w -> w instanceof Stage).map(w -> (Stage) w).filter(stage -> "Tạo Nhóm Mới".equals(stage.getTitle())).findFirst().ifPresent(Stage::close);

            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Đã tạo nhóm: " + newGroup.getName());
            alert.setHeaderText(null);

            String css = getClass().getResource("/org/proptit/localchat/create_group.css").toExternalForm();
            alert.getDialogPane().getStylesheets().add(css);

            alert.show();
        });
    }

    public void setMyGroupsList(List<ChatGroup> groups) {
        Platform.runLater(() -> {
            this.myGroupsList = groups;
            if (selectedConversationGroup != null) {
                for (ChatGroup g : groups) {
                    if (g.getId() == selectedConversationGroup.getId()) {
                        selectedConversationGroup = g;
                        break;
                    }
                }
            }

            if (isGroupMode) {
                onTabGroupsClick(null);
                if (selectedConversationGroup != null) {
                    lvChatList.getSelectionModel().select(selectedConversationGroup.getId() + "@" + selectedConversationGroup.getName());
                }
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

    @FXML
    void onManageGroupClick(ActionEvent event) {

        ContextMenu menu = new ContextMenu();
        MenuItem addMember = new MenuItem("Thêm thành viên");
        MenuItem removeMember = new MenuItem("Xóa thành viên");

        addMember.setOnAction(e -> openGroupManagerWindow("ADD"));
        removeMember.setOnAction(e -> openGroupManagerWindow("REMOVE"));

        menu.getItems().addAll(addMember, removeMember);
        menu.show(btnManageGroup, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    private void openGroupManagerWindow(String mode) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/proptit/localchat/manage_group.fxml"));
            Parent root = loader.load();

            GroupManagerController controller = loader.getController();
            controller.init(client, me, selectedConversationGroup, allMembers, mode);

            Stage stage = new Stage();
            stage.setTitle(mode.equals("ADD") ? "Thêm thành viên" : "Xóa thành viên");
            stage.setScene(new Scene(root));
            stage.initOwner(btnManageGroup.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateManageGroupButtonVisibility() {
        if (isGroupMode && selectedConversationGroup != null && me != null) {
            boolean isCreator = selectedConversationGroup.getCreatedBy() != null && selectedConversationGroup.getCreatedBy().getId().equals(me.getId());
            btnManageGroup.setVisible(isCreator);
            btnManageGroup.setManaged(isCreator);
        } else {
            btnManageGroup.setVisible(false);
            btnManageGroup.setManaged(false);
        }
    }

    public void updateGroupSilent(ChatGroup group) {
        if (group == null) return;

        Platform.runLater(() -> {
            if ("DELETED_SIGNAL".equals(group.getName())) {
                for (int i = myGroupsList.size() - 1; i >= 0; i--) {
                    if (myGroupsList.get(i).getId().equals(group.getId())) {
                        myGroupsList.remove(i);
                    }
                }
                String labelToRemove = null;
                for (String label : conversationGroupMap.keySet()) {
                    if (conversationGroupMap.get(label).getId().equals(group.getId())) {
                        labelToRemove = label;
                        break;
                    }
                }
                if (labelToRemove != null) {
                    lvChatList.getItems().remove(labelToRemove);
                    conversationGroupMap.remove(labelToRemove);
                }
                if (selectedConversationGroup != null && selectedConversationGroup.getId().equals(group.getId())) {
                    clearMessageArea();
                    selectedConversationGroup = null;
                    messageInput.getParent().setVisible(false);
                }
                return;
            }


            String label = group.getId() + "@" + group.getName();

            boolean existsInList = false;
            for (int i = 0; i < myGroupsList.size(); i++) {
                if (myGroupsList.get(i).getId().equals(group.getId())) {
                    myGroupsList.set(i, group);
                    existsInList = true;
                    break;
                }
            }
            if (!existsInList) myGroupsList.add(group);
            conversationGroupMap.put(label, group);

            if (isGroupMode) {
                if (!lvChatList.getItems().contains(label)) {
                    lvChatList.getItems().add(label);
                }
            }

            if (selectedConversationGroup != null && selectedConversationGroup.getId().equals(group.getId())) {
                this.selectedConversationGroup = group;
                if (isGroupMode) {
                    lvChatList.getSelectionModel().select(label);
                }
                updateManageGroupButtonVisibility();
            }

            lvChatList.refresh();

        });
    }

    @FXML
    void onGroupInfoClick(ActionEvent event) {
        if (selectedConversationGroup == null) return;
        ContextMenu contextMenu = new ContextMenu();
        MenuItem viewMembersItem = new MenuItem("Thành viên trong đoạn chat");
        viewMembersItem.setOnAction(e -> showGroupMembers());
        MenuItem leaveGroupItem = new MenuItem("Rời nhóm");
        leaveGroupItem.setOnAction(e -> handleLeaveGroup());

        contextMenu.getItems().addAll(viewMembersItem, leaveGroupItem);
        contextMenu.show(btnGroupInfo, javafx.geometry.Side.BOTTOM, 0, 5);
    }

    private void showGroupMembers() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông tin nhóm");
        alert.setHeaderText(null);
        alert.setGraphic(null);

        VBox root = new VBox(15);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(10, 25, 10, 25));

        root.setPrefWidth(350);
        root.setPrefHeight(450);

        Label titleLabel = new Label("DANH SÁCH THÀNH VIÊN");
        titleLabel.getStyleClass().add("header-label");

        VBox groupInfoBox = new VBox(2);
        groupInfoBox.setAlignment(Pos.CENTER);
        Label labelNhom = new Label("NHÓM");
        labelNhom.setStyle("-fx-text-fill: #7a829a; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label groupName = new Label(selectedConversationGroup.getName().toUpperCase());
        groupName.setStyle("-fx-text-fill: #b388ff; -fx-font-size: 18px; -fx-font-weight: bold;");
        groupInfoBox.getChildren().addAll(labelNhom, groupName);

        Label listTitle = new Label("DANH SÁCH THÀNH VIÊN");
        listTitle.setStyle("-fx-text-fill: #7a829a; -fx-font-size: 11px; -fx-font-weight: bold;");
        HBox listTitleWrapper = new HBox(listTitle);
        listTitleWrapper.setAlignment(Pos.CENTER_LEFT);

        ListView<User> lv = new ListView<>();
        lv.getStyleClass().add("list-view");

        VBox.setVgrow(lv, javafx.scene.layout.Priority.ALWAYS);

        lv.setItems(FXCollections.observableArrayList(selectedConversationGroup.getMembers()));
        lv.setCellFactory(param -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    String role = (user.getId().equals(selectedConversationGroup.getCreatedBy().getId())) ? " (Trưởng nhóm)" : "";
                    Label name = new Label(user.getNickname() + role + " (@" + user.getUsername() + ")");
                    name.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

                    HBox cell = new HBox(10, name);
                    cell.setAlignment(Pos.CENTER_LEFT);
                    cell.setPadding(new Insets(5, 0, 5, 5));
                    setGraphic(cell);
                }
            }
        });

        root.getChildren().addAll(titleLabel, groupInfoBox, listTitleWrapper, lv);
        alert.getDialogPane().setContent(root);

        java.net.URL cssUrl = getClass().getResource("/org/proptit/localchat/create_group.css");
        if (cssUrl != null) {
            alert.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
            alert.getDialogPane().getStyleClass().add("dialog-pane");
        }

        alert.show();
    }

    private void handleLeaveGroup() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Bạn có chắc chắn muốn rời khỏi nhóm này không?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        java.net.URL cssUrl = getClass().getResource("/org/proptit/localchat/create_group.css");
        if (cssUrl != null) confirm.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                client.sendData(new DataPacket(TypeDataPacket.LEAVE_GROUP_REQUEST, selectedConversationGroup.getId()));
                btnTabAll.fire();
                contactNameTopBar.setText("");
                vboxMessage.getChildren().clear();
            }
        });
    }

}