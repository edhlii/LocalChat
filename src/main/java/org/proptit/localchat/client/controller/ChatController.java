package org.proptit.localchat.client.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.proptit.localchat.client.networks.SocketClient;
import org.proptit.localchat.common.enums.TypeDataPacket;
import org.proptit.localchat.common.models.ChatGroup;
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
import java.util.List;

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
    private ChatMessageRenderer messageRenderer;
    private ChatConversationManager conversationManager;
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

    public void init(SocketClient client, User me) {
        this.client = client;
        this.me = me;
        this.messageRenderer = new ChatMessageRenderer(vboxMessage, scrollPane, client);
        this.conversationManager = new ChatConversationManager(client, me, lvOnlinePeople, lvChatList, txtSearchPeopleChat, messageInput, contactNameTopBar, sendMessageAllButton, btnTabAll, btnTabGroups, btnManageGroup, btnGroupInfo, messageRenderer);
        this.callManager = new ChatCallManager(client, me, this);

        vboxMessage.heightProperty().addListener((observable, oldValue, newValue) -> scrollPane.setVvalue(1.0));
        conversationManager.install();

        if (messageInput != null) {
            messageInput.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    onSendButtonClick(new ActionEvent());
                    event.consume();
                }
            });
        }

        client.sendData(new DataPacket(TypeDataPacket.GET_CHAT_CONTACTS, null));
        client.sendData(new DataPacket(TypeDataPacket.GET_MY_GROUPS_REQUEST, me.getId()));
        client.sendData(new DataPacket(TypeDataPacket.GET_OFFLINE_NOTIFICATIONS, null));
    }

    @FXML
    void onSendButtonClick(ActionEvent event) {
        if (conversationManager == null || messageRenderer == null) {
            return;
        }

        String messageText = messageInput.getText().trim();
        if (messageText.isEmpty()) {
            return;
        }

        String selectedItem = lvChatList.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            return;
        }

        Message msg = null;
        if (selectedItem.equals(ChatConversationManager.ANNOUNCEMENT_LABEL)) {
            msg = TextMessage.createBroadcast(me, messageText);
            messageRenderer.addMessageToScreen(messageText, true, msg.getSentAt());
        } else if (conversationManager.isGroupMode()) {
            ChatGroup selectedGroup = conversationManager.getSelectedConversationGroup();
            if (selectedGroup == null) {
                return;
            }
            msg = TextMessage.createGroup(me, selectedGroup, messageText);
            messageRenderer.addMessageToScreen(messageText, true, msg.getSentAt());
        } else {
            User selectedUser = conversationManager.getSelectedConversationUser();
            if (selectedUser == null) {
                return;
            }
            msg = TextMessage.createPrivate(me, selectedUser, messageText);
            messageRenderer.addMessageToScreen(messageText, true, msg.getSentAt());
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

    public void loadHistory(List<Message> history) {
        Platform.runLater(() -> {
            if (messageRenderer != null) {
                messageRenderer.loadHistory(history, me);
            }
        });
    }

    @FXML
    void onFileButtonClick(ActionEvent event) {
        File file = FileUtils.chooseFile((Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow());
        if (file == null) {
            return;
        }

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
            if (selectedItem == null) {
                return;
            }

            Message msg = null;
            if (isImage) {
                if (selectedItem.equals(ChatConversationManager.ANNOUNCEMENT_LABEL)) {
                    msg = ImageMessage.createBroadcast(me, fileBytes, extension);
                } else if (conversationManager.isGroupMode()) {
                    ChatGroup selectedGroup = conversationManager.getSelectedConversationGroup();
                    if (selectedGroup == null) {
                        return;
                    }
                    msg = ImageMessage.createGroup(me, selectedGroup, fileBytes, extension);
                } else {
                    User selectedUser = conversationManager.getSelectedConversationUser();
                    if (selectedUser == null) {
                        return;
                    }
                    msg = ImageMessage.createPrivate(me, selectedUser, fileBytes, extension);
                }
                messageRenderer.addImageToScreen(new ImageView(new Image(new ByteArrayInputStream(fileBytes))), true, msg.getSentAt(), me);
            } else {
                if (selectedItem.equals(ChatConversationManager.ANNOUNCEMENT_LABEL)) {
                    msg = FileMessage.createBroadcast(me, fileBytes, fileName, extension);
                } else if (conversationManager.isGroupMode()) {
                    ChatGroup selectedGroup = conversationManager.getSelectedConversationGroup();
                    if (selectedGroup == null) {
                        return;
                    }
                    msg = FileMessage.createGroup(me, selectedGroup, fileBytes, fileName, extension);
                } else {
                    User selectedUser = conversationManager.getSelectedConversationUser();
                    if (selectedUser == null) {
                        return;
                    }
                    msg = FileMessage.createPrivate(me, selectedUser, fileBytes, fileName, extension);
                }
                messageRenderer.addFileToScreen(null, fileName, fileBytes, true, msg.getSentAt(), me);
            }

            if (msg != null) {
                client.sendData(new DataPacket(TypeDataPacket.CHAT_MESSAGE, msg));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            System.out.println("Lỗi khi đọc file!");
        }
    }

    public void setAllMembers(List<User> members) {
        if (conversationManager != null) {
            conversationManager.setAllMembers(members);
        }
    }

    public void updateOnlinePeople(List<User> users) {
        if (conversationManager != null) {
            conversationManager.updateOnlinePeople(users);
        }
    }

    public void handleFileDownloadResponse(String fileName, byte[] fileData) {
        Platform.runLater(() -> {
            if (messageRenderer != null) {
                messageRenderer.handleFileDownloadResponse(fileName, fileData);
            }
        });
    }

    public void onCallButtonClick(ActionEvent actionEvent) {
        if (callManager != null) {
            callManager.startOutgoingCall(conversationManager != null ? conversationManager.getSelectedConversationUser() : null);
        }
    }

    public void onVideoCallButtonClick(ActionEvent actionEvent) {
        if (callManager != null) {
            callManager.startOutgoingVideoCall(conversationManager != null ? conversationManager.getSelectedConversationUser() : null);
        }
    }

    public void receiveMessage(Message msg) {
        if (conversationManager != null) {
            conversationManager.handleReceivedMessage(msg);
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
        } catch (IOException ex) {
            ex.printStackTrace();
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
        if (conversationManager != null) {
            return conversationManager.resolveUser(username, nickname);
        }

        User user = new User(username);
        user.setNickname((nickname != null && !nickname.isBlank()) ? nickname : username);
        return user;
    }

    @Override
    public String resolveLocalAddress() {
        if (conversationManager != null) {
            return conversationManager.resolveLocalAddress();
        }

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
        if (conversationManager != null) {
            conversationManager.onTabAllClick();
        }
    }

    @FXML
    void onTabGroupsClick(ActionEvent event) {
        if (conversationManager != null) {
            conversationManager.onTabGroupsClick();
        }
    }

    @FXML
    void onCreateGroupClick(ActionEvent event) {
        if (conversationManager != null) {
            conversationManager.onCreateGroupClick();
        }
    }

    public void onGroupCreatedSuccess(ChatGroup newGroup) {
        if (conversationManager != null) {
            conversationManager.onGroupCreatedSuccess(newGroup);
        }
    }

    public void setMyGroupsList(List<ChatGroup> groups) {
        if (conversationManager != null) {
            conversationManager.setMyGroupsList(groups);
        }
    }

    public void setOfflineMessages(List<Integer> unreadIds) {
        if (conversationManager != null) {
            conversationManager.setOfflineMessages(unreadIds);
        }
    }

    @FXML
    void onManageGroupClick(ActionEvent event) {
        if (conversationManager != null) {
            conversationManager.onManageGroupClick();
        }
    }

    @FXML
    void onGroupInfoClick(ActionEvent event) {
        if (conversationManager != null) {
            conversationManager.onGroupInfoClick();
        }
    }

    public void updateGroupSilent(ChatGroup group) {
        if (conversationManager != null) {
            conversationManager.updateGroupSilent(group);
        }
    }
}