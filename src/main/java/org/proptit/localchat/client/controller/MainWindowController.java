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
    private SocketClient client;
    private User me;
    private List<User> allMembers = new ArrayList<>();
    private List<User> onlineUsers = new ArrayList<>();
    private final Map<String, User> conversationUserMap = new HashMap<>();
    private User selectedConversationUser;

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
    @FXML private TextField txtSearchPeople;
    @FXML private ListView<String> lvOnlinePeople;
    @FXML private ListView<String> lvChatList;

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

        if (txtSearchPeople != null) {
            txtSearchPeople.textProperty().addListener((observable, oldValue, newValue) -> {
                renderMemberListByKeyword(newValue);
            });
        }

        if (lvChatList != null) {
            lvChatList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                selectedConversationUser = conversationUserMap.get(newValue);
            });
        }
    }

    public void setupUI(SocketClient client, User user) {
        this.client = client;
        this.me = user;
        boolean isManager = me.isManager();

        btnNavMembers.setVisible(isManager);
        btnNavMembers.setManaged(isManager);

        sendMessageAllButton.setVisible(isManager);
        sendMessageAllButton.setManaged(isManager);

        btnAddMember.setVisible(isManager);
        btnAddMember.setManaged(isManager);

        client.sendData(new DataPacket(TypeDataPacket.GET_ONLINE_USERS, null));
    }

    @FXML
    void onNavMembersClick(ActionEvent event) {
        showMemberManagerArea();
        client.sendData(new DataPacket(TypeDataPacket.GET_ALL_USERS, null));
        client.sendData(new DataPacket(TypeDataPacket.GET_ONLINE_USERS, null));
    }

    public void updateMemberList(List<User> users) {
        Platform.runLater(() -> {
            allMembers = users == null ? new ArrayList<>() : new ArrayList<>(users);
            renderMemberListByKeyword(txtSearchPeople != null ? txtSearchPeople.getText() : "");
        });
    }

    public void updateOnlinePeople(List<User> users) {
        Platform.runLater(() -> {
            if (lvOnlinePeople == null || lvChatList == null) {
                return;
            }

            onlineUsers = users == null ? new ArrayList<>() : new ArrayList<>(users);

            lvOnlinePeople.getItems().clear();
            lvChatList.getItems().clear();
            conversationUserMap.clear();

            if (onlineUsers.isEmpty()) {
                lvOnlinePeople.getItems().add("No one online");
                lvChatList.getItems().add("No conversations");
                selectedConversationUser = null;
                return;
            }

            List<String> onlineNames = onlineUsers.stream()
                    .map(user -> user.getNickname() + " (@" + user.getUsername() + ")")
                    .collect(Collectors.toList());
            lvOnlinePeople.getItems().addAll(onlineNames);

            List<User> availableConversations = onlineUsers.stream()
                    .filter(user -> me == null || !user.getUsername().equalsIgnoreCase(me.getUsername()))
                    .collect(Collectors.toList());

            if (availableConversations.isEmpty()) {
                lvChatList.getItems().add("No conversations");
                selectedConversationUser = null;
                return;
            }

            for (User user : availableConversations) {
                String label = user.getNickname() + " (@" + user.getUsername() + ")";
                conversationUserMap.put(label, user);
                lvChatList.getItems().add(label);
            }

            if (selectedConversationUser != null) {
                String selectedUsername = selectedConversationUser.getUsername();
                boolean stillOnline = availableConversations.stream()
                        .anyMatch(user -> user.getUsername().equalsIgnoreCase(selectedUsername));
                if (!stillOnline) {
                    selectedConversationUser = null;
                    lvChatList.getSelectionModel().clearSelection();
                }
            }
        });
    }

    public User getSelectedConversationUser() {
        return selectedConversationUser;
    }

    @FXML
    void onNavChatClick(ActionEvent event) {
        showChatArea();
        client.sendData(new DataPacket(TypeDataPacket.GET_ONLINE_USERS, null));
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
            boolean isSendAll = event.getSource() == sendMessageAllButton;
            Message msg;

            if (!isSendAll && selectedConversationUser != null) {
                msg = TextMessage.createPrivate(me, selectedConversationUser.getNickname(), messageText);
                addMessageToScreen(messageText, "Me -> " + selectedConversationUser.getNickname(), true);
            } else {
                msg = TextMessage.createBroadcast(me, messageText);
                addMessageToScreen(messageText, "Me", true);
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
            addImageToScreen(img, senderName, false);
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
            ImageMessage imgMsg;
            if (selectedConversationUser != null) {
                imgMsg = ImageMessage.createPrivate(me, selectedConversationUser.getNickname(), imageBytes, "image");
            } else {
                imgMsg = ImageMessage.createBroadcast(me, imageBytes, "image");
            }
            addImageToScreen(FileUtils.bytesToImage(imageBytes), "Me", true);
            client.sendData(new DataPacket(TypeDataPacket.CHAT_MESSAGE, imgMsg));
        }
    }

    public void addMemberToUI(User user) {
        HBox memberItem = new HBox(20);
        memberItem.setAlignment(Pos.CENTER_LEFT);
        memberItem.setPadding(new Insets(10, 15, 10, 15));
        memberItem.setStyle("-fx-background-color: white");

        if (user.getUsername().equals(me.getUsername())) {
            memberItem.setStyle("-fx-background-color: #e3f2fd; -fx-background-radius: 10; ");
        } else {
            memberItem.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        }

        Label lblCircle = new Label("\uD83D\uDC64");
        lblCircle.setMinWidth(45);
        lblCircle.setMaxWidth(45);
        lblCircle.setMinHeight(45);
        lblCircle.setMaxHeight(45);
        lblCircle.setAlignment(Pos.CENTER);
        lblCircle.setStyle("-fx-background-color: #0d1a5f; -fx-text-fill: white; " +
                "-fx-background-radius: 30; -fx-font-weight: bold; -fx-font-size: 20px;");

        VBox infoMemBox = new VBox(2);
        Label lblNickname = new Label(user.getNickname());
        lblNickname.setStyle("-fx-font-weight: bold; -fx-font-size: 20px;");

        Label lblUsername = new Label("Username: " + user.getUsername());
        lblUsername.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");

        infoMemBox.getChildren().addAll(lblNickname, lblUsername);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        memberItem.getChildren().addAll(lblCircle, infoMemBox);

        if (user.getRole().equalsIgnoreCase("MEMBER")) {
            Button btnDelete = new Button("X");
            btnDelete.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

            btnDelete.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);

                confirm.setTitle("Do you want to delete this account?");
                confirm.setHeaderText(null);
                confirm.setGraphic(null);
                ButtonType btnYes = new ButtonType("YES", ButtonBar.ButtonData.YES);
                ButtonType btnNo = new ButtonType("NO", ButtonBar.ButtonData.NO);
                confirm.getButtonTypes().setAll(btnYes, btnNo);

                if (confirm.showAndWait().get() == btnYes) {
                    client.sendData(new DataPacket(TypeDataPacket.DELETE_USER_REQUEST, user.getId()));
                    vboxMemberList.getChildren().remove(memberItem);
                }
            });

            memberItem.getChildren().addAll(spacer, btnDelete);
        } else {
            Label lblBadge = new Label(user.getRole());
            lblBadge.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 2;");
            memberItem.getChildren().addAll(spacer, lblBadge);
        }

        vboxMemberList.getChildren().add(memberItem);
    }

    private void renderMemberListByKeyword(String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        vboxMemberList.getChildren().clear();

        for (User user : allMembers) {
            String username = user.getUsername() == null ? "" : user.getUsername().toLowerCase(Locale.ROOT);
            String nickname = user.getNickname() == null ? "" : user.getNickname().toLowerCase(Locale.ROOT);

            if (normalizedKeyword.isEmpty() || username.contains(normalizedKeyword) || nickname.contains(normalizedKeyword)) {
                addMemberToUI(user);
            }
        }
    }

    @FXML
    void onAddMemberClick(ActionEvent event) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Add New Account");
        dialog.setHeaderText(null);

        ButtonType btnAddType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnAddType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(20);
        grid.setPadding(new Insets(20, 30, 10, 30));

        TextField txtUser = new TextField();
        txtUser.setPromptText("Enter username...");

        TextField txtPass = new TextField();
        txtPass.setPromptText("Enter password...");

        TextField txtNick = new TextField();
        txtNick.setPromptText("Enter nickname...");

        ComboBox<String> cbRole = new ComboBox<>();
        cbRole.getItems().addAll("MEMBER", "MANAGER");
        cbRole.setValue("MEMBER");
        cbRole.setMaxWidth(Double.MAX_VALUE);

        grid.add(new Label("Username:"), 0, 0);
        grid.add(txtUser, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(txtPass, 1, 1);
        grid.add(new Label("Nickname:"), 0, 2);
        grid.add(txtNick, 1, 2);
        grid.add(new Label("Role:"), 0, 3);
        grid.add(cbRole, 1, 3);

        dialog.getDialogPane().setContent(grid);

        Node btnCreate = dialog.getDialogPane().lookupButton(btnAddType);
        btnCreate.setDisable(true);

        javafx.beans.value.ChangeListener<String> validationListener = (observable, oldValue, newValue) -> {
            boolean isInvalid = txtUser.getText().trim().isEmpty()
                    || txtPass.getText().isEmpty()
                    || txtNick.getText().isEmpty();
            btnCreate.setDisable(isInvalid);
        };

        txtUser.textProperty().addListener(validationListener);
        txtPass.textProperty().addListener(validationListener);
        txtNick.textProperty().addListener(validationListener);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnAddType) {
                return new User(0, txtUser.getText().trim(), txtPass.getText(),
                        txtNick.getText().trim(), cbRole.getValue());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(newUser -> {
            client.sendData(new DataPacket(TypeDataPacket.ADD_USER_REQUEST, newUser));
        });
    }
}
