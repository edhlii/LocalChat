package org.proptit.localchat.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.proptit.localchat.client.networks.SocketClient;
import org.proptit.localchat.common.enums.TypeDataPacket;
import org.proptit.localchat.common.models.DataPacket;
import org.proptit.localchat.common.models.User;
import org.proptit.localchat.common.models.call.CallSignal;
import org.proptit.localchat.common.models.message.Message;

import java.io.IOException;
import java.util.List;

public class MainWindowController {
    @FXML
    private HBox chatArea;
    @FXML
    private AnchorPane memberManagerArea;

    @FXML
    private Button btnNavMembers;

    @FXML
    private ChatController chatAreaController;
    @FXML
    private MemberManagementController memberManagerAreaController;

    private UserSettingsController userSettingsController;


    private SocketClient client;
    private User me;

    public void setMe(User me) {
        this.me = me;
    }

    public void setClient(SocketClient client) {
        this.client = client;
    }

    public void setup(SocketClient client, User user) {
        this.client = client;
        this.me = user;

        chatAreaController.init(client, user);
        memberManagerAreaController.init(client, user);

        boolean isManager = me.isManager();
        btnNavMembers.setVisible(isManager);
        btnNavMembers.setManaged(isManager);

        showChatArea();
        client.sendData(new DataPacket(TypeDataPacket.GET_ONLINE_USERS, null));
    }

    public void updateMemberList(List<User> users) {
        memberManagerAreaController.updateMemberList(users);
    }

    public void updateOnlinePeople(List<User> users) {
        chatAreaController.updateOnlinePeople(users);
    }

    public void receiveMessage(Message msg) {
        chatAreaController.receiveMessage(msg);
    }
    public void updateChatContacts(List<User> contactList) {
        chatAreaController.setAllMembers(contactList);
    }

    public void addMemberToUI(User user) {
        memberManagerAreaController.addMemberToUI(user);
    }

    public void receiveCallSignal(CallSignal signal) {
        chatAreaController.receiveCallSignal(signal);
    }

    public ChatController getChatAreaController() {
        return chatAreaController;
    }

    @FXML
    void onNavMembersClick(ActionEvent event) {
        showMemberManagerArea();
        client.sendData(new DataPacket(TypeDataPacket.GET_ALL_USERS, null));
        client.sendData(new DataPacket(TypeDataPacket.GET_ONLINE_USERS, null));
    }

    @FXML
    void onNavChatClick(ActionEvent event) {
        showChatArea();
        client.sendData(new DataPacket(TypeDataPacket.GET_ONLINE_USERS, null));
    }

    private void showChatArea() {
        chatArea.setVisible(true);
        chatArea.setManaged(true);
        memberManagerArea.setVisible(false);
        memberManagerArea.setManaged(false);
    }

    private void showMemberManagerArea() {
        memberManagerArea.setVisible(true);
        memberManagerArea.setManaged(true);
        chatArea.setVisible(false);
        chatArea.setManaged(false);
    }

    @FXML
    void onChangeInfoAction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/proptit/localchat/user_setting.fxml"));
            VBox root = loader.load();


            this.userSettingsController = loader.getController();


            this.userSettingsController.init(this.client, this.me);

            Stage stage = new Stage();
            stage.setTitle("User Profile - LocalChat");
            stage.setScene(new Scene(root));


            stage.setOnCloseRequest(e -> {
                this.userSettingsController = null;
            });

            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public UserSettingsController getUserSettingsController() {
        return userSettingsController;
    }
}
