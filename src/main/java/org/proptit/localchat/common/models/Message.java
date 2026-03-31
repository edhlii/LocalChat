package org.proptit.localchat.common.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.spi.LocaleServiceProvider;

public class Message implements Serializable {
    private static final long serialVersionUID = 2L;
    private final User sender;
    private String receiver;
    private final String content;
    private final String timestamp;
    private boolean isBroadcast;

    public Message(User sender, String content) {
        this.sender = sender;
        this.content = content;
        this.isBroadcast = false;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    public static Message createBroadcast(User admin, String content) {
        Message message = new Message(admin, content);
        message.isBroadcast = true;
        return message;
    }

    public static Message createPrivate(User sender, String receiver, String content) {
        Message message = new Message(sender, content);
        message.receiver = receiver;
        message.isBroadcast = false;
        return message;
    }

    @Override
    public String toString() {
        return "[" + timestamp + "] " + sender.getNickname() + ": " + content;
    }

    public User getSender() {
        return sender;
    }
    public String getContent() {
        return content;
    }
    public String getReceiverNickname() {
        return receiver;
    }
    public boolean isBroadcast() {
        return isBroadcast;
    }
}
