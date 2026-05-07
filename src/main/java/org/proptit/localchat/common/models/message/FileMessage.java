package org.proptit.localchat.common.models.message;

import org.proptit.localchat.common.enums.TypeMessage;
import org.proptit.localchat.common.models.ChatGroup;
import org.proptit.localchat.common.models.User;

import java.io.Serializable;

public class FileMessage extends Message implements Serializable {
    private byte[] fileData;
    private String fileName;

    public FileMessage(User sender, byte[] fileData, String fileName) {
        super(sender);
        this.fileData = fileData;
        this.fileName = fileName;
        typeMessage = TypeMessage.FILE;
    }


    public FileMessage(User sender) {
        super(sender);
    }

    public static FileMessage createBroadcast(User admin, byte[] fileData, String fileName) {
        FileMessage msg = new FileMessage(admin, fileData, fileName);
        msg.isBroadcast = true;
        msg.setTypeMessage(TypeMessage.FILE);
        return msg;
    }

    public static FileMessage createPrivate(User sender, User receiver, byte[] fileData, String fileName) {
        FileMessage msg = new FileMessage(sender, fileData, fileName);
        msg.receiverNickname = receiver.getNickname();
        msg.setReceiver(receiver);
        msg.setTypeMessage(TypeMessage.FILE);
        msg.isBroadcast = false;
        return msg;
    }

    public static FileMessage createGroup(User sender, ChatGroup group, byte[] fileData, String fileName) {
        FileMessage msg = new FileMessage(sender, fileData, fileName);
        msg.setGroupId(group.getId());
        msg.isBroadcast = false;
        msg.setTypeMessage(TypeMessage.FILE);
        return msg;
    }

    public byte[] getFileData() {
        return fileData;
    }

    @Override
    public String getFileName() {
        return fileName;
    }


    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }


    @Override
    public String toString() {
        return "[" + sentAt + "] " + sender.getNickname() + " đã gửi file: " + fileName;
    }
}