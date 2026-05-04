package org.proptit.localchat.client.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.proptit.localchat.client.networks.SocketClient;
import org.proptit.localchat.common.enums.TypeDataPacket;
import org.proptit.localchat.common.models.ChatGroup;
import org.proptit.localchat.common.models.DataPacket;
import org.proptit.localchat.common.models.User;
import org.proptit.localchat.common.models.message.FileMessage;
import org.proptit.localchat.common.models.message.ImageMessage;
import org.proptit.localchat.common.models.message.Message;
import org.proptit.localchat.common.models.message.TextMessage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ChatConversationManager {
    static final String ANNOUNCEMENT_LABEL = "Thông báo chung";

    private final SocketClient client;
    private final User me;
    private final ListView<String> lvOnlinePeople;
    private final ListView<String> lvChatList;
    private final TextField txtSearchPeopleChat;
    private final TextField messageInput;
    private final Label contactNameTopBar;
    private final Button sendMessageAllButton;
    private final Button btnTabAll;
    private final Button btnTabGroups;
    private final Button btnManageGroup;
    private final Button btnGroupInfo;
    private final ChatMessageRenderer messageRenderer;

    private final Map<String, User> conversationUserMap = new HashMap<>();
    private List<User> allMembers = new ArrayList<>();
    private final Set<Integer> onlineUserIds = new HashSet<>();
    private User selectedConversationUser;
    private List<ChatGroup> myGroupsList = new ArrayList<>();
    private final Map<String, ChatGroup> conversationGroupMap = new HashMap<>();
    private final Set<Integer> usersWithNewMessages = new HashSet<>();
    private boolean groupMode;
    private ChatGroup selectedConversationGroup;

    ChatConversationManager(SocketClient client,
                            User me,
                            ListView<String> lvOnlinePeople,
                            ListView<String> lvChatList,
                            TextField txtSearchPeopleChat,
                            TextField messageInput,
                            Label contactNameTopBar,
                            Button sendMessageAllButton,
                            Button btnTabAll,
                            Button btnTabGroups,
                            Button btnManageGroup,
                            Button btnGroupInfo,
                            ChatMessageRenderer messageRenderer) {
        this.client = client;
        this.me = me;
        this.lvOnlinePeople = lvOnlinePeople;
        this.lvChatList = lvChatList;
        this.txtSearchPeopleChat = txtSearchPeopleChat;
        this.messageInput = messageInput;
        this.contactNameTopBar = contactNameTopBar;
        this.sendMessageAllButton = sendMessageAllButton;
        this.btnTabAll = btnTabAll;
        this.btnTabGroups = btnTabGroups;
        this.btnManageGroup = btnManageGroup;
        this.btnGroupInfo = btnGroupInfo;
        this.messageRenderer = messageRenderer;
    }

    void install() {
        messageRenderer.setupListViewCustomCells(lvOnlinePeople, lvChatList, conversationUserMap, conversationGroupMap, usersWithNewMessages, onlineUserIds, () -> groupMode);

        if (messageRenderer != null) {
            messageRenderer.clearMessageArea();
        }

        if (btnManageGroup != null) {
            btnManageGroup.setVisible(false);
            btnManageGroup.setManaged(false);
        }

        if (lvChatList != null) {
            lvChatList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == null) {
                    return;
                }

                if (newValue.equals(ANNOUNCEMENT_LABEL)) {
                    usersWithNewMessages.remove(0);
                    lvChatList.refresh();
                    client.sendData(new DataPacket(TypeDataPacket.MARK_AS_READ, 0));

                    selectedConversationUser = null;
                    selectedConversationGroup = null;
                    messageRenderer.clearMessageArea();

                    contactNameTopBar.setText(ANNOUNCEMENT_LABEL);
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

                if (groupMode) {
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
                            messageRenderer.clearMessageArea();
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

                        if (selectedConversationUser == null || !newUser.getId().equals(selectedConversationUser.getId())) {
                            usersWithNewMessages.remove(newUser.getId());
                            lvChatList.refresh();

                            client.sendData(new DataPacket(TypeDataPacket.MARK_AS_READ, newUser.getId()));

                            selectedConversationUser = newUser;
                            messageRenderer.clearMessageArea();
                            System.out.println("DEBUG: Load lịch sử với " + selectedConversationUser.getNickname());
                            client.sendData(new DataPacket(TypeDataPacket.GET_HISTORY_REQUEST, selectedConversationUser.getId()));
                        }
                    }
                }
            });
        }

        if (sendMessageAllButton != null) {
            boolean isManager = me.isManager();
            sendMessageAllButton.setVisible(isManager);
            sendMessageAllButton.setManaged(isManager);
        }

        if (lvOnlinePeople != null) {
            lvOnlinePeople.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && conversationUserMap.containsKey(newVal)) {
                    lvChatList.getSelectionModel().select(newVal);
                    selectedConversationUser = conversationUserMap.get(newVal);
                    contactNameTopBar.setText(selectedConversationUser.getNickname());
                    messageRenderer.clearMessageArea();
                }
            });
        }

        if (txtSearchPeopleChat != null) {
            txtSearchPeopleChat.textProperty().addListener((observable, oldValue, newValue) -> renderChatListByKeyword(newValue));
        }
    }

    void renderChatListByKeyword(String keyword) {
        String normalizedKeyword = (keyword == null) ? "" : keyword.trim().toLowerCase();

        lvChatList.getItems().clear();
        lvChatList.getItems().add(ANNOUNCEMENT_LABEL);

        for (User user : allMembers) {
            if (user.getId().equals(me.getId())) {
                continue;
            }

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

    void setAllMembers(List<User> members) {
        Platform.runLater(() -> {
            this.allMembers = members;
            if (!groupMode) {
                lvChatList.getItems().clear();
                lvChatList.getItems().add(ANNOUNCEMENT_LABEL);

                for (User user : allMembers) {
                    if (user.getId().equals(me.getId())) {
                        continue;
                    }
                    String label = user.getNickname() + " (@" + user.getUsername() + ")";
                    conversationUserMap.put(label, user);
                    lvChatList.getItems().add(label);
                }
            } else {
                conversationUserMap.clear();
                for (User user : allMembers) {
                    if (user.getId().equals(me.getId())) {
                        continue;
                    }
                    String label = user.getNickname() + " (@" + user.getUsername() + ")";
                    conversationUserMap.put(label, user);
                }
            }

            renderChatListByKeyword(txtSearchPeopleChat != null ? txtSearchPeopleChat.getText() : "");

            if (!lvChatList.getItems().isEmpty() && (txtSearchPeopleChat == null || txtSearchPeopleChat.getText().isEmpty())) {
                lvChatList.getSelectionModel().select(0);
            }
        });
    }

    void updateOnlinePeople(List<User> users) {
        Platform.runLater(() -> {
            if (lvOnlinePeople == null || lvChatList == null) {
                return;
            }

            List<String> onlineNames = new ArrayList<>();
            for (User user : users) {
                String label = user.getNickname() + " (@" + user.getUsername() + ")";
                conversationUserMap.put(label, user);
                onlineNames.add(label);
            }
            lvOnlinePeople.getItems().setAll(onlineNames);

            onlineUserIds.clear();
            for (User user : users) {
                onlineUserIds.add(user.getId());
            }

            lvOnlinePeople.refresh();
            lvChatList.refresh();
        });
    }

    void setMyGroupsList(List<ChatGroup> groups) {
        Platform.runLater(() -> {
            this.myGroupsList = groups;
            if (selectedConversationGroup != null) {
                for (ChatGroup group : groups) {
                    if (group.getId() == selectedConversationGroup.getId()) {
                        selectedConversationGroup = group;
                        break;
                    }
                }
            }

            if (groupMode) {
                onTabGroupsClick();
                if (selectedConversationGroup != null) {
                    lvChatList.getSelectionModel().select(selectedConversationGroup.getId() + "@" + selectedConversationGroup.getName());
                }
            }
        });
    }

    void setOfflineMessages(List<Integer> unreadIds) {
        Platform.runLater(() -> {
            if (unreadIds != null && !unreadIds.isEmpty()) {
                usersWithNewMessages.addAll(unreadIds);
                lvChatList.refresh();
            }
        });
    }

    void onTabAllClick() {
        groupMode = false;
        selectedConversationGroup = null;
        updateManageGroupButtonVisibility();

        if (txtSearchPeopleChat != null) {
            txtSearchPeopleChat.setVisible(true);
            txtSearchPeopleChat.setManaged(true);
        }

        btnTabAll.getStyleClass().removeAll("toggle-btn", "toggle-btn-active");
        btnTabAll.getStyleClass().add("toggle-btn-active");

        btnTabGroups.getStyleClass().removeAll("toggle-btn", "toggle-btn-active");
        btnTabGroups.getStyleClass().add("toggle-btn");

        selectedConversationGroup = null;
        selectedConversationUser = null;
        conversationUserMap.clear();
        messageRenderer.clearMessageArea();
        lvChatList.getItems().clear();
        lvChatList.getItems().add(ANNOUNCEMENT_LABEL);

        Collection<User> usersToDisplay = allMembers;
        List<User> availableConversations = new ArrayList<>();
        for (User user : usersToDisplay) {
            if (me == null || !user.getUsername().equalsIgnoreCase(me.getUsername())) {
                availableConversations.add(user);
            }
        }

        if (availableConversations.isEmpty()) {
            lvChatList.getItems().add("No conversations");
        } else {
            for (User user : availableConversations) {
                String label = user.getNickname() + " (@" + user.getUsername() + ")";
                conversationUserMap.put(label, user);
                lvChatList.getItems().add(label);
            }
        }

        Platform.runLater(() -> lvChatList.getSelectionModel().select(ANNOUNCEMENT_LABEL));
    }

    void onTabGroupsClick() {
        groupMode = true;
        selectedConversationGroup = null;
        updateManageGroupButtonVisibility();

        if (txtSearchPeopleChat != null) {
            txtSearchPeopleChat.setVisible(false);
            txtSearchPeopleChat.setManaged(false);
        }

        btnTabGroups.getStyleClass().removeAll("toggle-btn", "toggle-btn-active");
        btnTabGroups.getStyleClass().add("toggle-btn-active");

        btnTabAll.getStyleClass().removeAll("toggle-btn", "toggle-btn-active");
        btnTabAll.getStyleClass().add("toggle-btn");

        lvChatList.getItems().clear();
        conversationGroupMap.clear();

        selectedConversationUser = null;
        selectedConversationGroup = null;
        messageRenderer.clearMessageArea();
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

    void onCreateGroupClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/proptit/localchat/create_group.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Tạo Nhóm Mới");
            stage.setScene(new Scene(loader.load()));

            CreateGroupController controller = loader.getController();
            controller.setup(client, me, allMembers);
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void onGroupCreatedSuccess(ChatGroup newGroup) {
        Platform.runLater(() -> {
            myGroupsList.add(newGroup);

            if (groupMode) {
                lvChatList.getItems().add(newGroup.getId() + "@" + newGroup.getName());
                conversationGroupMap.put(newGroup.getId() + "@" + newGroup.getName(), newGroup);
            }

            javafx.stage.Window.getWindows().stream()
                    .filter(w -> w instanceof Stage)
                    .map(w -> (Stage) w)
                    .filter(stage -> "Tạo Nhóm Mới".equals(stage.getTitle()))
                    .findFirst()
                    .ifPresent(Stage::close);

            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Đã tạo nhóm: " + newGroup.getName());
            alert.setHeaderText(null);
            String css = getClass().getResource("/org/proptit/localchat/create_group.css").toExternalForm();
            alert.getDialogPane().getStylesheets().add(css);
            alert.show();
        });
    }

    void onManageGroupClick() {
        if (selectedConversationGroup == null) {
            return;
        }

        ContextMenu menu = new ContextMenu();
        MenuItem addMember = new MenuItem("Thêm thành viên");
        MenuItem removeMember = new MenuItem("Xóa thành viên");

        addMember.setOnAction(e -> openGroupManagerWindow("ADD"));
        removeMember.setOnAction(e -> openGroupManagerWindow("REMOVE"));

        menu.getItems().addAll(addMember, removeMember);
        menu.show(btnManageGroup, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    void onGroupInfoClick() {
        if (selectedConversationGroup == null) {
            return;
        }

        ContextMenu contextMenu = new ContextMenu();
        MenuItem viewMembersItem = new MenuItem("Thành viên trong đoạn chat");
        viewMembersItem.setOnAction(e -> showGroupMembers());
        MenuItem leaveGroupItem = new MenuItem("Rời nhóm");
        leaveGroupItem.setOnAction(e -> handleLeaveGroup());
        contextMenu.getItems().addAll(viewMembersItem, leaveGroupItem);
        contextMenu.show(btnGroupInfo, javafx.geometry.Side.BOTTOM, 0, 5);
    }

    void updateGroupSilent(ChatGroup group) {
        if (group == null) {
            return;
        }

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
                    messageRenderer.clearMessageArea();
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
            if (!existsInList) {
                myGroupsList.add(group);
            }

            conversationGroupMap.put(label, group);

            if (groupMode && !lvChatList.getItems().contains(label)) {
                lvChatList.getItems().add(label);
            }

            if (selectedConversationGroup != null && selectedConversationGroup.getId().equals(group.getId())) {
                selectedConversationGroup = group;
                if (groupMode) {
                    lvChatList.getSelectionModel().select(label);
                }
                updateManageGroupButtonVisibility();
            }

            lvChatList.refresh();
        });
    }

    void handleReceivedMessage(Message msg) {
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
                if (isBroadcastMsg && selectedItem.equals(ANNOUNCEMENT_LABEL)) {
                    isCurrent = true;
                } else if (isPrivateMsg && !groupMode && selectedConversationUser != null && msg.getSender().getId().equals(selectedConversationUser.getId())) {
                    isCurrent = true;
                } else if (isGroupMsg && groupMode && selectedConversationGroup != null && msg.getGroupId().equals(selectedConversationGroup.getId())) {
                    isCurrent = true;
                }
            }

            if (isCurrent) {
                if (msg.getTypeMessage() == org.proptit.localchat.common.enums.TypeMessage.IMAGE) {
                    ImageMessage imgMsg = (ImageMessage) msg;
                    Image image = new Image(new ByteArrayInputStream(imgMsg.getImageData()));
                    messageRenderer.addImageToScreen(new ImageView(image), false, msg.getSentAt(), msg.getSender());
                } else if (msg instanceof FileMessage) {
                    FileMessage fileMsg = (FileMessage) msg;
                    messageRenderer.addFileToScreen(fileMsg.getContent(), fileMsg.getFileName(), fileMsg.getFileData(), false, msg.getSentAt(), msg.getSender());
                } else {
                    messageRenderer.addMessageToScreen(msg.getContent(), false, msg.getSentAt());
                }
            } else {
                if (isBroadcastMsg) {
                    usersWithNewMessages.add(0);
                    int idx = lvChatList.getItems().indexOf(ANNOUNCEMENT_LABEL);
                    if (idx != -1) {
                        lvChatList.getItems().set(idx, ANNOUNCEMENT_LABEL);
                    }
                } else if (isPrivateMsg) {
                    usersWithNewMessages.add(msg.getSender().getId());
                    String label = msg.getSender().getNickname() + " (@" + msg.getSender().getUsername() + ")";

                    if (!groupMode) {
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

                    if (groupMode) {
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

    User getSelectedConversationUser() {
        return selectedConversationUser;
    }

    ChatGroup getSelectedConversationGroup() {
        return selectedConversationGroup;
    }

    boolean isGroupMode() {
        return groupMode;
    }

    List<User> getAllMembers() {
        return allMembers;
    }

    User resolveUser(String username, String nickname) {
        String searchKey = nickname + " (@" + username + ")";
        User onlineUser = conversationUserMap.get(searchKey);
        if (onlineUser != null) {
            return onlineUser;
        }

        User user = new User(username);
        user.setNickname((nickname != null && !nickname.isBlank()) ? nickname : username);
        return user;
    }

    Set<Integer> getOnlineUserIds() {
        return onlineUserIds;
    }

    Map<String, User> getConversationUserMap() {
        return conversationUserMap;
    }

    private void updateManageGroupButtonVisibility() {
        if (groupMode && selectedConversationGroup != null && me != null) {
            boolean isCreator = selectedConversationGroup.getCreatedBy() != null && selectedConversationGroup.getCreatedBy().getId().equals(me.getId());
            btnManageGroup.setVisible(isCreator);
            btnManageGroup.setManaged(isCreator);
        } else {
            btnManageGroup.setVisible(false);
            btnManageGroup.setManaged(false);
        }
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
        } catch (IOException ex) {
            ex.printStackTrace();
        }
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
        if (cssUrl != null) {
            confirm.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
        }

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                client.sendData(new DataPacket(TypeDataPacket.LEAVE_GROUP_REQUEST, selectedConversationGroup.getId()));
                btnTabAll.fire();
                contactNameTopBar.setText("");
                messageRenderer.clearMessageArea();
            }
        });
    }

    String resolveLocalAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ex) {
            return "127.0.0.1";
        }
    }
}