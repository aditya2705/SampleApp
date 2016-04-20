package com.adityarathi.sample.objects;

import java.io.Serializable;

public class UserObject implements Serializable {

    private String first_name, last_name, user_name;

    public UserObject(String first_name, String last_name, String user_name) {
        this.first_name = first_name;
        this.last_name = last_name;
        this.user_name = user_name;
    }

    public String getFirst_name() {
        return first_name;
    }

    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }

    public String getLast_name() {
        return last_name;
    }

    public void setLast_name(String last_name) {
        this.last_name = last_name;
    }

    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }
}