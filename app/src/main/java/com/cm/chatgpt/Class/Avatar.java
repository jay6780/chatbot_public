package com.cm.chatgpt.Class;

public class Avatar {
    private int Avatar;
    private String gender;

    public Avatar(int Avatar,String gender) {
        this.Avatar = Avatar;
        this.gender = gender;
    }

    public String getGender() {
        return gender;
    }

    public int getAvatar() {
        return Avatar;
    }
}
