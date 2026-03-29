package org.proptit.localchat.common.models;

import org.proptit.localchat.common.enums.TypeMessage;

public class MessagePacket {
    private TypeMessage type;
    private String sender;
    private String receiver;
    private Object data;

    public MessagePacket(TypeMessage type, String sender, String receiver, Object data) {
        this.type = type;
        this.sender = sender;
        this.receiver = receiver;
        this.data = data;
    }

    public TypeMessage getType() {
        return type;
    }

    public void setType(TypeMessage type) {
        this.type = type;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
