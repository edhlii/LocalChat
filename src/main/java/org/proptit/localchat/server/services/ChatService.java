package org.proptit.localchat.server.services;

import org.proptit.localchat.common.models.Message;
import org.proptit.localchat.common.models.User;
import org.proptit.localchat.server.controller.ClientHandler;

import java.util.List;

public class ChatService {
    private List<ClientHandler> clients;

    public ChatService(List<ClientHandler> clients) {
        this.clients = clients;
    }

    public void processMessage(ClientHandler senderHandler, Message msg) {
        User sender = senderHandler.getUser();

        if (msg.isBroadcast()) {
            if (sender.isAdmin()) {
                sendAll("[GLOBAL] " + msg.toString());
            } else {
                senderHandler.sendMessage("System: You do not have permission to send the entire message!");
            }
            return;
        }

        if (msg.getReceiverNickname() != null) {
            sendPrivate(msg);
        } else {
            sendAll(msg.toString());
        }
    }

    public void sendAll(Object message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message.toString());
        }
    }

    private void sendPrivate(Message msg) {
        for (ClientHandler client : clients) {
            if (client.getUser().getNickname().equalsIgnoreCase(msg.getReceiverNickname())) {
                client.sendMessage(msg.toString());
                return;
            }
        }
    }
}
