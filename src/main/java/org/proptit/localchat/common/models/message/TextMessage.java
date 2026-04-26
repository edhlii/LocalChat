package org.proptit.localchat.common.models.message;

import org.proptit.localchat.common.enums.TypeMessage;
import org.proptit.localchat.common.models.User;

import java.io.Serializable;

public class TextMessage extends Message implements Serializable {


    protected TextMessage(User sender, String content) {
        super(sender, content);

    }

    public static TextMessage createBroadcast(User admin, String content) {
        TextMessage msg = new TextMessage(admin, content);
        msg.isBroadcast = true;
        msg.setTypeMessage(TypeMessage.TEXT);
        return msg;
    }

    public static TextMessage createPrivate(User sender, User receiver, String content) {
        TextMessage msg = new TextMessage(sender, content);
        msg.setReceiver(receiver);
        msg.receiverNickname = receiver.getNickname();
        msg.isBroadcast = false;
        msg.setTypeMessage(TypeMessage.TEXT);
        return msg;
    }
    @Override
    public String getFileName()
    {
        return null;
    }
    public void setFileName(String fileName)
    {

    }


}