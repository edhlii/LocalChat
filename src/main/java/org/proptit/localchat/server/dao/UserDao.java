package org.proptit.localchat.server.dao;


import org.proptit.localchat.common.models.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDao {
    public List<User> getAllUsers() {
        try (Connection c = DbConnection.openConnection())
        {
            List<User> users = new ArrayList<>();
            String sql = "SELECT * FROM users";
            PreparedStatement ps = c.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                users.add(mapRowToUser(rs));
            return users;
        }
        catch (Exception ex) {
            ex.printStackTrace();

        }
        return null;

    }

    public boolean deleteUser(int userId) {

        try (Connection c = DbConnection.openConnection())
        {
            String sql = "DELETE FROM users WHERE id = ?";
            PreparedStatement ps = c.prepareStatement(sql);
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        }
        catch (Exception e)
        {
            System.out.println("Loi DB ko xoa duoc");
            e.printStackTrace();
        }
        return false;
    }

    public boolean addUser(User user) {

        try (Connection c = DbConnection.openConnection())
        {
            String sql = "INSERT INTO users (username, password, nickname, role) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = c.prepareStatement(sql);
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getNickname());
            ps.setString(4, user.getRole());
            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public User findByUsername(String username) {

        try (Connection c = DbConnection.openConnection()){
             String sql = "SELECT * FROM users WHERE username = ?";
             PreparedStatement ps = c.prepareStatement(sql);
             ps.setString(1, username);
             ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("nickname"),
                        rs.getString("role")
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private User mapRowToUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("nickname"),
                rs.getString("role")
        );
    }
}