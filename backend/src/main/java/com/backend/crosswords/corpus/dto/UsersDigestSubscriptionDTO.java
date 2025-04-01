package com.backend.crosswords.corpus.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.sql.Timestamp;
import java.util.List;

public class UsersDigestSubscriptionDTO {
    private Long id;
    private String title;
    private String description;

    @ArraySchema(schema = @Schema(description = "List of sources for the digest", example = "Интерфакс"))
    private List<String> sources;

    @ArraySchema(schema = @Schema(description = "List of tags associated with the digest", example = "Спорт"))
    private List<String> tags;

    @JsonProperty("subscribe_options")
    private GetSubscribeOptionsDTO subscribeOptions;

    @JsonProperty("public")
    @Schema(description = "Indicates if the digest is public", example = "true")
    private Boolean isPublic;

    @Schema(example = "admin")
    @JsonProperty("owner")
    private String ownersUsername;

    @JsonProperty("is_owner")
    private Boolean isOwner;

    @JsonProperty("creation_date")
    private Timestamp creationDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public GetSubscribeOptionsDTO getSubscribeOptions() {
        return subscribeOptions;
    }

    public void setSubscribeOptions(GetSubscribeOptionsDTO subscribeOptions) {
        this.subscribeOptions = subscribeOptions;
    }

    public Boolean getPublic() {
        return isPublic;
    }

    public void setPublic(Boolean aPublic) {
        isPublic = aPublic;
    }

    public String getOwnersUsername() {
        return ownersUsername;
    }

    public void setOwnersUsername(String ownersUsername) {
        this.ownersUsername = ownersUsername;
    }

    public Boolean getIsOwner() {
        return isOwner;
    }

    public void setIsOwner(Boolean owner) {
        isOwner = owner;
    }

    public Timestamp getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Timestamp creationDate) {
        this.creationDate = creationDate;
    }
}
