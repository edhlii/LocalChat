package org.proptit.localchat.server.networks;

import org.proptit.localchat.server.controller.ClientHandler;
import org.proptit.localchat.common.enums.TypeDataPacket;
import org.proptit.localchat.common.models.DataPacket;
import org.proptit.localchat.common.models.call.CallSignal;
import org.proptit.localchat.server.services.ChatService;
import org.proptit.localchat.server.utils.ServerLogger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SocketServer implements Runnable {
    private static final int PORT = 1204;
    private final List<ClientHandler> clients = new ArrayList<>();
    private ServerSocket serverSocket;
    private ChatService chatService;
    private boolean isRunning = true;

    public SocketServer() {
        this.chatService = new ChatService(this.clients);
    }

    public ChatService getChatService() {
        return chatService;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(PORT);
            ServerLogger.info("System", "Server is starting on PORT: " + PORT);
            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                ServerLogger.info("Network", "New socket connection from: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket, this);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            ServerLogger.error("System", "Server Runtime Error: " + e.getMessage());
        } finally {
            stopServer();
        }
    }

    public void broadcast(Object message) {
        for (ClientHandler client : clients) {
            client.sendData(message);
        }
    }

    public void removeClient(ClientHandler handler) {
        ServerLogger.warn("Network", "User [" + handler.getUser().getUsername() + "] disconnected");
        clients.remove(handler);
        broadcastOnlineUsers();
    }

    public List<org.proptit.localchat.common.models.User> getOnlineUsers() {
        return clients.stream()
                .map(ClientHandler::getUser)
                .filter(user -> user != null)
                .collect(Collectors.toList());
    }

    public void broadcastOnlineUsers() {
        List<org.proptit.localchat.common.models.User> onlineUsers = getOnlineUsers();
        ServerLogger.info("System", "Broadcasting online list. Current online count: " + onlineUsers.size());
        DataPacket packet = new DataPacket(TypeDataPacket.RETURN_ONLINE_USERS, onlineUsers);
        broadcast(packet);
    }

    public void forwardCallSignal(CallSignal signal) {
        if (signal == null || signal.getToUsername() == null) {
            return;
        }

        DataPacket packet = new DataPacket(TypeDataPacket.CALL_SIGNAL, signal);
        for (ClientHandler client : clients) {
            if (client.getUser() != null && signal.getToUsername().equalsIgnoreCase(client.getUser().getUsername())) {
                client.sendData(packet);
                return;
            }
        }
    }

    private void stopServer() {
        try {
            isRunning = false;
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<ClientHandler> getClients() {
        return clients;
    }
}
