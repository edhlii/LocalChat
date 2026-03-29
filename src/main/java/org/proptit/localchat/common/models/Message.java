package org.proptit.localchat.common.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.spi.LocaleServiceProvider;

public class Message implements Serializable {
    private static final long serialVersionUID = 2L;
    private User sender;
    private User reciver;
    private String content;
    private String timestamp;

    public Message(User sender, String content) {
        this.sender = sender;
        this.content = content;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
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
}
