package org.proptit.localchat;

import org.proptit.localchat.server.networks.SocketServer;

public class RunServer {
    public static void main(String[] args) {
        SocketServer server = new SocketServer();
        Thread serverThread = new Thread(server);
        serverThread.start();
    }
}
