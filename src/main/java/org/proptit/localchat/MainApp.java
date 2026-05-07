package org.proptit.localchat;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.proptit.localchat.client.controller.LoginController;
import org.proptit.localchat.client.networks.SocketClient;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/proptit/localchat/login_view.fxml"));
        Scene scene = new Scene(loader.load());

        LoginController loginController = loader.getController();

        Properties prop = new Properties();
        String host = "127.0.0.1";
        int port = 1204;

        try (InputStream input = new FileInputStream("server.properties")) {
            prop.load(input);
            host = prop.getProperty("server_ip", "127.0.0.1");
            port = Integer.parseInt(prop.getProperty("server_port", "1204"));
            System.out.println("FINDED");
        } catch (IOException ex) {
            System.out.println("No configuration file found, using default IP address.");
        }

        SocketClient client = new SocketClient(host, port, null);

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