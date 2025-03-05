package com.backend.crosswords.corpus.models;

import com.backend.crosswords.admin.models.User;
import jakarta.persistence.*;

@Table(name = "_ratings")
@Entity
public class Rating {
    @EmbeddedId
    private RatingId id;

    @ManyToOne
    @MapsId("docId")
    @JoinColumn(name = "doc_id")
    private DocMeta doc;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    private Integer summaryRating;

    private Integer classificationRating;

    public Rating(RatingId id, DocMeta doc, User user, Integer summaryRating, Integer classificationRating) {
        this.id = id;
        this.doc = doc;
        this.user = user;
        this.summaryRating = summaryRating;
        this.classificationRating = classificationRating;
    }

    public Rating() {
    }

    public RatingId getId() {
        return id;
    }

    public void setId(RatingId id) {
        this.id = id;
    }

    public DocMeta getDoc() {
        return doc;
    }

    public void setDoc(DocMeta doc) {
        this.doc = doc;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getSummaryRating() {
        return summaryRating;
    }

    public void setSummaryRating(Integer summaryRating) {
        this.summaryRating = summaryRating;
    }

    public Integer getClassificationRating() {
        return classificationRating;
    }

    public void setClassificationRating(Integer classificationRating) {
        this.classificationRating = classificationRating;
    }
}
