package org.proptit.localchat.server.networks;

import org.proptit.localchat.server.controller.ClientHandler;
import org.proptit.localchat.server.services.ChatService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

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
            client.sendMessage(message);
        }
    }

    public void removeClient(ClientHandler handler) {
        clients.remove(handler);
    }

    private void stopServer() {
        try {
            isRunning = false;
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
