package org.proptit.localchat.server.dao;


import org.proptit.localchat.common.enums.TypeMessage;
import org.proptit.localchat.common.models.User;
import org.proptit.localchat.common.models.message.FileMessage;
import org.proptit.localchat.common.models.message.ImageMessage;
import org.proptit.localchat.common.models.message.Message;
import org.proptit.localchat.common.models.message.TextMessage;

import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MessageDao {

    public Integer save(Message msg) {
        try (Connection c = DbConnection.openConnection())
        {
            System.out.println(msg.getTypeMessage().name());
            String sql = "INSERT INTO messages (sender_id, receiver_id, message_type, content, file_name) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            ps.setInt(1, msg.getSender().getId());
            if (msg.isBroadcast())
                ps.setNull(2, Types.INTEGER);
             else
                ps.setInt(2, msg.getReceiver().getId());

            ps.setString(3, msg.getTypeMessage().name());
            ps.setString(4, msg.getContent());

            if (msg.getTypeMessage() == TypeMessage.FILE )
                ps.setString(5, msg.getFileName());
            else if(msg.getTypeMessage() == TypeMessage.IMAGE)
                ps.setString(5, msg.getFileName());
             else
                ps.setNull(5, Types.VARCHAR);


            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
        return -1;
    }
    public List<Message> getHistory(int user1Id, int user2Id) {


        try (Connection c = DbConnection.openConnection())
        {
            List<Message> history = new ArrayList<>();
            String sql = "SELECT m.*, u.nickname AS sender_nickname FROM messages m " +
                    "JOIN users u ON m.sender_id = u.id " +
                    "WHERE (sender_id = ? AND receiver_id = ?) " +
                    "OR (sender_id = ? AND receiver_id = ?) " +
                    "ORDER BY sent_at ASC LIMIT 100";
            PreparedStatement ps = c.prepareStatement(sql);

            ps.setInt(1, user1Id);
            ps.setInt(2, user2Id);
            ps.setInt(3, user2Id);
            ps.setInt(4, user1Id);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {

                User sender = new User(rs.getInt("sender_id"));
                User receiver = new User(rs.getInt("receiver_id"));

                sender.setNickname(rs.getString("sender_nickname"));

                String typeStr = rs.getString("message_type");
                String content = rs.getString("content");
                String fileName = rs.getString("file_name");

                Timestamp dbTimestamp = rs.getTimestamp("sent_at");
                String formattedDate = "";
                if (dbTimestamp != null) {
                    formattedDate = dbTimestamp.toLocalDateTime()
                            .format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
                }
                Message msg;

                if (typeStr.equals("IMAGE")) {

                    msg = new ImageMessage(sender);
                    msg.setContent(content);
                    msg.setTypeMessage(TypeMessage.IMAGE);
                    msg.setFileName(fileName);
                }
                else if(typeStr.equals("FILE"))
                {
                    msg = new FileMessage(sender);
                    msg.setContent(content);
                    msg.setTypeMessage(TypeMessage.FILE);
                    msg.setFileName(fileName);
                }
                else
                    msg = TextMessage.createPrivate(sender, receiver, content);
                msg.setId(rs.getInt("id"));
                msg.setSentAt(formattedDate);
                history.add(msg);
            }
            return history;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Message> getBroadCastHistory() {


        try (Connection c = DbConnection.openConnection())
        {
            List<Message> history = new ArrayList<>();
            String sql = "SELECT m.*, u.nickname AS sender_nickname FROM messages m " +
                    "JOIN users u ON m.sender_id = u.id " +
                    "WHERE m.receiver_id IS NULL " +
                    "ORDER BY sent_at ASC LIMIT 100";
            PreparedStatement ps = c.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {

                User sender = new User(rs.getInt("sender_id"));
                sender.setNickname(rs.getString("sender_nickname"));
                String typeStr = rs.getString("message_type");
                String content = rs.getString("content");
                String fileName = rs.getString("file_name");

                Timestamp dbTimestamp = rs.getTimestamp("sent_at");
                String formattedDate = "";
                if (dbTimestamp != null) {
                    formattedDate = dbTimestamp.toLocalDateTime()
                            .format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
                }
                Message msg;

                if (typeStr.equals("IMAGE")) {

                    msg = new ImageMessage(sender);
                    msg.setContent(content);
                    msg.setTypeMessage(TypeMessage.IMAGE);
                    msg.setFileName(fileName);
                }
                else if(typeStr.equals("FILE"))
                {
                    msg = new FileMessage(sender);
                    msg.setContent(content);
                    msg.setTypeMessage(TypeMessage.FILE);
                    msg.setFileName(fileName);
                }
                else
                    msg = TextMessage.createBroadcast(sender, content);
                msg.setId(rs.getInt("id"));
                msg.setSentAt(formattedDate);
                history.add(msg);
            }
            return history;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Integer> getOfflineNotificationIds(int myId) {
        try (Connection c = DbConnection.openConnection()) {
            List<Integer> notifyIds = new ArrayList<>();

            String sql = "SELECT DISTINCT " +
                    "  CASE WHEN m.receiver_id IS NULL THEN 0 ELSE m.sender_id END as notify_id " +
                    "FROM messages m " +
                    "LEFT JOIN conversation_status cs ON cs.user_id = ? " +
                    "  AND cs.partner_id = (CASE WHEN m.receiver_id IS NULL THEN 0 ELSE m.sender_id END) " +
                    "WHERE (m.receiver_id = ? OR m.receiver_id IS NULL) " +
                    "  AND m.sender_id != ? " +
                    "  AND m.id > COALESCE(cs.last_read_message_id, 0)";;

            PreparedStatement ps = c.prepareStatement(sql);
            ps.setInt(1, myId);
            ps.setInt(2, myId);
            ps.setInt(3, myId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                notifyIds.add(rs.getInt("notify_id"));
            }
            return notifyIds;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updateReadStatus(int myId, int partnerId) {


        try (Connection c = DbConnection.openConnection())
        {
            String sql = "INSERT INTO conversation_status (user_id, partner_id, last_read_message_id) " +
                    "SELECT ?, ?, IFNULL(MAX(id), 0) FROM messages " +
                    "WHERE (" +
                    "  (? = 0 AND receiver_id IS NULL) " +
                    "  OR " +
                    "  (? > 0 AND ((sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?))) " +
                    ") " +
                    "ON DUPLICATE KEY UPDATE last_read_message_id = VALUES(last_read_message_id)";
            PreparedStatement ps = c.prepareStatement(sql);

            ps.setInt(1, myId);
            ps.setInt(2, partnerId);

            ps.setInt(3, partnerId);

            ps.setInt(4, partnerId);
            ps.setInt(5, partnerId);
            ps.setInt(6, myId);
            ps.setInt(7, myId);
            ps.setInt(8, partnerId);

            ps.executeUpdate();
            System.out.println("DEBUG: Đã cập nhật mốc đọc cho " + (partnerId == 0 ? "Thông báo chung" : "User " + partnerId));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
