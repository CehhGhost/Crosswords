package com.backend.crosswords.corpus.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "DTO to wrap a list of digests")
public class DigestsDTO {
    private Boolean isAuthed;
    private List<DigestDTO> digests;

    public DigestsDTO() {
        digests = new ArrayList<>();
    }

    public List<DigestDTO> getDigests() {
        return digests;
    }

    public void setDigests(List<DigestDTO> digests) {
        this.digests = digests;
    }

    public Boolean getIsAuthed() {
        return isAuthed;
    }

    public void setIsAuthed(Boolean isAuthed) {
        this.isAuthed = isAuthed;
    }

    @Override
    public String toString() {
        return "DigestsDTO{" +
                "isAuthed=" + isAuthed +
                ", digests=" + digests +
                '}';
    }
}
