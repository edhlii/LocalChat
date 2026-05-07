package org.proptit.localchat.client.controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.proptit.localchat.client.networks.SocketClient;
import org.proptit.localchat.common.enums.TypeDataPacket;
import org.proptit.localchat.common.models.ChatGroup;
import org.proptit.localchat.common.models.DataPacket;
import org.proptit.localchat.common.models.User;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GroupManagerController {

    @FXML
    private Label lblHeader;
    @FXML
    private Label lblGroupName;
    @FXML
    private Label lblListTitle;
    @FXML
    private ListView<User> lvMembers;
    @FXML
    private Button btnConfirm;

    private SocketClient client;
    private User me;
    private ChatGroup group;
    private String mode;
    private final List<User> selectedUsers = new ArrayList<>();

    public void init(SocketClient client, User me, ChatGroup group, List<User> allMembers, String mode) {
        this.client = client;
        this.me = me;
        this.group = group;
        this.mode = mode;

        lblGroupName.setText(group.getName());

        if (mode.equals("ADD")) {
            lblHeader.setText("ADD MEMBERS");
            lblListTitle.setText("SELECT MEMBERS");
            btnConfirm.setText("ADD");
            btnConfirm.setStyle("-fx-background-color: #AD7BFF; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        } else {
            lblHeader.setText("DELETE MEMBERS");
            lblListTitle.setText("CURRENT MEMBERS");
            btnConfirm.setText("DELETE");
            btnConfirm.setStyle("-fx-background-color: #ff5252; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        }

        List<User> displayList = new ArrayList<>();

        Set<Integer> currentMemberIds = new HashSet<>();
        for (User user : group.getMembers()) {
            currentMemberIds.add(user.getId());
        }

        if (mode.equals("ADD")) {
            for (User u : allMembers) {
                if (!currentMemberIds.contains(u.getId()) && !u.getId().equals(me.getId())) {
                    displayList.add(u);
                }
            }
        } else {
            for (User u : group.getMembers()) {
                if (!u.getId().equals(me.getId())) {
                    displayList.add(u);
                }
            }
        }

        lvMembers.setItems(FXCollections.observableArrayList(displayList));
        setupCellFactory();
    }

    private static Integer getId(User user) {
        return user.getId();
    }


    private void setupCellFactory() {
        lvMembers.setCellFactory(param -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    CheckBox checkBox = new CheckBox(user.getNickname() + " (@" + user.getUsername() + ")");
                    checkBox.getStyleClass().add("check-box");
                    checkBox.setSelected(selectedUsers.contains(user));

                    checkBox.setOnAction(e -> {
                        if (checkBox.isSelected()) {
                            if (!selectedUsers.contains(user)) selectedUsers.add(user);
                        } else {
                            selectedUsers.remove(user);
                        }
                    });

                    setGraphic(checkBox);
                    setText(null);
                }
            }
        });
    }

    @FXML
    void onConfirmClick(ActionEvent event) {
        if (selectedUsers.isEmpty()) {
            return;
        }
        ChatGroup payload = new ChatGroup(group.getId(), group.getName(), me, new ArrayList<>(selectedUsers));
        TypeDataPacket type = mode.equals("ADD") ?
                TypeDataPacket.ADD_GROUP_MEMBERS : TypeDataPacket.REMOVE_GROUP_MEMBERS;
        if (client != null) {
            client.sendData(new DataPacket(type, payload));
        }
        ((Stage) btnConfirm.getScene().getWindow()).close();
    }


}