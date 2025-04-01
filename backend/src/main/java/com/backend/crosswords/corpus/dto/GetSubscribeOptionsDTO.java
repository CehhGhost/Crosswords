package com.backend.crosswords.corpus.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GetSubscribeOptionsDTO {
    @JsonProperty("send_to_mail")
    private Boolean sendToMail;
    @JsonProperty("mobile_notifications")
    private Boolean mobileNotifications;
    private Boolean subscribed;

    public GetSubscribeOptionsDTO() {
    }

    public GetSubscribeOptionsDTO(Boolean sendToMail, Boolean mobileNotifications, Boolean subscribed) {
        this.sendToMail = sendToMail;
        this.mobileNotifications = mobileNotifications;
        this.subscribed = subscribed;
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

    public Boolean getSubscribed() {
        return subscribed;
    }

    public void setSubscribed(Boolean subscribed) {
        this.subscribed = subscribed;
    }

    @Override
    public String toString() {
        return "GetSubscribeOptionsDTO{" +
                "sendToMail=" + sendToMail +
                ", mobileNotifications=" + mobileNotifications +
                ", subscribed=" + subscribed +
                '}';
    }
}
