package org.proptit.localchat.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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

public class MainWindowController {
    private SocketClient client;
    private User me;

    public void setClient(SocketClient client) {
        this.client = client;
    }

    public void setMe(User me) {
        this.me = me;
    }


    @FXML private SplitPane chatAreaContainer;
    @FXML private AnchorPane memberManagerArea;
    @FXML private VBox vboxMessage;
    @FXML private VBox vboxMemberList;
    @FXML private ScrollPane scrollPane;
    @FXML private TextArea messageInput;

    @FXML private Button sendMessageAllButton;
    @FXML private Button btnAddMember;

    @FXML private Button sendMessageButton;

    @FXML private Button btnNavMembers;

    @FXML
    public void initialize() {

        showChatArea();
        vboxMessage.heightProperty().addListener((observable, oldValue, newValue) -> {
            scrollPane.setVvalue((Double) newValue);
        });
    }

    public void setupUI(SocketClient client,User user) {
        this.client = client;
        this.me = user;
        boolean isManager = me.isManager();

        btnNavMembers.setVisible(isManager);
        btnNavMembers.setManaged(isManager);

        sendMessageAllButton.setVisible(isManager);
        sendMessageAllButton.setManaged(isManager);

        btnAddMember.setVisible(isManager);
        btnAddMember.setManaged(isManager);
    }

    @FXML
    void onNavChatClick(ActionEvent event) {
        showChatArea();
    }

    @FXML
    void onNavMembersClick(ActionEvent event) {
        showMemberManagerArea();
    }

    private void showChatArea() {
        chatAreaContainer.setVisible(true);
        chatAreaContainer.setManaged(true);
        memberManagerArea.setVisible(false);
        memberManagerArea.setManaged(false);
    }

    private void showMemberManagerArea() {
        memberManagerArea.setVisible(true);
        memberManagerArea.setManaged(true);
        chatAreaContainer.setVisible(false);
        chatAreaContainer.setManaged(false);
    }


    @FXML
    void onSendButtonClick(ActionEvent event) {
        String messageText = messageInput.getText();
        if (!messageText.trim().isEmpty()) {
            addMessageToScreen(messageText, "Me", true);
            Message msg = TextMessage.createBroadcast(me, messageText);
            DataPacket packet = new DataPacket(TypeDataPacket.CHAT_MESSAGE, msg);
            if (client != null) {
                System.out.println("CLIENT GỬI: Đã đóng gói và bắt đầu gửi đi...");
                client.sendData(packet);
            }
            else {
                System.out.println("Don't connect to internet");
            }
            messageInput.clear();
        }
    }

    private void addMessageToScreen(String text, String senderName, boolean isMe) {
        Label lblMessage = new Label(text);
        lblMessage.setWrapText(true);
        lblMessage.setMaxWidth(400);

        if (isMe) {
            lblMessage.setStyle("-fx-background-color: #0084FF; -fx-text-fill: white; -fx-background-radius: 15px; -fx-padding: 8px 12px;");
        } else {
            lblMessage.setStyle("-fx-background-color: #E4E6EB; -fx-text-fill: black; -fx-background-radius: 15px; -fx-padding: 8px 12px;");
        }

        VBox messageGroup = new VBox(3);

        if (!isMe) {
            Label lblSender = new Label(senderName);
            lblSender.setStyle("-fx-font-size: 11px; -fx-text-fill: #65676B; -fx-padding: 0 0 0 5px;");
            messageGroup.getChildren().add(lblSender);
            messageGroup.setAlignment(Pos.TOP_LEFT);
        } else {
            messageGroup.setAlignment(Pos.TOP_RIGHT);
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

    private void addImageToScreen(Image img, String senderName, boolean isMe) {
        ImageView imageView = new ImageView(img);
        imageView.setFitWidth(250);
        imageView.setPreserveRatio(true);

        VBox messageGroup = new VBox(3);

        if (!isMe) {
            Label lblSender = new Label(senderName);
            lblSender.setStyle("-fx-font-size: 11px; -fx-text-fill: #65676B; -fx-padding: 0 0 0 5px;");
            messageGroup.getChildren().add(lblSender);
            messageGroup.setAlignment(Pos.TOP_LEFT);
        } else {
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

    public void receiveMessage(Message msg) {
        String senderName = msg.getSender().getNickname();
        if (msg instanceof ImageMessage) {
            ImageMessage imgMsg = (ImageMessage) msg;
            Image img = new Image(new ByteArrayInputStream(imgMsg.getImageData()));
            addImageToScreen(img, senderName,false);
        } else {
            String content = msg.toString();

            addMessageToScreen(content, senderName, false);
        }
    }

    @FXML
    void onFileButtonClick(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        byte[] imageBytes = FileUtils.chooseImageAndReadBytes(stage);

        if (imageBytes != null) {
            ImageMessage imgMsg = ImageMessage.createBroadcast(me, imageBytes, "image");

            addImageToScreen(FileUtils.bytesToImage(imageBytes), "Me",true);

            client.sendData(new DataPacket(TypeDataPacket.CHAT_MESSAGE, imgMsg));
        }
    }
}
