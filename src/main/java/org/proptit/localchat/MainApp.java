package org.proptit.localchat;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.proptit.localchat.client.controller.LoginController;
import org.proptit.localchat.client.networks.SocketClient;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("login_view.fxml"));
        Scene scene = new Scene(loader.load());

        LoginController loginController = loader.getController();


        SocketClient client = new SocketClient("127.0.0.1", 1204, null);
        client.setLoginController(loginController);
        loginController.setSocketClient(client);

        Thread clientThread = new Thread(client);

        clientThread.setDaemon(true);
        clientThread.start();

        stage.setTitle("LocalChat.");
        stage.setScene(scene);
        stage.show();
    }
}