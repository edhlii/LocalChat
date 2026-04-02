package org.proptit.localchat.client;

import org.proptit.localchat.client.networks.SocketClient;
import org.proptit.localchat.common.models.TextMessage;
import org.proptit.localchat.common.models.User;

import java.lang.reflect.Member;
import java.util.Scanner;

public class ClientRun {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        User me = new User(12, "vanq", "1204", "quangdz", "admin");

        SocketClient client = new SocketClient("127.0.0.1", 1204, me);
        new Thread(client).start();

        while (true) {
            String txt = sc.nextLine();
            if(txt.equals("exit")) break;
            client.sendData(TextMessage.createBroadcast(me, txt));
        }
    }
}