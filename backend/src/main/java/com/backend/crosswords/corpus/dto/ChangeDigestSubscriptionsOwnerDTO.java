package com.backend.crosswords.corpus.dto;

public class ChangeDigestSubscriptionsOwnerDTO {
    private String owner;

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Override
    public String toString() {
        return "ChangeDigestSubscriptionOwnerDTO{" +
                "owner='" + owner + '\'' +
                '}';
    }
}
