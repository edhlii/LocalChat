package org.proptit.localchat.common.models.message;

import org.proptit.localchat.common.enums.TypeMessage;
import org.proptit.localchat.common.models.User;

import java.io.Serializable;

public class ImageMessage extends Message implements Serializable {
    private byte[] imageData;
    private String fileName;

    public ImageMessage(User sender, byte[] imageData, String fileName) {
        super(sender);
        this.imageData = imageData;
        this.fileName = fileName;
        typeMessage = TypeMessage.IMAGE;
    }

    public ImageMessage(User sender) {
        super(sender);
    }

    public static ImageMessage createBroadcast(User admin, byte[] imageData, String fileName) {
        ImageMessage msg = new ImageMessage(admin, imageData, fileName);
        msg.isBroadcast = true;

        msg.setTypeMessage(TypeMessage.IMAGE);
        return msg;
    }

    public static ImageMessage createPrivate(User sender, User receiver, byte[] imageData, String fileName) {
        ImageMessage msg = new ImageMessage(sender, imageData, fileName);
        msg.setReceiver(receiver);
        msg.receiverNickname = receiver.getNickname();
        msg.isBroadcast = false;
        msg.setTypeMessage(TypeMessage.IMAGE);
        return msg;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public String toString() {
        return "[" + sentAt + "] " + sender.getNickname() + " đã gửi một ảnh: " + fileName;
    }
}
