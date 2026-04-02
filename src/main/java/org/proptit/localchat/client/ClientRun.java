package org.proptit.localchat.client;

import javafx.application.Application;
import org.proptit.localchat.client.views.ChatWindow;


public class ClientRun {
    public static void main(String[] args) {
        Application.launch(ChatWindow.class, args);
    }
}