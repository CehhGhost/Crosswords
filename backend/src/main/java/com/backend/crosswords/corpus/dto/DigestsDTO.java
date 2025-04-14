package com.backend.crosswords.corpus.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "DTO to wrap a list of digests")
public class DigestsDTO {
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

    @Override
    public String toString() {
        return "DigestsDTO{" +
                "digests=" + digests +
                '}';
    }
}
