package com.backend.crosswords.corpus.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.sql.Timestamp;

public class CreateUpdateCommentDTO {
    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }


    @Override
    public String toString() {
        return "CreateUpdateCommentDTO{" +
                "text='" + text + '\'' +
                '}';
    }
}
