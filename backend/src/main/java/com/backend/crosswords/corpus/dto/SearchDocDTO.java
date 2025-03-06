package com.backend.crosswords.corpus.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.sql.Timestamp;
import java.util.List;

public class SearchDocDTO {
    private Long id;
    @JsonProperty("search_body")
    private String searchTerm;
    @JsonProperty("search_mode")
    private String searchMode;
    @JsonFormat(pattern = "dd/MM/yyyy")
    @JsonProperty("date_from")
    private Timestamp dateFrom;
    @JsonFormat(pattern = "dd/MM/yyyy")
    @JsonProperty("date_to")
    private Timestamp dateTo;
    @JsonProperty("page_number")
    private Integer pageNumber; // бывший last_sentence_pos
    @JsonProperty("matches_per_page")
    private Integer matchesPerPage;
    @JsonProperty("approval_percentage")
    private Float approvalPercentage;
    private List<String> language;
    private List<String> sources;
    private List<String> tags;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public String getSearchMode() {
        return searchMode;
    }

    public void setSearchMode(String searchMode) {
        this.searchMode = searchMode;
    }

    public Timestamp getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(Timestamp dateFrom) {
        this.dateFrom = dateFrom;
    }

    public Timestamp getDateTo() {
        return dateTo;
    }

    public void setDateTo(Timestamp dateTo) {
        this.dateTo = dateTo;
    }

    public List<String> getLanguage() {
        return language;
    }

    public void setLanguage(List<String> language) {
        this.language = language;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Integer getMatchesPerPage() {
        return matchesPerPage;
    }

    public void setMatchesPerPage(Integer matchesPerPage) {
        this.matchesPerPage = matchesPerPage;
    }

    public List<String> getSources() {
        return sources;
    }

    public void setSources(List<String> sources) {
        this.sources = sources;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Float getApprovalPercentage() {
        return approvalPercentage;
    }

    public void setApprovalPercentage(Float approvalPercentage) {
        this.approvalPercentage = approvalPercentage;
    }

    @Override
    public String toString() {
        return "SearchDocDTO{" +
                "id=" + id +
                ", searchTerm='" + searchTerm + '\'' +
                ", searchMode='" + searchMode + '\'' +
                ", dateFrom=" + dateFrom +
                ", dateTo=" + dateTo +
                ", pageNumber=" + pageNumber +
                ", matchesPerPage=" + matchesPerPage +
                ", approvalPercentage=" + approvalPercentage +
                ", language=" + language +
                ", sources=" + sources +
                ", tags=" + tags +
                '}';
    }
}
