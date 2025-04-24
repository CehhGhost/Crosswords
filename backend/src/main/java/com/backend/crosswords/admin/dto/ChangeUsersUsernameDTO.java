package com.backend.crosswords.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChangeUsersUsernameDTO {
    @JsonProperty("new_email")
    private String newEmail;

    public String getNewEmail() {
        return newEmail;
    }

    public void setNewEmail(String newEmail) {
        this.newEmail = newEmail;
    }
}
