package com.backend.crosswords.corpus.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "digest_core")
public class DigestCoreES {
    @Id
    private Long id;
    @Field(type = FieldType.Text, name = "text")
    private String text;

    public DigestCoreES() {
    }

    public DigestCoreES(Long id, String text) {
        this.id = id;
        this.text = text;
    }

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
}
