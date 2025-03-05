package com.backend.crosswords.corpus.models;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "document")
public class DocES {
    @Id
    private Long id;
    @Field(type = FieldType.Text, name = "text")
    private String text;

    @Field(type = FieldType.Text, name = "title")
    private String title;

    public DocES() {
    }

    public DocES(Long id, String text, String title) {
        this.id = id;
        this.text = text;
        this.title = title;
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

    @Override
    public String toString() {
        return "DocES{" +
                "text='" + text + '\'' +
                ", title='" + title + '\'' +
                '}';
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
