package org.proptit.localchat.server.services;

import org.proptit.localchat.common.enums.TypeDataPacket;
import org.proptit.localchat.common.models.DataPacket;
import org.proptit.localchat.common.models.message.Message;
import org.proptit.localchat.common.models.User;
import org.proptit.localchat.server.controller.ClientHandler;
import org.proptit.localchat.server.dao.GroupDao;
import org.proptit.localchat.server.networks.SocketServer;
import org.proptit.localchat.server.utils.ServerLogger;

import java.util.List;

public class ChatService {
    private List<ClientHandler> clients;

    public ChatService(List<ClientHandler> clients) {
        this.clients = clients;
    }

    public void processMessage(ClientHandler senderHandler, Message msg) {
        User sender = senderHandler.getUser();
        if (msg.getGroupId() != null && msg.getGroupId() > 0) {
            ServerLogger.info("Chat", "Processing Group message from [" + sender.getUsername() + "] to Group ID: " + msg.getGroupId());
            List<Integer> memberIds = new GroupDao().getMemberIdsByGroupId(msg.getGroupId());
            for (ClientHandler client : this.clients) {
                if (client.getUser() != null && memberIds.contains(client.getUser().getId())) {
                    if (!client.getUser().getId().equals(msg.getSender().getId())) {
                        client.sendData(new DataPacket(TypeDataPacket.CHAT_MESSAGE, msg));
                    }
                }
            }
            return;
        } else if (msg.isBroadcast()) {
            if (sender.isManager()) {
                sendAll(senderHandler, msg);
            } else {
                ServerLogger.warn("Chat", "Unauthorized Broadcast attempt by user: [" + sender.getUsername() + "]");
            }
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
        ServerLogger.info("Chat", "Sending " + " message from [" + senderHandler.getUser().getUsername() + "] to all users.");
        for (ClientHandler client : clients) {
            if (client != senderHandler) {
                client.sendData(packet);
            }
        }
    }

    private void sendPrivate(Message msg) {
        DataPacket packet = new DataPacket(TypeDataPacket.CHAT_MESSAGE, msg);
        for (ClientHandler client : clients) {
            if (client.getUser().getId().equals(msg.getReceiver().getId())) {
                ServerLogger.info("Chat", "Forwarding Private message: [" + msg.getSender().getUsername() + "] -> [" + msg.getReceiver().getUsername() + "]");
                client.sendData(packet);
                return;
            }
        }
    }
}
