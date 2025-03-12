package com.backend.crosswords.corpus.models;

import jakarta.persistence.*;

import java.sql.Timestamp;
import java.util.List;

@Table(name = "_digest_cores")
@Entity
public class DigestCore {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "text", columnDefinition = "TEXT")
    private String text;
    @Column(name = "date")
    private Timestamp date;
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "digest_template_id")
    private DigestTemplate template;
    @OneToMany(mappedBy = "core")
    private List<DigestSubscription> subscriptions;
    @ManyToMany(mappedBy = "digestCores")
    private List<DocMeta> docs;
    @OneToMany(mappedBy = "core", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DigestRating> ratings;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Timestamp getDate() {
        return date;
    }

    public void setDate(Timestamp date) {
        this.date = date;
    }

    public DigestTemplate getTemplate() {
        return template;
    }

    public void setTemplate(DigestTemplate template) {
        this.template = template;
    }

    public List<DigestSubscription> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(List<DigestSubscription> subscriptions) {
        this.subscriptions = subscriptions;
    }

    public List<DocMeta> getDocs() {
        return docs;
    }

    public void setDocs(List<DocMeta> docs) {
        this.docs = docs;
    }

    public List<DigestRating> getRatings() {
        return ratings;
    }

    public void setRatings(List<DigestRating> ratings) {
        this.ratings = ratings;
    }
}
