package org.proptit.localchat.server.controller;

import org.proptit.localchat.common.enums.TypeDataPacket;
import org.proptit.localchat.common.enums.TypeMessage;
import org.proptit.localchat.common.models.ChatGroup;
import org.proptit.localchat.common.models.DataPacket;
import org.proptit.localchat.common.models.User;
import org.proptit.localchat.common.models.call.CallSignal;
import org.proptit.localchat.common.models.message.FileMessage;
import org.proptit.localchat.common.models.message.ImageMessage;
import org.proptit.localchat.common.models.message.Message;
import org.proptit.localchat.common.utils.PasswordUtils;
import org.proptit.localchat.server.config.StorageConfig;
import org.proptit.localchat.server.dao.GroupDao;
import org.proptit.localchat.server.dao.MessageDao;
import org.proptit.localchat.server.dao.UserDao;
import org.proptit.localchat.server.networks.SocketServer;
import org.proptit.localchat.server.services.AuthService;
import org.proptit.localchat.server.services.StorageFileService;
import org.proptit.localchat.server.utils.ServerLogger;

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

            AuthService authService = new AuthService(new UserDao(), new MessageDao());

            Object receivedData;
            while ((receivedData = in.readObject()) != null) {
                DataPacket data = (DataPacket) receivedData;
                ServerLogger.info("Network", "Received Packet: " + data.getTypeDataPacket() + " from " + (user != null ? user.getUsername() : "Unknown"));

                switch (data.getTypeDataPacket()) {
                    case TypeDataPacket.LOGIN_REQUEST: {
                        authService.handleLogin(this, (User) data.getData());
                        break;
                    }
                    case TypeDataPacket.CHAT_MESSAGE: {
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
                                ServerLogger.info("File", "Saved attachment: " + fileName + " (Stored as: " + uuidName + ")");
                                if (msg.getTypeMessage() == TypeMessage.FILE) {
                                    FileMessage fileMessage = ((FileMessage) msg);
                                    fileMessage.setFileData(null);
                                }
                            } catch (IOException e) {
                                ServerLogger.error("File", "Failed to save attachment from user: " + user.getUsername());
                            }
                        }
                        server.getChatService().processMessage(this, msg);
                        dbExecutor.execute(() -> {
                            messageDao.save(msg);
                            ServerLogger.info("Chat", "Message from [" + user.getUsername() + "] saved to database.");
                        });
                        break;
                    }
                    case TypeDataPacket.GET_ALL_USERS: {
                        List<User> allUsers = userDao.getAllUsers();
                        DataPacket responseData = new DataPacket(TypeDataPacket.RETURN_ALL_USERS, allUsers);
                        sendData(responseData);
                        break;
                    }
                    case TypeDataPacket.GET_ONLINE_USERS: {
                        List<User> onlineUsers = server.getOnlineUsers();
                        sendData(new DataPacket(TypeDataPacket.RETURN_ONLINE_USERS, onlineUsers));
                        break;
                    }
                    case TypeDataPacket.DELETE_USER_REQUEST: {
                        int id = (int) data.getData();
                        userDao.deleteUser(id);
                        break;
                    }
                    case TypeDataPacket.ADD_USER_REQUEST: {
                        User user = (User) data.getData();
                        if (userDao.findByUsername(user.getUsername()) == null) {
                            user.setPassword(PasswordUtils.hashPassword(user.getPassword()));
                            userDao.addUser(user);
                            DataPacket errorPacket = new DataPacket(TypeDataPacket.ADD_ACCOUNT_SUCCESS, userDao.findByUsername(user.getUsername()));
                            ServerLogger.info("Auth", "New account created");
                            sendData(errorPacket);
                        } else {
                            DataPacket errorPacket = new DataPacket(TypeDataPacket.ADD_ACCOUNT_FAILURE, null);
                            ServerLogger.warn("Auth", "Account creation failed");
                            sendData(errorPacket);
                        }
                        break;
                    }
                    case TypeDataPacket.GET_HISTORY_REQUEST: {
                        Integer partnerId = (Integer) data.getData();
                        List<Message> history;
                        if (partnerId == null)
                            history = messageDao.getBroadCastHistory();
                        else
                            history = messageDao.getHistory(this.user.getId(), partnerId);
                        sendData(new DataPacket(TypeDataPacket.RETURN_HISTORY, history));
                        break;
                    }

                    case TypeDataPacket.DOWNLOAD_IMAGE_REQUEST: {
                        String fileName = (String) data.getData();
                        try {
                            ServerLogger.info("File", "User [" + user.getUsername() + "] is requesting image: " + fileName);
                            Path path = Paths.get(StorageConfig.UPLOAD_DIR + fileName);
                            ImageMessage imageMessage = new ImageMessage(null, Files.readAllBytes(path), fileName);
                            imageMessage.setTypeMessage(TypeMessage.IMAGE);
                            DataPacket response = new DataPacket(TypeDataPacket.DOWNLOAD_IMAGE_RESPONSE, imageMessage);
                            this.sendData(response);

                        } catch (IOException e) {
                            ServerLogger.error("File", "Error reading image file [" + fileName + "]: " + e.getMessage());
                        }
                        break;
                    }
                    case TypeDataPacket.DOWNLOAD_FILE_REQUEST: {
                        try {
                            String fileName = (String) data.getData();

                            ServerLogger.info("File", "User [" + user.getUsername() + "] requested download: " + fileName);

                            Path path = Paths.get(StorageConfig.UPLOAD_DIR + fileName);
                            FileMessage fileMessage = new FileMessage(null, Files.readAllBytes(path), fileName);
                            fileMessage.setTypeMessage(TypeMessage.FILE);
                            DataPacket response = new DataPacket(TypeDataPacket.DOWNLOAD_FILE_RESPONSE, fileMessage);
                            this.sendData(response);
                        } catch (Exception e) {
                            ServerLogger.error("File", "File not found or unreadable: " + data.getData());
                        }
                        break;
                    }
                    case TypeDataPacket.GET_CHAT_CONTACTS: {
                        List<User> contacts = userDao.getAllUsers();
                        sendData(new DataPacket(TypeDataPacket.RETURN_CHAT_CONTACTS, contacts));
                        break;
                    }
                    case TypeDataPacket.CALL_SIGNAL: {
                        CallSignal signal = (CallSignal) data.getData();
                        server.forwardCallSignal(signal);
                        break;
                    }
                    case TypeDataPacket.UPDATE_PASS_REQUEST: {
                        User updateUser = (User) data.getData();
                        updateUser.setPassword(PasswordUtils.hashPassword(updateUser.getPassword()));
                        boolean isUpdated = userDao.updatePasswordUser(updateUser);
                        if (isUpdated) {
                            this.user.setPassword(updateUser.getPassword());
                            ServerLogger.info("Auth", "User [" + updateUser.getUsername() + "] updated their profile (Nickname/Avatar).");
                            server.broadcastOnlineUsers();
                        } else {
                            ServerLogger.warn("Auth", "Failed to update profile for User ID: " + updateUser.getId());
                            sendData(new DataPacket(TypeDataPacket.UPDATE_PASS_FAILURE, "Cập nhật thất bại!"));
                        }
                        break;
                    }
                    case CREATE_GROUP_REQUEST: {
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
                            ServerLogger.info("Group", "New group created: " + requestedGroup.getName() + " (ID: " + newGroupId + ")");
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
                            ServerLogger.error("Group", "Database error while creating group: " + requestedGroup.getName());
                            sendData(new DataPacket(TypeDataPacket.CREATE_GROUP_FAILURE, null));
                        }
                        break;
                    }
                    case GET_MY_GROUPS_REQUEST: {
                        int myUserId = (int) data.getData();
                        List<ChatGroup> myGroups = groupDao.getGroupsByUserId(myUserId);
                        sendData(new DataPacket(TypeDataPacket.RETURN_MY_GROUPS, myGroups));
                        break;
                    }
                    case GET_GROUP_HISTORY_REQUEST: {
                        Integer groupIdForHistory = (Integer) data.getData();
                        List<Message> groupHistory = messageDao.getGroupHistory(groupIdForHistory);
                        sendData(new DataPacket(TypeDataPacket.RETURN_HISTORY, groupHistory));
                        break;
                    }

                    case UPDATE_PROFILE_REQUEST: {
                        User u = (User) data.getData();
                        if (userDao.updateProfileInfo(u.getId(), u.getNickname(), u.getAvatar())) {
                            sendData(new DataPacket(TypeDataPacket.UPDATE_PROFILE_SUCCESS, null));
                            ServerLogger.info("Auth", "Profile updated for user: " + u.getUsername());
                            server.broadcastOnlineUsers();
                        } else {
                            sendData(new DataPacket(TypeDataPacket.UPDATE_PROFILE_FAILURE, null));
                            ServerLogger.warn("Auth", "Profile update failed for user: " + u.getUsername());
                        }
                        break;
                    }
                    case MARK_AS_READ: {
                        Integer idReceived = (Integer) data.getData();

                        if (idReceived == 0) {
                            messageDao.updateReadStatus(this.user.getId(), 0, 0);
                        } else if (idReceived > 0) {
                            messageDao.updateReadStatus(this.user.getId(), idReceived, 0);
                        } else {
                            int realGroupId = Math.abs(idReceived);
                            messageDao.updateReadStatus(this.user.getId(), 0, realGroupId);
                        }
                        break;
                    }
                    case TypeDataPacket.GET_OFFLINE_NOTIFICATIONS: {
                        if (this.user != null) {
                            List<Integer> unreadIds = messageDao.getOfflineNotificationIds(this.user.getId());
                            sendData(new DataPacket(TypeDataPacket.RETURN_OFFLINE_NOTIFICATIONS, unreadIds));
                        }
                        break;
                    }
                    case ADD_GROUP_MEMBERS: {
                        ChatGroup addPayload = (ChatGroup) data.getData();
                        List<Integer> addIds = new ArrayList<>();
                        for (User member : addPayload.getMembers()) {
                            addIds.add(member.getId());
                        }
                        if (groupDao.addMembers(addPayload.getId(), addIds)) {
                            ChatGroup updatedGroup = groupDao.getGroupById(addPayload.getId());
                            if (updatedGroup != null) {
                                for (ClientHandler ch : server.getClients()) {
                                    if (ch.getUser() != null) {
                                        int currentClientId = ch.getUser().getId();
                                        if (currentClientId == this.user.getId() || addIds.contains(currentClientId)) {
                                            ch.sendData(new DataPacket(TypeDataPacket.UPDATE_GROUP_SUCCESS, updatedGroup));
                                        }
                                    }
                                }
                            }
                        }
                        break;
                    }
                    case REMOVE_GROUP_MEMBERS: {
                        ChatGroup removePayload = (ChatGroup) data.getData();
                        List<Integer> removeIds = new ArrayList<>();
                        for (User member : removePayload.getMembers()) {
                            removeIds.add(member.getId());
                        }

                        if (groupDao.removeMembers(removePayload.getId(), removeIds)) {
                            for (ClientHandler ch : server.getClients()) {
                                if (ch.getUser() != null && removeIds.contains(ch.getUser().getId())) {
                                    ChatGroup deleteSignal = new ChatGroup(removePayload.getId(), "DELETED_SIGNAL", null, null);
                                    ch.sendData(new DataPacket(TypeDataPacket.UPDATE_GROUP_SUCCESS, deleteSignal));
                                }
                            }
                            ChatGroup fullGroupForAdmin = groupDao.getGroupById(removePayload.getId());
                            sendData(new DataPacket(TypeDataPacket.UPDATE_GROUP_SUCCESS, fullGroupForAdmin));
                        }
                        break;
                    }
                    case LEAVE_GROUP_REQUEST: {
                        int groupIdToLeave = (int) data.getData();
                        boolean left = groupDao.leaveGroup(this.user.getId(), groupIdToLeave);
                        if (left) {
                            ServerLogger.warn("Group", "User [" + user.getUsername() + "] left group ID: " + groupIdToLeave);
                            List<ChatGroup> myUpdatedGroups = groupDao.getGroupsByUserId(this.user.getId());
                            sendData(new DataPacket(TypeDataPacket.RETURN_MY_GROUPS, myUpdatedGroups));
                            List<Integer> remainingMemberIds = groupDao.getMemberIdsByGroupId(groupIdToLeave);
                            for (ClientHandler clientHandler : server.getClients()) {
                                if (clientHandler.getUser() != null && remainingMemberIds.contains(clientHandler.getUser().getId())) {
                                    List<ChatGroup> updatedGroups = groupDao.getGroupsByUserId(clientHandler.getUser().getId());
                                    clientHandler.sendData(new DataPacket(TypeDataPacket.RETURN_MY_GROUPS, updatedGroups));
                                }
                            }
                        }
                        break;
                    }
                }
            }


        } catch (IOException | ClassNotFoundException e) {
            String name = (user != null) ? user.getNickname() : "Unknown";
            ServerLogger.warn("Network", "Connection lost with client: " + name + " (" + e.getMessage() + ")");
        } finally {
            closeEverything();
        }
    }

    public void sendData(Object obj) {
        synchronized (out) {
            try {
                out.writeObject(obj);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
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