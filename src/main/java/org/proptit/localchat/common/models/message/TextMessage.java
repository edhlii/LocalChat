package org.proptit.localchat.common.models.message;

import org.proptit.localchat.common.models.User;

import java.io.Serializable;

public class TextMessage extends Message implements Serializable {
    private String content;

    protected TextMessage(User sender, String content) {
        super(sender);
        this.content = content;
    }

    public static TextMessage createBroadcast(User admin, String content) {
        TextMessage msg = new TextMessage(admin, content);
        msg.isBroadcast = true;
        return msg;
    }

    public static TextMessage createPrivate(User sender, String receiver, String content) {
        TextMessage msg = new TextMessage(sender, content);
        msg.receiverNickname = receiver;
        msg.isBroadcast = false;
        return msg;
    }

    public String toString() {
        return "[" + timestamp + "] " + sender.getNickname() + ": " + content;
    }
}