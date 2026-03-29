package org.proptit.localchat.server.services;

import org.proptit.localchat.server.controller.ClientHandler;
import org.proptit.localchat.server.networks.SocketServer;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ChatService {
    private List<ClientHandler> clients;

    public ChatService(List<ClientHandler> clients) {
        this.clients = clients;
    }

    public void sendToAll(Object message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message.toString());
        }
    }

    public void sendToPerson(Object message) {

    }
}
