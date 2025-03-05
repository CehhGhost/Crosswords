package com.backend.crosswords.corpus.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.sql.Timestamp;
import java.util.List;

public class DocDTO {
    private Long id;
    private String title;
    @JsonProperty("source")
    private String rusSource;
    private String summary;
    private String text;
    @JsonProperty("tags")
    private List<String> tagNames;
    @JsonFormat(pattern = "dd/MM/yyyy")
    private Timestamp date;
    @JsonFormat(pattern = "dd/MM/yyyy")
    @JsonProperty("last_edit")
    private Timestamp lastEdit;
    @JsonProperty("rating_summary")
    private Integer ratingSummary;
    @JsonProperty("rating_classification")
    private Integer ratingClassification;
    private Boolean favourite;
    @JsonProperty("is_authed")
    private Boolean authed;
    private String language;
    private String url;

    public DocDTO(Long id, String summary, Timestamp date, String url, String language, String text, String title) {
        this.id = id;
        this.summary = summary;
        this.date = date;
        this.url = url;
        this.language = language;
        this.text = text;
        this.title = title;
    }

    public DocDTO() {
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

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getRusSource() {
        return rusSource;
    }

    public void setRusSource(String rusSource) {
        this.rusSource = rusSource;
    }

    public List<String> getTagNames() {
        return tagNames;
    }

    public void setTagNames(List<String> tagNames) {
        this.tagNames = tagNames;
    }

    public Timestamp getLastEdit() {
        return lastEdit;
    }

    public void setLastEdit(Timestamp lastEdit) {
        this.lastEdit = lastEdit;
    }

    public Integer getRatingSummary() {
        return ratingSummary;
    }

    public void setRatingSummary(Integer ratingSummary) {
        this.ratingSummary = ratingSummary;
    }

    public Integer getRatingClassification() {
        return ratingClassification;
    }

    public void setRatingClassification(Integer ratingClassification) {
        this.ratingClassification = ratingClassification;
    }

    public Boolean getFavourite() {
        return favourite;
    }

    public void setFavourite(Boolean favourite) {
        this.favourite = favourite;
    }

    public Boolean getAuthed() {
        return authed;
    }

    public void setAuthed(Boolean authed) {
        this.authed = authed;
    }

    @Override
    public String toString() {
        return "DocDTO{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", rusSource='" + rusSource + '\'' +
                ", summary='" + summary + '\'' +
                ", text='" + text + '\'' +
                ", tagDTOs=" + tagNames +
                ", date=" + date +
                ", lastEdit=" + lastEdit +
                ", ratingSummary=" + ratingSummary +
                ", ratingClassification=" + ratingClassification +
                ", favourite=" + favourite +
                ", isAuthed=" + authed +
                ", language='" + language + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
