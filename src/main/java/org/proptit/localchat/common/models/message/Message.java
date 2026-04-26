package org.proptit.localchat.common.models.message;

import org.proptit.localchat.common.enums.TypeMessage;
import org.proptit.localchat.common.models.User;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class Message implements Serializable {
    protected int id;
    protected TypeMessage typeMessage;
    protected User sender;
    protected User receiver;
    protected String content;


    protected String sentAt;
    protected String receiverNickname;
    protected boolean isBroadcast;

    public Message(User sender) {
        this.sender = sender;
        this.sentAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
    }

    public void setSentAt(String sentAt) {
        this.sentAt = sentAt;
    }

    public String getSentAt() {
        return sentAt;
    }

    public abstract String getFileName();
    public abstract void setFileName(String fileName);

    public TypeMessage getTypeMessage() {
        return typeMessage;
    }

    public Message(User sender, String content) {
        this.sender = sender;
        this.content = content;
        this.sentAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
    }

    public void setTypeMessage(TypeMessage typeMessage) {
        this.typeMessage = typeMessage;
    }

    public User getReceiver() {
        return receiver;
    }

    public boolean isBroadcast() {
        return isBroadcast;
    }

    public String getReceiverNickname() {
        return receiverNickname;
    }

    public User getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public void setReceiver(User receiver) {
        this.receiver = receiver;
    }

    public void setContent(String content) {
        this.content = content;
    }


}