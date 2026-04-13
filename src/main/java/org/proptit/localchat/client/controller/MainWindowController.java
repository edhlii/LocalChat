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
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.proptit.localchat.client.networks.SocketClient;
import org.proptit.localchat.common.enums.TypeDataPacket;
import org.proptit.localchat.common.models.DataPacket;
import org.proptit.localchat.common.models.User;
import org.proptit.localchat.common.models.message.ImageMessage;
import org.proptit.localchat.common.models.message.Message;
import org.proptit.localchat.common.models.message.TextMessage;
import org.proptit.localchat.common.utils.FileUtils;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class MainWindowController {
    @FXML
    private SplitPane chatArea;
    @FXML
    private AnchorPane memberManagerArea;

    @FXML
    private Button btnNavMembers;

    @FXML
    private ChatController chatAreaController;
    @FXML
    private MemberManagementController memberManagerAreaController;


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

    public void addMemberToUI(User user) {
        memberManagerAreaController.addMemberToUI(user);
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

}
