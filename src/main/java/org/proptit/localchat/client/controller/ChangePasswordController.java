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
            showErrorMessage("Vui lòng điền đầy đủ các trường!");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            showErrorMessage("Mật khẩu mới không khớp!");
            return;
        }
        User updateData = new User(me.getId(), me.getUsername(), newPass, me.getNickname(), me.getRole());
        DataPacket packet = new DataPacket(TypeDataPacket.UPDATE_PROFILE_REQUEST, updateData);
        client.sendData(packet);
        lblError.setStyle("-fx-text-fill: #23A559;");
        lblError.setText("Đang xử lý...");
        btnSave.setDisable(true);
    }
    @FXML
    void onCancelClick(ActionEvent event) {
        closeWindow();
    }

    public void showSuccessMessage() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Thành công");
            alert.setHeaderText(null);
            alert.setContentText("Đổi mật khẩu thành công!");

            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setStyle("-fx-background-color: #161B28; -fx-border-color: #2A3042; -fx-border-width: 1;");
            dialogPane.lookupAll(".label").forEach(node -> node.setStyle("-fx-text-fill: white; -fx-font-size: 14px;"));

            Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
            if (okButton != null) {
                okButton.setStyle("-fx-background-color: #AD7BFF; -fx-text-fill: black; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 15 6 15;");
            }

            alert.showAndWait();
            closeWindow();
        });
    }

    public void showErrorMessage(String errorContent) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setHeaderText(null);
            alert.setContentText(errorContent);

            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setStyle("-fx-background-color: #161B28; -fx-border-color: #2A3042; -fx-border-width: 1;");
            dialogPane.lookupAll(".label").forEach(node -> node.setStyle("-fx-text-fill: white; -fx-font-size: 14px;"));

            Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
            if (okButton != null) {
                okButton.setStyle("-fx-background-color: #FF5C5C; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 15 6 15;");
            }
            alert.showAndWait();
            btnSave.setDisable(false);
            lblError.setText("");
        });
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