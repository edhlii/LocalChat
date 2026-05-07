package org.proptit.localchat.client.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import org.proptit.localchat.client.networks.SocketClient;
import org.proptit.localchat.common.enums.TypeDataPacket;
import org.proptit.localchat.common.models.DataPacket;
import org.proptit.localchat.common.models.User;
import org.proptit.localchat.common.utils.PasswordUtils;

public class ChangePasswordController {
    @FXML
    private PasswordField txtCurrentPassword;
    @FXML
    private PasswordField txtNewPassword;
    @FXML
    private PasswordField txtConfirmPassword;
    @FXML
    private Label lblError;
    @FXML
    private Button btnSave;
    @FXML
    private Button btnCancel;

    private SocketClient client;
    private User me;

    public void setup(SocketClient client, User me) {
        this.client = client;
        this.me = me;
    }

    @FXML
    void onSaveClick(ActionEvent event) {
        String currentPass = txtCurrentPassword.getText();
        String newPass = txtNewPassword.getText();
        String confirmPass = txtConfirmPassword.getText();

        if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            lblError.setStyle("-fx-text-fill: #23A559;");
            lblError.setText("Please fill in all fields...");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            lblError.setStyle("-fx-text-fill: #23A559;");
            lblError.setText("Passwords do not match...");
            return;
        }

        if (!PasswordUtils.checkPassword(currentPass, me.getPassword())) {
            lblError.setStyle("-fx-text-fill: #23A559;");
            lblError.setText("Incorrect current password...");
            return;
        }


        User updateData = new User(me.getId(), me.getUsername(), newPass, me.getNickname(), me.getRole(), me.getAvatar());
        DataPacket packet = new DataPacket(TypeDataPacket.UPDATE_PASS_REQUEST, updateData);
        client.sendData(packet);

        btnSave.setDisable(true);
    }

    @FXML
    void onCancelClick(ActionEvent event) {
        closeWindow();
    }

    public void closeWindow() {
        Platform.runLater(() -> {
            if (btnCancel != null && btnCancel.getScene() != null) {
                Stage stage = (Stage) btnCancel.getScene().getWindow();
                stage.close();
            }
        });
    }
}