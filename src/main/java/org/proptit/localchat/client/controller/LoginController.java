package org.proptit.localchat.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.proptit.localchat.client.networks.SocketClient;
import org.proptit.localchat.common.enums.TypeDataPacket;
import org.proptit.localchat.common.models.DataPacket;
import org.proptit.localchat.common.models.User;

import java.io.IOException;

public class LoginController {
    @FXML
    private TextField txtUsername;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private Label lblMessage;

    private SocketClient socketClient;

    public void setSocketClient(SocketClient client) { this.socketClient = client; }

    @FXML
    void onLoginButtonClick() {
        String username = txtUsername.getText();
        String password = txtPassword.getText();


        User loginUser = new User(username, password);

        socketClient.sendData(new DataPacket(TypeDataPacket.LOGIN_REQUEST, loginUser));
    }

    public void handleLoginResult(boolean success, Object data) {
        Platform.runLater(() -> {
            if (success) {
                try {
                    Stage stage = (Stage) txtUsername.getScene().getWindow();
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/proptit/localchat/chat_window.fxml"));
                    Scene scene = new Scene(loader.load());
                    stage.setScene(scene);
                    stage.setTitle("LocalChat - " + ((User)data).getNickname());
                } catch (IOException e) { e.printStackTrace(); }
            } else {
                lblMessage.setText("Login failed");
            }
        });
    }
}