package org.proptit.localchat.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
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
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();


        User loginUser = new User(username, password);

        socketClient.sendData(new DataPacket(TypeDataPacket.LOGIN_REQUEST, loginUser));
    }

    public void handleLoginResult(boolean success, Object data) {
        Platform.runLater(() -> {
            if (success) {
                try {
                    Stage stage = (Stage) txtUsername.getScene().getWindow();
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/proptit/localchat/main_window.fxml"));
                    Parent root = loader.load();
                    MainWindowController mainWindowController = loader.getController();
                    mainWindowController.setup(socketClient, (User) data);
                    mainWindowController.setClient(socketClient);
                    mainWindowController.setMe((User)data);
                    socketClient.setController(mainWindowController);



                    stage.setScene(new Scene(root));
                    stage.setTitle("LocalChat - " + ((User)data).getNickname());

                } catch (IOException e) { e.printStackTrace(); }
            } else {
                lblMessage.setVisible(true);
                lblMessage.setManaged(true);
                lblMessage.setText("Login failed!");
            }
        });
    }
}