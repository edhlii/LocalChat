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
    public User(String userame)
    {
        this.userame = userame;
    }

    public Integer getId() {
        return id;
    }

    public String getUserame() {
        return userame;
    }

    public String getNickname() {
        return nickname;
    }

    public String getRole() {
        return role;
    }

    public boolean isAdmin() {
        return this.role.equalsIgnoreCase("admin");
    }
}