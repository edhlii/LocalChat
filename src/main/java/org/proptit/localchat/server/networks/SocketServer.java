package org.proptit.localchat.server.networks;

import org.proptit.localchat.server.controller.ClientHandler;
import org.proptit.localchat.common.enums.TypeDataPacket;
import org.proptit.localchat.common.models.DataPacket;
import org.proptit.localchat.common.models.call.CallSignal;
import org.proptit.localchat.server.dao.GroupDao;
import org.proptit.localchat.server.services.ChatService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SocketServer implements Runnable {
    private static final int PORT = 1204;
    private static final String GROUP_TARGET_PREFIX = "__group__:";
    private final List<ClientHandler> clients = new ArrayList<>();
    private final GroupDao groupDao = new GroupDao();
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
            System.out.println("--- SERVER'S PORT " + PORT + " ---");
            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Log: New connection from " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket, this);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("Log: Server error: " + e.getMessage());
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
        DataPacket packet = new DataPacket(TypeDataPacket.RETURN_ONLINE_USERS, onlineUsers);
        broadcast(packet);
    }

    public void forwardCallSignal(CallSignal signal) {
        if (signal == null || signal.getToUsername() == null) {
            return;
        }

        if (signal.getToUsername().startsWith(GROUP_TARGET_PREFIX)) {
            Integer groupId = parseGroupId(signal.getToUsername());
            if (groupId == null) {
                return;
            }

            List<Integer> memberIds = groupDao.getMemberIdsByGroupId(groupId);
            if (memberIds == null || memberIds.isEmpty()) {
                return;
            }

            Set<Integer> memberSet = new HashSet<>(memberIds);
            DataPacket packet = new DataPacket(TypeDataPacket.CALL_SIGNAL, signal);
            for (ClientHandler client : clients) {
                if (client.getUser() == null) {
                    continue;
                }
                Integer userId = client.getUser().getId();
                if (userId == null || !memberSet.contains(userId)) {
                    continue;
                }
                if (signal.getFromUsername() != null
                        && signal.getFromUsername().equalsIgnoreCase(client.getUser().getUsername())) {
                    continue;
                }
                client.sendData(packet);
            }
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

    private Integer parseGroupId(String target) {
        if (target == null || !target.startsWith(GROUP_TARGET_PREFIX)) {
            return null;
        }
        try {
            return Integer.parseInt(target.substring(GROUP_TARGET_PREFIX.length()));
        } catch (Exception ex) {
            return null;
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
