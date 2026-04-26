package org.proptit.localchat.client.controller;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.proptit.localchat.client.networks.SocketClient;
import org.proptit.localchat.common.enums.TypeDataPacket;
import org.proptit.localchat.common.models.DataPacket;
import org.proptit.localchat.common.models.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MemberManagementController {
    private SocketClient client;
    private User me;
    private List<User> allMembers = new ArrayList<>();

    @FXML
    private VBox vboxMemberList;
    @FXML private TextField txtSearchPeople;

    public void init(SocketClient client, User me) {
        this.client = client;
        this.me = me;
        if (txtSearchPeople != null) {
            txtSearchPeople.textProperty().addListener((observable, oldValue, newValue) -> {
                renderMemberListByKeyword(newValue);
            });
        }
    }

    public void updateMemberList(List<User> users) {
        Platform.runLater(() -> {
            allMembers = users == null ? new ArrayList<>() : new ArrayList<>(users);
            renderMemberListByKeyword(txtSearchPeople != null ? txtSearchPeople.getText() : "");
        });
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

    public void addMemberToUI(User user) {
        HBox memberItem = new HBox(20);
        memberItem.setAlignment(Pos.CENTER_LEFT);
        memberItem.setPadding(new Insets(12, 20, 12, 20));

        if (user.getUsername().equals(me.getUsername())) {
            memberItem.setStyle("-fx-background-color: #1E2435; -fx-background-radius: 10; -fx-border-color: #AD7BFF; -fx-border-radius: 10;");
        } else {
            memberItem.setStyle("-fx-background-color: #161B28; -fx-background-radius: 10; -fx-border-color: #2A3042; -fx-border-radius: 10;");
        }

        String initial = user.getNickname() != null && !user.getNickname().isEmpty()
                ? user.getNickname().substring(0, 1).toUpperCase() : "U";
        Label lblCircle = new Label(initial);
        lblCircle.setMinWidth(45);
        lblCircle.setMaxWidth(45);
        lblCircle.setMinHeight(45);
        lblCircle.setMaxHeight(45);
        lblCircle.setAlignment(Pos.CENTER);
        lblCircle.setStyle("-fx-background-color: #2A3042; -fx-text-fill: white; -fx-background-radius: 30; -fx-font-weight: bold; -fx-font-size: 20px;");

        VBox infoMemBox = new VBox(2);

        Label lblNickname = new Label(user.getNickname());
        lblNickname.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #FFFFFF;");

        Label lblUsername = new Label("Username: " + user.getUsername());
        lblUsername.setStyle("-fx-text-fill: #8B92A5; -fx-font-size: 13px;");

        infoMemBox.getChildren().addAll(lblNickname, lblUsername);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        memberItem.getChildren().addAll(lblCircle, infoMemBox);

        if (user.getRole().equalsIgnoreCase("MEMBER")) {
            Button btnDelete = new Button("X");
            btnDelete.setStyle("-fx-background-color: transparent; -fx-text-fill: #FF5C5C; -fx-border-color: #FF5C5C; -fx-border-radius: 6; -fx-font-weight: bold; -fx-cursor: hand;");

            btnDelete.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Delete Account");
                confirm.setHeaderText("Do you want to delete this account?");

                confirm.getDialogPane().setStyle("-fx-background-color: #161B28; -fx-base: #161B28;");
                confirm.getDialogPane().lookupAll(".label").forEach(node -> ((Label)node).setStyle("-fx-text-fill: white;"));

                ButtonType btnYes = new ButtonType("YES", ButtonBar.ButtonData.YES);
                ButtonType btnNo = new ButtonType("NO", ButtonBar.ButtonData.NO);
                confirm.getButtonTypes().setAll(btnYes, btnNo);

                if (confirm.showAndWait().orElse(btnNo) == btnYes) {
                    client.sendData(new DataPacket(TypeDataPacket.DELETE_USER_REQUEST, user.getId()));
                    vboxMemberList.getChildren().remove(memberItem);
                }
            });

            memberItem.getChildren().addAll(spacer, btnDelete);
        } else {
            Label lblBadge = new Label("MANAGER");
            lblBadge.setStyle("-fx-text-fill: #23A559; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 2;");
            memberItem.getChildren().addAll(spacer, lblBadge);
        }

        vboxMemberList.getChildren().add(memberItem);
    }
    @FXML
    void onAddMemberClick(ActionEvent event) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Add New Account");
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #161B28; -fx-border-color: #2A3042; -fx-border-width: 1;");
        try {
            String cssPath = getClass().getResource("/org/proptit/localchat/modal.css").toExternalForm();
            dialogPane.getStylesheets().add(cssPath);
        } catch (Exception e) {
            System.out.println("Không tìm thấy file CSS, dùng style trực tiếp.");
        }
        dialogPane.lookupAll(".label").forEach(node -> node.setStyle("-fx-text-fill: white;"));
        ButtonType btnAddType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(btnAddType, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(20);
        grid.setPadding(new Insets(20, 30, 10, 30));
        String inputStyle = "-fx-background-color: #0B0F19; -fx-text-fill: white; -fx-border-color: #2A3042; -fx-border-radius: 6; -fx-padding: 8;";
        TextField txtUsername = new TextField();
        txtUsername.setPromptText("Enter username...");
        txtUsername.setStyle(inputStyle);
        PasswordField txtPassword = new PasswordField();
        txtPassword.setPromptText("Enter password...");
        txtPassword.setStyle(inputStyle);
        TextField txtNickname = new TextField();
        txtNickname.setPromptText("Enter nickname...");
        txtNickname.setStyle(inputStyle);
        ComboBox<String> cbRole = new ComboBox<>();
        cbRole.getItems().addAll("MEMBER", "MANAGER");
        cbRole.setValue("MEMBER");
        cbRole.setMaxWidth(Double.MAX_VALUE);
        cbRole.setStyle(inputStyle);
        cbRole.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("-fx-background-color: #0B0F19;");
                } else {
                    setText(item);
                    setStyle("-fx-background-color: #0B0F19; -fx-text-fill: white; -fx-padding: 8;");
                    // Hiệu ứng khi di chuột qua item trong danh sách sổ xuống
                    setOnMouseEntered(e -> setStyle("-fx-background-color: #1E2435; -fx-text-fill: #AD7BFF; -fx-padding: 8;"));
                    setOnMouseExited(e -> setStyle("-fx-background-color: #0B0F19; -fx-text-fill: white; -fx-padding: 8;"));
                }
            }
        });
        cbRole.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("-fx-text-fill: white;");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: white;");
                }
            }
        });
        Label l1 = new Label("Username:"); l1.setStyle("-fx-text-fill: #8B92A5; -fx-font-weight: bold;");
        Label l2 = new Label("Password:"); l2.setStyle("-fx-text-fill: #8B92A5; -fx-font-weight: bold;");
        Label l3 = new Label("Nickname:"); l3.setStyle("-fx-text-fill: #8B92A5; -fx-font-weight: bold;");
        Label l4 = new Label("Role:"); l4.setStyle("-fx-text-fill: #8B92A5; -fx-font-weight: bold;");
        grid.add(l1, 0, 0); grid.add(txtUsername, 1, 0);
        grid.add(l2, 0, 1); grid.add(txtPassword, 1, 1);
        grid.add(l3, 0, 2); grid.add(txtNickname, 1, 2);
        grid.add(l4, 0, 3); grid.add(cbRole, 1, 3);
        dialogPane.setContent(grid);
        Node btnCreate = dialogPane.lookupButton(btnAddType);
        btnCreate.setDisable(true);
        btnCreate.setStyle("-fx-background-color: #AD7BFF; -fx-text-fill: black; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");
        Node btnCancel = dialogPane.lookupButton(ButtonType.CANCEL);
        btnCancel.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: #2A3042; -fx-border-radius: 6; -fx-cursor: hand;");
        String usernameRegex = "^[a-zA-Z0-9._]+";
        ChangeListener<String> validationListener = (observable, oldValue, newValue) -> {
            String username = txtUsername.getText().trim();
            String password = txtPassword.getText().trim();
            String nickname = txtNickname.getText().trim();
            boolean isValid = !username.isEmpty() && username.matches(usernameRegex) && !password.isEmpty() && !nickname.isEmpty();
            btnCreate.setDisable(!isValid);
        };
        txtUsername.textProperty().addListener(validationListener);
        txtPassword.textProperty().addListener(validationListener);
        txtNickname.textProperty().addListener(validationListener);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnAddType) {
                return new User(0, txtUsername.getText().trim(), txtPassword.getText().trim(), txtNickname.getText().trim(), cbRole.getValue(), null);
            }
            return null;
        });
        dialog.showAndWait().ifPresent(newUser -> {
            client.sendData(new DataPacket(TypeDataPacket.ADD_USER_REQUEST, newUser));
        });
    }
}