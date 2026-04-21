package org.proptit.localchat.common.models.message;

import org.proptit.localchat.common.enums.TypeMessage;
import org.proptit.localchat.common.models.User;

import java.io.Serializable;

public class FileMessage extends Message implements Serializable {
    private byte[] fileData;
    private String fileName;
    private String fileExtension;

    public FileMessage(User sender, byte[] fileData, String fileName, String fileExtension) {
        super(sender);
        this.fileData = fileData;
        this.fileName = fileName;
        this.fileExtension = fileExtension;
        typeMessage = TypeMessage.FILE;
    }

    public FileMessage(User sender, byte[] fileData, String fileName) {
        super(sender);
        this.fileName = fileName;
        this.fileData = fileData;
        typeMessage = TypeMessage.FILE;
    }

    public FileMessage(User sender) {
        super(sender);
    }

    public static FileMessage createBroadcast(User admin, byte[] fileData, String fileName, String fileExtension) {
        FileMessage msg = new FileMessage(admin, fileData, fileName, fileExtension);
        msg.isBroadcast = true;
        msg.setTypeMessage(TypeMessage.FILE);
        return msg;
    }

    public static FileMessage createPrivate(User sender, User receiver, byte[] fileData, String fileName, String fileExtension) {
        FileMessage msg = new FileMessage(sender, fileData, fileName, fileExtension);
        msg.receiverNickname = receiver.getNickname();
        msg.setReceiver(receiver);
        msg.setTypeMessage(TypeMessage.FILE);
        msg.isBroadcast = false;
        return msg;
    }

    public byte[] getFileData() { return fileData; }
    @Override
    public String getFileName() { return fileName; }
    public String getFileExtension() { return fileExtension; }

    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    @Override
    public String toString() {
        return "[" + sentAt + "] " + sender.getNickname() + " đã gửi file: " + fileName;
    }
}