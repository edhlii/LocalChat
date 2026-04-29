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
import java.util.List;

public class CreateGroupController {

    @FXML private TextField txtGroupName;
    @FXML private ListView<User> lvMembers;
    @FXML private Label lblError;
    @FXML private Button btnCancel;
    @FXML private Button btnCreate;

    private SocketClient client;
    private User me;

    private List<User> selectedMembersForGroup = new ArrayList<>();

    public void setup(SocketClient client, User me, List<User> onlineUsers) {
        this.client = client;
        this.me = me;

        lvMembers.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        List<User> selectableUsers = new ArrayList<>();
        for (User u : onlineUsers) {
            if (u.getId() != me.getId()) {
                selectableUsers.add(u);
            }
        }
        lvMembers.setItems(FXCollections.observableArrayList(selectableUsers));

        lvMembers.setCellFactory(param -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    CheckBox checkBox = new CheckBox(user.getNickname() + " (@" + user.getUsername() + ")");
                    checkBox.getStyleClass().add("check-box");
                    checkBox.setSelected(selectedMembersForGroup.contains(user));
                    checkBox.setOnAction(event -> {
                        if (checkBox.isSelected()) {
                            if (!selectedMembersForGroup.contains(user)) {
                                selectedMembersForGroup.add(user);
                            }
                        } else {
                            selectedMembersForGroup.remove(user);
                        }
                    });

                    setGraphic(checkBox);
                    setText(null);
                }
            }
        });
    }

    @FXML
    void onCreateClick(ActionEvent event) {
        String groupName = txtGroupName.getText().trim();
        List<User> selectedUsers = new ArrayList<>(selectedMembersForGroup);

        if (groupName.isEmpty()) {
            lblError.setText("Vui lòng nhập tên nhóm!");
            return;
        }

        if (selectedUsers.isEmpty()) {
            lblError.setText("Vui lòng chọn ít nhất 1 thành viên!");
            return;
        }

        selectedUsers.add(me);

        ChatGroup newGroup = new ChatGroup(0, groupName, me, selectedUsers);

        client.sendData(new DataPacket(TypeDataPacket.CREATE_GROUP_REQUEST, newGroup));

        lblError.setStyle("-fx-text-fill: green;");
        lblError.setText("Đang tạo nhóm...");
        btnCreate.setDisable(true);
    }

    @FXML
    void onCancelClick(ActionEvent event) {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }
}