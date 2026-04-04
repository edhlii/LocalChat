package org.proptit.localchat.server.services;

import org.proptit.localchat.common.enums.TypeDataPacket;
import org.proptit.localchat.common.models.DataPacket;
import org.proptit.localchat.common.models.message.Message;
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
            if (sender.isManager()) {
                sendAll(senderHandler, msg);
            } else {
                System.out.println("System: You do not have permission to send the entire message!");

            }
            //sendAll(senderHandler, msg);
            return;
        }

        if (msg.getReceiverNickname() != null) {
            sendPrivate(msg);
        } else {
            sendAll(senderHandler, msg);
        }
    }

    public void sendAll(ClientHandler senderHandler, Message msg) {
        DataPacket packet = new DataPacket(TypeDataPacket.CHAT_MESSAGE, msg);
        System.out.println("SERVER PHÁT: Đang gửi tin nhắn này cho " + clients.size() + " người!");
        for (ClientHandler client : clients) {
            if (client != senderHandler) {
                client.sendMessage(packet);
            }
        }
    }

    private void sendPrivate(Message msg) {
        DataPacket packet = new DataPacket(TypeDataPacket.CHAT_MESSAGE, msg);

        for (ClientHandler client : clients) {
            if (client.getUser().getNickname().equalsIgnoreCase(msg.getReceiverNickname())) {
                client.sendMessage(packet);
                return;
            }
        }
    }
}
