package com.backend.crosswords.corpus.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class CreateUpdateAnnotationDTO {
    @JsonProperty("start")
    private Integer startPos;
    @JsonProperty("end")
    private Integer endPos;
    private List<String> comments;

    public Integer getStartPos() {
        return startPos;
    }

    public void setStartPos(Integer startPos) {
        this.startPos = startPos;
    }

    public Integer getEndPos() {
        return endPos;
    }

    public void setEndPos(Integer endPos) {
        this.endPos = endPos;
    }

    public List<String> getComments() {
        return comments;
    }

    public void setComments(List<String> comments) {
        this.comments = comments;
    }

    @Override
    public String toString() {
        return "CreateUpdateAnnotationDTO{" +
                "startPos=" + startPos +
                ", endPos=" + endPos +
                ", comments=" + comments +
                '}';
    }
}
