package org.proptit.localchat.server.controller;

import org.proptit.localchat.common.enums.TypeDataPacket;
import org.proptit.localchat.common.enums.TypeMessage;
import org.proptit.localchat.common.models.ChatGroup;
import org.proptit.localchat.common.models.DataPacket;

import org.proptit.localchat.common.models.message.FileMessage;
import org.proptit.localchat.common.models.message.ImageMessage;

import org.proptit.localchat.common.models.call.CallSignal;

import org.proptit.localchat.common.models.message.Message;
import org.proptit.localchat.common.models.User;
import org.proptit.localchat.common.utils.PasswordUtils;
import org.proptit.localchat.server.config.StorageConfig;
import org.proptit.localchat.server.dao.GroupDao;
import org.proptit.localchat.server.dao.MessageDao;
import org.proptit.localchat.server.dao.UserDao;
import org.proptit.localchat.server.networks.SocketServer;
import org.proptit.localchat.server.services.AuthService;
import org.proptit.localchat.server.services.StorageFileService;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientHandler implements Runnable {
    private Socket socket;
    private SocketServer server;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private User user;
    private UserDao userDao = new UserDao();
    private GroupDao groupDao = new GroupDao();
    private StorageFileService storageFileServicefileService = new StorageFileService();


    private static final ExecutorService dbExecutor = Executors.newFixedThreadPool(10);
    private MessageDao messageDao = new MessageDao();

    public ClientHandler(Socket socket, SocketServer server) {
        this.socket = socket;
        this.server = server;
    }


    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            AuthService authService = new AuthService(new UserDao());

            Object receivedData;
            while ((receivedData = in.readObject()) != null)
            {
                DataPacket data = (DataPacket)receivedData;
                switch (data.getTypeDataPacket())
                {
                    case TypeDataPacket.LOGIN_REQUEST:
                        authService.handleLogin(this, (User)data.getData());
                        break;
                    case TypeDataPacket.CHAT_MESSAGE:
                        Message msg = (Message) data.getData();
                        if (msg.getTypeMessage() == TypeMessage.IMAGE || msg.getTypeMessage() == TypeMessage.FILE) {
                            try {
                                byte[] fileBytes;
                                String fileName;

                                if (msg.getTypeMessage() == TypeMessage.IMAGE) {
                                    fileBytes = ((ImageMessage) msg).getImageData();
                                    fileName = ((ImageMessage) msg).getFileName();
                                } else {
                                    fileBytes = ((FileMessage) msg).getFileData();
                                    fileName = ((FileMessage) msg).getFileName();
                                }

                                String uuidName = storageFileServicefileService.saveFile(fileBytes, fileName);

                                msg.setContent(uuidName);

                                if (msg.getTypeMessage() == TypeMessage.FILE) {
                                    FileMessage fileMessage = ((FileMessage) msg);
                                    fileMessage.setFileData(null);

                                }
                            } catch (IOException e) {
                                System.err.println("luu file that bai");
                            }
                        }


                        server.getChatService().processMessage(this, msg);

                        dbExecutor.execute(() -> {

                            messageDao.save(msg);
                            System.out.println("da luu tin nhan");
                        });


                        break;
                    case TypeDataPacket.GET_ALL_USERS:
                        List<User> allUsers = userDao.getAllUsers();
                        DataPacket responseData = new DataPacket(TypeDataPacket.RETURN_ALL_USERS, allUsers);
                        sendData(responseData);
                        break;
                    case TypeDataPacket.GET_ONLINE_USERS:
                        List<User> onlineUsers = server.getOnlineUsers();
                        sendData(new DataPacket(TypeDataPacket.RETURN_ONLINE_USERS, onlineUsers));
                        break;
                    case TypeDataPacket.DELETE_USER_REQUEST:
                        int id = (int)data.getData();
                        userDao.deleteUser(id);
                        break;
                    case TypeDataPacket.ADD_USER_REQUEST:
                        User user = (User)data.getData();
                        if(userDao.findByUsername(user.getUsername()) == null)
                        {
                            user.setPassword(PasswordUtils.hashPassword(user.getPassword()));
                            userDao.addUser(user);
                            DataPacket errorPacket = new DataPacket(TypeDataPacket.ADD_ACCOUNT_SUCCESS, userDao.findByUsername(user.getUsername()));
                            sendData(errorPacket);
                        }
                        else
                        {
                            DataPacket errorPacket = new DataPacket(TypeDataPacket.ADD_ACCOUNT_FAILURE, null);
                            sendData(errorPacket);
                        }
                        break;
                    case TypeDataPacket.GET_HISTORY_REQUEST:
                        Integer partnerId = (Integer) data.getData();
                        List<Message> history;
                        if(partnerId == null)
                            history = messageDao.getBroadCastHistory();
                        else
                            history = messageDao.getHistory(this.user.getId(), partnerId);
                        sendData(new DataPacket(TypeDataPacket.RETURN_HISTORY, history));
                        break;

                    case TypeDataPacket.DOWNLOAD_IMAGE_REQUEST:
                    {
                        String fileName = (String) data.getData();
                        try {

                            Path path = Paths.get(StorageConfig.UPLOAD_DIR + fileName);
                            ImageMessage imageMessage = new ImageMessage(null, Files.readAllBytes(path), fileName);
                            imageMessage.setTypeMessage(TypeMessage.IMAGE);



                            DataPacket response = new DataPacket(TypeDataPacket.DOWNLOAD_IMAGE_RESPONSE, imageMessage);
                            this.sendData(response);

                        } catch (IOException e) {
                            System.err.println("Không tìm thấy file");
                        }
                        break;
                    }
                    case TypeDataPacket.DOWNLOAD_FILE_REQUEST:
                        try
                        {
                            String fileName = (String) data.getData();
                            Path path = Paths.get(StorageConfig.UPLOAD_DIR + fileName);
                            FileMessage fileMessage = new FileMessage(null, Files.readAllBytes(path), fileName);
                            fileMessage.setTypeMessage(TypeMessage.FILE);
                            DataPacket response = new DataPacket(TypeDataPacket.DOWNLOAD_FILE_RESPONSE, fileMessage);
                            this.sendData(response);
                        }
                        catch (Exception e)
                        {
                            System.out.println("loi doc file");
                        }

                        break;
                    case TypeDataPacket.GET_CHAT_CONTACTS:
                        List<User> contacts = userDao.getAllUsers();
                        sendData(new DataPacket(TypeDataPacket.RETURN_CHAT_CONTACTS, contacts));
                        break;
                    case TypeDataPacket.CALL_SIGNAL:
                        CallSignal signal = (CallSignal) data.getData();
                        server.forwardCallSignal(signal);
                        break;

                    case TypeDataPacket.UPDATE_PROFILE_REQUEST:
                        User updateUser = (User) data.getData();
                        updateUser.setPassword(PasswordUtils.hashPassword(updateUser.getPassword()));
                        boolean isUpdated = userDao.updateUser(updateUser);
                        if(isUpdated) {
                            this.user.setNickname(updateUser.getNickname());
                            this.user.setPassword(updateUser.getPassword());

                            sendData(new DataPacket(TypeDataPacket.UPDATE_PROFILE_SUCCESS, this.user));
                            server.broadcastOnlineUsers();
                        }
                        else {
                            sendData(new DataPacket(TypeDataPacket.UPDATE_PROFILE_FAILURE, "Cập nhật thất bại!"));
                        }
                        break;
                    case CREATE_GROUP_REQUEST:
                        System.out.println("SERVER: Đã nhận yêu cầu tạo nhóm!");
                        ChatGroup requestedGroup = (ChatGroup) data.getData();

                        List<Integer> memberIds = new ArrayList<>();
                        for (User member : requestedGroup.getMembers()) {
                            memberIds.add(member.getId());
                        }

                        int newGroupId = groupDao.createGroup(
                                requestedGroup.getName(),
                                requestedGroup.getCreatedBy().getId(),
                                memberIds
                        );

                        if (newGroupId != -1) {
                            System.out.println("SERVER: Tạo nhóm thành công! ID = " + newGroupId);
                            requestedGroup.setId(newGroupId);

                            for (ClientHandler ch : server.getClients()) {
                                if (ch.getUser() != null) {
                                    boolean isMember = false;
                                    for (User member : requestedGroup.getMembers()) {
                                        if (member.getId().equals(ch.getUser().getId()) || (int) member.getId() == (int) ch.getUser().getId()) {
                                            isMember = true;
                                            break;
                                        }
                                    }

                                    if (isMember) {
                                        ch.sendData(new DataPacket(TypeDataPacket.CREATE_GROUP_SUCCESS, requestedGroup));
                                    }
                                }
                            }
                        } else {
                            System.out.println("SERVER: Lỗi DB khi tạo nhóm!");
                            sendData(new DataPacket(TypeDataPacket.CREATE_GROUP_FAILURE, "Lỗi DB khi tạo nhóm!"));
                        }
                        break;
                    case GET_MY_GROUPS_REQUEST:
                        int myUserId = (int) data.getData();
                        List<ChatGroup> myGroups = groupDao.getGroupsByUserId(myUserId);
                        sendData(new DataPacket(TypeDataPacket.RETURN_MY_GROUPS, myGroups));
                        break;
                    case GET_GROUP_HISTORY_REQUEST:
                        Integer groupIdForHistory = (Integer) data.getData();
                        List<Message> groupHistory = messageDao.getGroupHistory(groupIdForHistory);
                        sendData(new DataPacket(TypeDataPacket.RETURN_HISTORY, groupHistory));
                        break;
                }
            }


        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Log: Connection lost with " + (user != null ? user.getNickname() : "unknown"));
        } finally {
            closeEverything();
        }
    }

    public void sendData(Object obj) {
        try {
            out.writeObject(obj);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeEverything() {
        try {
            server.removeClient(this);
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public User getUser() {
        return this.user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public SocketServer getServer() {
        return server;
    }
}