package org.proptit.localchat.common.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class Message implements Serializable {
    protected User sender;
    protected String timestamp;
    protected String receiverNickname;
    protected boolean isBroadcast;

    public Message(User sender) {
        this.sender = sender;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    public boolean isBroadcast() {
        return isBroadcast;
    }

    public String getReceiverNickname() {
        return receiverNickname;
    }
}