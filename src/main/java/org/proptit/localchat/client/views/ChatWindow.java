package org.proptit.localchat.client.views;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.proptit.localchat.client.controller.ChatWindowController; // Nhớ import đúng package của bạn
import org.proptit.localchat.common.models.User;
import org.proptit.localchat.client.networks.SocketClient; // Nhớ import đúng package

import java.io.IOException;

public class ChatWindow extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/org/proptit/localchat/chat_window.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1280, 720);
        ChatWindowController controller = fxmlLoader.getController();
        User me = new User("quangdz");
        me.setRole("ADMIN");
        SocketClient client = new SocketClient("127.0.0.1", 1204, me);
        client.setController(controller);
        controller.setupNetwork(client, me);
        new Thread(client).start();
        stage.setTitle("LocalChat - " + me.getNickname());
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> {
            System.exit(0);
        });

        stage.show();
    }
    public static void main(String[] args) {
        launch();
    }
}