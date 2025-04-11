package com.backend.crosswords.corpus.models;

import jakarta.persistence.*;

@Table(name = "_digests")
@Entity
public class Digest {
    @EmbeddedId
    private DigestId id;
    @ManyToOne
    @MapsId("coreId")
    @JoinColumn(name = "digest_core_id")
    private DigestCore core;
    @ManyToOne
    @MapsId("subscriptionId")
    @JoinColumn(name = "digest_subscription_id")
    private DigestSubscription subscription;

    public Digest() {
    }

    public Digest(DigestId id, DigestCore core, DigestSubscription subscription) {
        this.id = id;
        this.core = core;
        this.subscription = subscription;
    }

    public DigestId getId() {
        return id;
    }

    public void setId(DigestId id) {
        this.id = id;
    }

    public DigestCore getCore() {
        return core;
    }

    public void setCore(DigestCore core) {
        this.core = core;
    }

    public DigestSubscription getSubscription() {
        return subscription;
    }

    public void setSubscription(DigestSubscription subscription) {
        this.subscription = subscription;
    }
}
