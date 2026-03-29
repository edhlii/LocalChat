package org.proptit.localchat.server;

import org.proptit.localchat.server.networks.SocketServer;

public class ServerRun {
    public static void main(String[] args) {
        SocketServer server = new SocketServer();
        Thread serverThread = new Thread(server);
        serverThread.start();
    }
}
