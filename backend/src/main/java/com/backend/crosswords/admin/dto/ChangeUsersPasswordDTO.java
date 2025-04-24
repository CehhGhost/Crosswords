package com.backend.crosswords.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class ChangeUsersPasswordDTO {
    @Schema(example = "12345")
    private String oldPassword;
    @Schema(example = "123456")
    private String newPassword;

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    @Override
    public String toString() {
        return "ChangeUsersPasswordDTO{" +
                "oldPassword='" + oldPassword + '\'' +
                ", newPassword='" + newPassword + '\'' +
                '}';
    }
}
