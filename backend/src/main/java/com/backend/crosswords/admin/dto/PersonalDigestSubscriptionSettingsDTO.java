package com.backend.crosswords.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO for user's subscription settings")
public class PersonalDigestSubscriptionSettingsDTO {
    @Schema(
            example = "true"
    )
    @JsonProperty("send_to_mail")
    private Boolean sendToMail;
    @Schema(
            example = "true"
    )
    @JsonProperty("mobile_notifications")
    private Boolean mobileNotifications;

    public PersonalDigestSubscriptionSettingsDTO() {
    }

    public PersonalDigestSubscriptionSettingsDTO(Boolean sendToMail, Boolean mobileNotifications) {
        this.sendToMail = sendToMail;
        this.mobileNotifications = mobileNotifications;
    }

    public Boolean getSendToMail() {
        return sendToMail;
    }

    public void setSendToMail(Boolean sendToMail) {
        this.sendToMail = sendToMail;
    }

    public Boolean getMobileNotifications() {
        return mobileNotifications;
    }

    public void setMobileNotifications(Boolean mobileNotifications) {
        this.mobileNotifications = mobileNotifications;
    }

    @Override
    public String toString() {
        return "SubscriptionSettingsDTO{" +
                "sendToMail=" + sendToMail +
                ", mobileNotifications=" + mobileNotifications +
                '}';
    }
}
