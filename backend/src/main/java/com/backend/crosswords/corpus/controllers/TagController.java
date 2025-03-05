package com.backend.crosswords.corpus.controllers;

import com.backend.crosswords.corpus.dto.CreateDocDTO;
import com.backend.crosswords.corpus.dto.CreateTagDTO;
import com.backend.crosswords.corpus.services.TagService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tags")
public class TagController {
    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }
    @PostMapping("/create")
    public ResponseEntity<HttpStatus> createDoc(@RequestBody CreateTagDTO createTagDTO) {
        return tagService.createTag(createTagDTO);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> deleteTagById(@PathVariable Long id) {
        return tagService.deleteTagById(id);
    }
}
