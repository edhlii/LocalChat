package org.proptit.localchat.common.models;

import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String userame;
    private String password;
    private String nickname;
    private String role;

    public User(Integer id, String userame, String password, String nickname, String role) {
        this.id = id;
        this.userame = userame;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
    }
    public User(String username)
    {
        this.userame = username;
    }

    public Integer getId() {
        return id;
    }

    public String getUserame() {
        return userame;
    }

    public String getNickname() {
        return nickname != null ? nickname : "Unknown";
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public User(String userame, String password) {
        this.password = password;
        this.userame = userame;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isManager() {
        return "MANAGER".equalsIgnoreCase(this.role);
    }
}