package com.backend.crosswords.corpus.models;

import com.backend.crosswords.corpus.enums.Language;
import com.backend.crosswords.corpus.enums.Source;
import jakarta.persistence.*;

import java.sql.Timestamp;
import java.util.List;

@Table(name = "_documents")
@Entity
public class DocMeta {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "date")
    private Timestamp date;

    @Column(name = "last_edit")
    private Timestamp lastEdit;

    @Column(name = "url")
    private String url;

    @Column(name = "language")
    @Enumerated(EnumType.ORDINAL)
    private Language language;

    @Column(name = "source")
    @Enumerated(EnumType.ORDINAL)
    private Source source;

    @ManyToMany(mappedBy = "docs")
    private List<Tag> tags;

    @OneToMany(mappedBy = "doc", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Rating> ratings;

    // TODO добавить модель дайджестов

    // TODO добавить функционал фильтрации дайджестов по дате (от и до), по тегам и по источникам, public/private

    // TODO добавить поиск дайджестов по названию (точный)

    // TODO выгрузка дайджестов в PDF

    public DocMeta() {
    }

    public DocMeta(Long id, String summary, Timestamp date, Timestamp lastEdit, String url, Language language, Source source, List<Tag> tags, List<Rating> ratings) {
        this.id = id;
        this.summary = summary;
        this.date = date;
        this.lastEdit = lastEdit;
        this.url = url;
        this.language = language;
        this.source = source;
        this.tags = tags;
        this.ratings = ratings;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Timestamp getDate() {
        return date;
    }

    public void setDate(Timestamp date) {
        this.date = date;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }


    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public List<Rating> getRatings() {
        return ratings;
    }

    public void setRatings(List<Rating> ratings) {
        this.ratings = ratings;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public Timestamp getLastEdit() {
        return lastEdit;
    }

    public void setLastEdit(Timestamp lastEdit) {
        this.lastEdit = lastEdit;
    }

    @Override
    public String toString() {
        return "DocMeta{" +
                "id=" + id +
                ", summary='" + summary + '\'' +
                ", date=" + date +
                ", lastEdit=" + lastEdit +
                ", url='" + url + '\'' +
                ", language=" + language +
                ", source=" + source +
                ", tags=" + tags +
                ", ratings=" + ratings +
                '}';
    }
}
