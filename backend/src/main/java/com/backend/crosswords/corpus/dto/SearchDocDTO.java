package com.backend.crosswords.corpus.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.sql.Timestamp;
import java.util.List;

@Schema(description = "DTO for searching document, if any filter's arguments are null, then their check automatically approves")
public class SearchDocDTO {
    @Schema(
            description = "Documents' id for search by id (optional)",
            example = "3"
    )
    private Long id;

    @Schema(
            example = "ставший лучшим спортом"
    )
    @JsonProperty("search_body")
    private String searchTerm;

    @Schema(
            description = "Search mode ('id'/'semantic'/'certain')",
            example = "semantic"
    )
    @JsonProperty("search_mode")
    private String searchMode;

    @Schema(
            description = "The date of the beginning of the searching period in format dd/MM/yyyy. Can be later then date_to, they would simply exchange values",
            example = "25/03/2000"
    )
    @JsonFormat(pattern = "dd/MM/yyyy")
    @JsonProperty("date_from")
    private Timestamp dateFrom;

    @Schema(
            description = "The date of the end of the searching period in format dd/MM/yyyy. Can be before then date_from, they would simply exchange values",
            example = "25/03/2025"
    )
    @JsonFormat(pattern = "dd/MM/yyyy")
    @JsonProperty("date_to")
    private Timestamp dateTo;

    @Schema(
            description = "Page number for pagination (begins from 0). Modified version of the last_sentence_pos",
            example = "0"
    )
    @JsonProperty("page_number")
    private Integer pageNumber;

    @Schema(
            description = "The number of results on the page",
            example = "20"
    )
    @JsonProperty("matches_per_page")
    private Integer matchesPerPage;

    @Schema(
            description = "The experimental argument, it gives a percentage of the effect on scoring hits in ES, by the numbers of the search terms. The higher value, the more accurate the results",
            example = "0.5"
    )
    @JsonProperty("approval_percentage")
    private Float approvalPercentage;

    @Schema(
            description = "The list of probable languages, if document's language contains in them, then it gets through",
            example = "[\"RU\"]"
    )
    private List<String> language;

    @Schema(
            description = "The list of probable sources, if document's source contains in them, then it gets through",
            example = "[\"Интерфакс\"]"
    )
    private List<String> sources;

    @Schema(
            description = "The list of required tags, if document doesn't have at least one of them, then it doesn't get through",
            example = "[\"Спорт\"]"
    )
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
