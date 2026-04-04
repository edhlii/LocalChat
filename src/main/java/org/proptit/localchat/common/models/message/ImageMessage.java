package org.proptit.localchat.common.models.message;

import org.proptit.localchat.common.models.User;

import java.io.Serializable;

public class ImageMessage extends Message implements Serializable {
    private byte[] imageData;
    private String fileName;

    protected ImageMessage(User sender, byte[] imageData, String fileName) {
        super(sender);
        this.imageData = imageData;
        this.fileName = fileName;
    }

    public static ImageMessage createBroadcast(User admin, byte[] imageData, String fileName) {
        ImageMessage msg = new ImageMessage(admin, imageData, fileName);
        msg.isBroadcast = true;
        return msg;
    }

    public static ImageMessage createPrivate(User sender, String receiver, byte[] imageData, String fileName) {
        ImageMessage msg = new ImageMessage(sender, imageData, fileName);
        msg.receiverNickname = receiver;
        msg.isBroadcast = false;
        return msg;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public String getFileName() {
        return fileName;
    }


    public String toString() {
        return "[" + timestamp + "] " + sender.getNickname() + " đã gửi một ảnh: " + fileName;
    }
}
