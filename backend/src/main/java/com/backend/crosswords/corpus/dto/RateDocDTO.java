package com.backend.crosswords.corpus.dto;

public class RateDocDTO {
    private Integer summaryRating;
    private Integer classificationRating;

    public RateDocDTO(Integer summaryRating, Integer classificationRating) {
        this.summaryRating = summaryRating;
        this.classificationRating = classificationRating;
    }

    public RateDocDTO() {
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

    @Override
    public String toString() {
        return "RateDocDTO{" +
                "summaryRating=" + summaryRating +
                ", classificationRating=" + classificationRating +
                '}';
    }
}
