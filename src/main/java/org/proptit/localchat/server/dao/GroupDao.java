package org.proptit.localchat.server.dao;

import org.proptit.localchat.common.models.ChatGroup;
import org.proptit.localchat.common.models.User;
import org.proptit.localchat.server.utils.ServerLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class GroupDao {
    public int createGroup(String groupName, int creatorId, List<Integer> memberIds) {
        Connection conn = null;
        try {
            conn = DbConnection.openConnection();
            conn.setAutoCommit(false);

            String sqlGroup = "INSERT INTO chat_groups (name, created_by) VALUES (?, ?)";
            PreparedStatement psGroup = conn.prepareStatement(sqlGroup, Statement.RETURN_GENERATED_KEYS);
            psGroup.setString(1, groupName);
            psGroup.setInt(2, creatorId);
            psGroup.executeUpdate();

            ResultSet rs = psGroup.getGeneratedKeys();
            int newGroupId = -1;
            if (rs.next()) {
                newGroupId = rs.getInt(1);
            }

            if (newGroupId != -1) {
                String sqlMember = "INSERT INTO group_members (group_id, user_id) VALUES (?, ?)";
                PreparedStatement psMember = conn.prepareStatement(sqlMember);
                for (Integer memberId : memberIds) {
                    psMember.setInt(1, newGroupId);
                    psMember.setInt(2, memberId);
                    psMember.addBatch();
                }
                psMember.executeBatch();
            }
            conn.commit();
            return newGroupId;

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            ServerLogger.error("Database", "Failed to create group: " + groupName);
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        return -1;
    }

    public List<ChatGroup> getGroupsByUserId(int userId) {
        List<ChatGroup> myGroups = new ArrayList<>();


        List<ChatGroup> temp = new ArrayList<>();
        String sql = "SELECT cg.id, cg.name, cg.created_by FROM chat_groups cg " +
                "JOIN group_members gm ON cg.id = gm.group_id WHERE gm.user_id = ?";

        try (Connection conn = DbConnection.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    temp.add(new ChatGroup(
                            rs.getInt("id"),
                            rs.getString("name"),
                            new User(rs.getInt("created_by")),
                            null
                    ));
                }
            }
            for (ChatGroup group : temp) {
                group.setMembers(getFullMembersByGroupId(group.getId()));
                myGroups.add(group);
            }

        } catch (Exception e) {
            ServerLogger.error("Database", "Error fetching groups for user ID: " + userId);
        }
        return myGroups;
    }

    public ChatGroup getGroupById(int groupId) {
        try (Connection conn = DbConnection.openConnection()) {
            String sql = "SELECT id, name, created_by FROM chat_groups WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ChatGroup group = new ChatGroup(rs.getInt("id"), rs.getString("name"), new User(rs.getInt("created_by")), null);
                group.setMembers(getFullMembersByGroupId(groupId));
                return group;
            }

        } catch (Exception e) {
            ServerLogger.error("Database", "Error fetching group data for group ID: " + groupId);
        }
        return null;
    }

    public List<Integer> getMemberIdsByGroupId(int groupId) {
        List<Integer> memberIds = new ArrayList<>();
        String sql = "SELECT user_id FROM group_members WHERE group_id = ?";
        try (Connection conn = DbConnection.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    memberIds.add(rs.getInt("user_id"));
                }
            }
        } catch (Exception e) {
            ServerLogger.error("Database", "Failed to get member IDs for Group: " + groupId);
        }
        return memberIds;
    }


    public List<User> getFullMembersByGroupId(int groupId) {

        try (Connection conn = DbConnection.openConnection()) {
            List<User> members = new ArrayList<>();

            String sql = "SELECT u.id, u.nickname, u.username FROM users u " +
                    "JOIN group_members gm ON u.id = gm.user_id " +
                    "WHERE gm.group_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                User u = new User(rs.getInt("id"));
                u.setNickname(rs.getString("nickname"));
                u.setUsername(rs.getString("username"));
                members.add(u);
            }
            return members;

        } catch (Exception e) {
            ServerLogger.error("Database", "Error getting members for group ID: " + groupId);
        }
        return null;
    }

    public boolean addMembers(int groupId, List<Integer> userIds) {
        String sql = "INSERT INTO group_members (group_id, user_id) VALUES (?, ?)";
        try (Connection conn = DbConnection.openConnection()) {
            PreparedStatement ps = conn.prepareStatement(sql);
            for (Integer userId : userIds) {
                ps.setInt(1, groupId);
                ps.setInt(2, userId);
                ps.addBatch();
            }
            ps.executeBatch();
            return true;
        } catch (Exception e) {
            ServerLogger.error("Database", "Failed to add members to group ID: " + groupId);
            return false;
        }
    }

    public boolean removeMembers(int groupId, List<Integer> userIds) {
        String sql = "DELETE FROM group_members WHERE group_id = ? AND user_id = ?";
        try (Connection conn = DbConnection.openConnection()) {
            PreparedStatement ps = conn.prepareStatement(sql);
            for (Integer userId : userIds) {
                ps.setInt(1, groupId);
                ps.setInt(2, userId);
                ps.addBatch();
            }
            ps.executeBatch();
            return true;
        } catch (Exception e) {
            ServerLogger.error("Database", "Failed to remove members from group ID: " + groupId);
            return false;
        }
    }

    public boolean leaveGroup(int userId, int groupId) {
        String sql = "DELETE FROM group_members WHERE user_id = ? AND group_id = ?";
        try (Connection conn = DbConnection.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, groupId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            ServerLogger.error("Database", "Error: User " + userId + " failed to leave group " + groupId);
        }
        return false;
    }
}
