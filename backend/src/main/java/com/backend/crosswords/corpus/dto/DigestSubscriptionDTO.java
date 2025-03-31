package com.backend.crosswords.corpus.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

// TODO доделать
public class DigestSubscriptionDTO {
    private Long id;

    @Schema(description = "Title of the digest", example = "Главное о спорте")
    private String title;

    @Schema(description = "Description of the digest", example = "самое важно о мире спорта")
    private String description;

    @ArraySchema(schema = @Schema(description = "List of sources for the digest", example = "Интерфакс"))
    private List<String> sources;

    @ArraySchema(schema = @Schema(description = "List of tags associated with the digest", example = "Спорт"))
    private List<String> tags;
}
