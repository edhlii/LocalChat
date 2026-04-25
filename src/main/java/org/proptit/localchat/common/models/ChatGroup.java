package org.proptit.localchat.common.models;

import java.io.Serializable;
import java.util.List;

public class ChatGroup implements Serializable {
    private int id;
    private String name;
    private User createdBy;
    private List<User> members;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public List<User> getMembers() {
        return members;
    }

    public void setMembers(List<User> members) {
        this.members = members;
    }

    public ChatGroup(int id, String name, User createdBy, List<User> members) {
        this.id = id;
        this.name = name;
        this.createdBy = createdBy;
        this.members = members;
    }
}
