package org.proptit.localchat.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import org.proptit.localchat.client.networks.SocketClient;
import org.proptit.localchat.common.enums.TypeDataPacket;
import org.proptit.localchat.common.models.DataPacket;
import org.proptit.localchat.common.models.User;

public class ChangePasswordController {

    @FXML private PasswordField txtCurrentPassword;
    @FXML private PasswordField txtNewPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Label lblError;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;

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
            showError("Vui lòng điền đầy đủ các trường!");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            showError("Mật khẩu mới không khớp!");
            return;
        }
        User updateData = new User(me.getId(), me.getUsername(), newPass, me.getNickname(), me.getRole());

        DataPacket packet = new DataPacket(TypeDataPacket.UPDATE_PROFILE_REQUEST, updateData);
        client.sendData(packet);

        lblError.setStyle("-fx-text-fill: green;");
        lblError.setText("Đang xử lý...");
        btnSave.setDisable(true);
    }

    @FXML
    void onCancelClick(ActionEvent event) {
        closeWindow();
    }

    private void showError(String message) {
        lblError.setStyle("-fx-text-fill: #e84118;");
        lblError.setText(message);
    }

    private void closeWindow() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }
}