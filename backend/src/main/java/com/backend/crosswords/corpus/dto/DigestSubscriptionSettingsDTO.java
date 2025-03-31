package com.backend.crosswords.corpus.dto;

public class DigestSubscriptionSettingsDTO {
    private Boolean subscribed;
    private Boolean send_to_mail;
    private Boolean mobile_notifications;

    public Boolean getSubscribed() {
        return subscribed;
    }

    public void setSubscribed(Boolean subscribed) {
        this.subscribed = subscribed;
    }

    public Boolean getSend_to_mail() {
        return send_to_mail;
    }

    public void setSend_to_mail(Boolean send_to_mail) {
        this.send_to_mail = send_to_mail;
    }

    public Boolean getMobile_notifications() {
        return mobile_notifications;
    }

    public void setMobile_notifications(Boolean mobile_notifications) {
        this.mobile_notifications = mobile_notifications;
    }
}
