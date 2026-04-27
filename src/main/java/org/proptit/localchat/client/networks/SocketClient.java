package org.proptit.localchat.client.networks;


import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.proptit.localchat.client.controller.ChatController;
import org.proptit.localchat.client.controller.LoginController;
import org.proptit.localchat.client.controller.MainWindowController;
import org.proptit.localchat.common.enums.TypeDataPacket;
import javafx.application.Platform;
import org.proptit.localchat.common.enums.TypeMessage;
import org.proptit.localchat.common.models.ChatGroup;
import org.proptit.localchat.common.models.DataPacket;
import org.proptit.localchat.common.models.User;

import org.proptit.localchat.common.models.message.FileMessage;
import org.proptit.localchat.common.models.message.ImageMessage;

import org.proptit.localchat.common.models.call.CallSignal;

import org.proptit.localchat.common.models.message.Message;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SocketClient implements Runnable {
    private String host;
    private int port;
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private User user;
    private boolean isRunning = true;
    private MainWindowController controller;

    private LoginController loginController;
    private Map<String, ImageView> pendingImages = new HashMap<>();


    public SocketClient(String host, int port, User user) {
        this.host = host;
        this.port = port;
        this.user = user;
    }

    @Override
    public void run() {
        try {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());



            Object response;
            while (isRunning && (response = in.readObject()) != null) {
                handleServerPacket((DataPacket) response);
            }
        }
        catch (IOException | ClassNotFoundException e) {
            System.err.println("Can not connect to Server: " + e.getMessage());
        } finally {
            closeEverything();
        }
    }

        private void handleServerPacket(DataPacket data) {
        switch (data.getTypeDataPacket()) {
            case TypeDataPacket.LOGIN_SUCCESS:
                this.user = (User) data.getData();

                loginController.handleLoginResult(true, this.user);
                break;
            case TypeDataPacket.LOGIN_FAILED:
                loginController.handleLoginResult(false, data.getData());
                break;
            case TypeDataPacket.CHAT_MESSAGE:
                Message msg = (Message) data.getData();
                if (controller != null) {
                    Platform.runLater(() -> {
                        controller.receiveMessage(msg);
                    });
                } else {
                    System.out.println("The interface is not ready yet: " + msg.toString());
                }
                break;
            case TypeDataPacket.RETURN_ALL_USERS:
                controller.updateMemberList((List<User>) data.getData());
                break;
            case TypeDataPacket.RETURN_ONLINE_USERS:
                if (controller != null) {
                    controller.updateOnlinePeople((List<User>) data.getData());
                }
                break;
            case TypeDataPacket.ADD_ACCOUNT_SUCCESS:
                User user = (User)data.getData();
                Platform.runLater(() -> {
                    controller.addMemberToUI(user);
                });
                break;

            case TypeDataPacket.ADD_ACCOUNT_FAILURE:
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Create account failure...");
                    alert.setHeaderText(null);
                    alert.setGraphic(null);
                    alert.showAndWait();
                });
                break;
            case TypeDataPacket.RETURN_HISTORY:
                List<Message> history = (List<Message>) data.getData();
                controller.getChatAreaController().loadHistory(history);
                break;
            case TypeDataPacket.DOWNLOAD_IMAGE_RESPONSE:
                {
                    ImageMessage imageMessage = (ImageMessage)data.getData();
                    ImageView targetIv = pendingImages.get(imageMessage.getFileName());
                    if (targetIv != null && imageMessage.getImageData() != null) {
                        Image img = new Image(new ByteArrayInputStream(imageMessage.getImageData()));
                        Platform.runLater(() -> {
                            targetIv.setImage(img);
                            pendingImages.remove(imageMessage.getFileName());
                        });
                    }
                }
                break;
            case TypeDataPacket.DOWNLOAD_FILE_RESPONSE:
                FileMessage fileMessage = (FileMessage)data.getData();
                controller.getChatAreaController().handleFileDownloadResponse(fileMessage.getFileName(), fileMessage.getFileData());
                break;
            case TypeDataPacket.RETURN_CHAT_CONTACTS:
                List<User> contactList = (List<User>) data.getData();
                if (controller != null) {
                    Platform.runLater(() -> {
                        controller.updateChatContacts(contactList);
                    });
                }
                break;
            case TypeDataPacket.CALL_SIGNAL:
                if (controller != null) {
                    CallSignal signal = (CallSignal) data.getData();
                    Platform.runLater(() -> controller.receiveCallSignal(signal));
                }
                break;
            case TypeDataPacket.UPDATE_PASS_SUCCESS:
                this.user = (User) data.getData();
                controller.setMe((User) data.getData());
                controller.getUserSettingsController().setMe((User) data.getData());
                controller.getUserSettingsController().getChangePasswordController().closeWindow();
                break;
            case UPDATE_PROFILE_SUCCESS:
                User updatedUser = (User) data.getData();
                if (controller != null) {
                    controller.getUserSettingsController().closeWindow(updatedUser);
                }
                break;
            case RETURN_OFFLINE_NOTIFICATIONS:
                List<Integer> unreadIds = (List<Integer>) data.getData();

                Platform.runLater(() -> {
                    if (controller != null && controller.getChatAreaController() != null) {
                        controller.getChatAreaController().setOfflineMessages(unreadIds);
                    } else {
                        System.err.println("LỖI: Controller chưa sẵn sàng để hiện thông báo!");
                    }
                });

                break;
            case CREATE_GROUP_SUCCESS:
                ChatGroup newGroup = (ChatGroup) data.getData();
                ChatController.getInstance().onGroupCreatedSuccess(newGroup);
                break;
            case CREATE_GROUP_FAILURE:
                String errorMsg = (String) data.getData();
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Lỗi tạo nhóm: " + errorMsg);
                    alert.setHeaderText(null);
                    String css = getClass().getResource("/org/proptit/localchat/create_group.css").toExternalForm();
                    alert.getDialogPane().getStylesheets().add(css);

                    alert.show();

                    javafx.stage.Window.getWindows().stream()
                            .filter(w -> w instanceof Stage)
                            .map(w -> (Stage) w)
                            .filter(stage -> "Tạo Nhóm Mới".equals(stage.getTitle()))
                            .findFirst()
                            .ifPresent(Stage::close);
                });
                break;
            case RETURN_MY_GROUPS:
                List<ChatGroup> loadedGroups = (List<ChatGroup>) data.getData();
                ChatController.getInstance().setMyGroupsList(loadedGroups);
                break;
        }
    }

    public void sendData(Object data) {
        try {
            if (out != null) {
                out.writeObject(data);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setLoginController(LoginController loginController) {
        this.loginController = loginController;
    }

    private void closeEverything() {
        try {
            isRunning = false;
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setController(MainWindowController controller) {
        this.controller = controller;
    }



    public void sendRequestDownload(String fileName, ImageView imageView) {

        pendingImages.put(fileName, imageView);


        DataPacket packet = new DataPacket(TypeDataPacket.DOWNLOAD_IMAGE_REQUEST, fileName);


        this.sendData(packet);

        System.out.println("Đang xin Server file: " + fileName);
    }

}
