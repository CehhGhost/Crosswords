package com.backend.crosswords.corpus.models;

import com.backend.crosswords.admin.models.User;
import jakarta.persistence.*;

@Table(name = "_digest_ratings")
@Entity
public class DigestRating {
    @EmbeddedId
    private DigestRatingId id;
    @Column(name = "digest_rating")
    private Integer digestRating;
    @ManyToOne
    @MapsId("digestCoreId")
    @JoinColumn(name = "digest_core_id")
    private DigestCore core;
    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;
    public DigestRating(DigestRatingId id, DigestCore core, User user, Integer digestRating) {
        this.id = id;
        this.core = core;
        this.user = user;
        this.digestRating = digestRating;
    }

    public DigestRating() {
    }

    public DigestRatingId getId() {
        return id;
    }

    public void setId(DigestRatingId id) {
        this.id = id;
    }

    public DigestCore getCore() {
        return core;
    }

    public void setCore(DigestCore core) {
        this.core = core;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getDigestRating() {
        return digestRating;
    }

    public void setDigestRating(Integer digestRating) {
        this.digestRating = digestRating;
    }
}
