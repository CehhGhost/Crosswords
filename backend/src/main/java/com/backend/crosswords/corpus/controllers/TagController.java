package com.backend.crosswords.corpus.controllers;

import com.backend.crosswords.corpus.dto.CreateTagDTO;
import com.backend.crosswords.corpus.services.TagService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/tags")
public class TagController {
    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }
    @PostMapping("/create")
    public ResponseEntity<?> createTag(@RequestBody CreateTagDTO createTagDTO) {
        try {
            tagService.createTag(createTagDTO);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
        return ResponseEntity.ok(HttpStatus.OK);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTagById(@PathVariable Long id) {
        try {
            tagService.deleteTagById(id);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
        return ResponseEntity.ok(HttpStatus.OK);
    }
}
