package org.proptit.localchat.common.models.message;

import org.proptit.localchat.common.models.User;

import java.io.Serializable;

public class FileMessage extends Message implements Serializable {
    private byte[] fileData;
    private String fileName;
    private String fileExtension;

    protected FileMessage(User sender, byte[] fileData, String fileName, String fileExtension) {
        super(sender);
        this.fileData = fileData;
        this.fileName = fileName;
        this.fileExtension = fileExtension;
    }

    public static FileMessage createBroadcast(User admin, byte[] fileData, String fileName, String fileExtension) {
        FileMessage msg = new FileMessage(admin, fileData, fileName, fileExtension);
        msg.isBroadcast = true;
        return msg;
    }

    public static FileMessage createPrivate(User sender, String receiver, byte[] fileData, String fileName, String fileExtension) {
        FileMessage msg = new FileMessage(sender, fileData, fileName, fileExtension);
        msg.receiverNickname = receiver;
        msg.isBroadcast = false;
        return msg;
    }

    public byte[] getFileData() { return fileData; }
    public String getFileName() { return fileName; }
    public String getFileExtension() { return fileExtension; }

    @Override
    public String toString() {
        return "[" + timestamp + "] " + sender.getNickname() + " đã gửi file: " + fileName;
    }
}