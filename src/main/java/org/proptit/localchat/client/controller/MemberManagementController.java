package org.proptit.localchat.client.controller;

import javafx.application.Platform;
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
