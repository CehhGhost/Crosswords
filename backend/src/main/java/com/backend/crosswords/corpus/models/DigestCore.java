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
    @Column(name = "date")
    private Timestamp date;
    @ManyToOne(cascade = {CascadeType.MERGE})
    @JoinColumn(name = "digest_template_id")
    private DigestTemplate template;
    @ManyToMany(cascade = {CascadeType.MERGE})
    @JoinTable(name = "_digest_cores_docs",
            joinColumns = @JoinColumn(name = "digest_core_id"),
            inverseJoinColumns = @JoinColumn(name = "doc_id"))
    private List<DocMeta> docs;
    @OneToMany(mappedBy = "core", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DigestCoreRating> ratings;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public List<DocMeta> getDocs() {
        return docs;
    }

    public void setDocs(List<DocMeta> docs) {
        this.docs = docs;
    }

    public List<DigestCoreRating> getRatings() {
        return ratings;
    }

    public void setRatings(List<DigestCoreRating> ratings) {
        this.ratings = ratings;
    }
}
