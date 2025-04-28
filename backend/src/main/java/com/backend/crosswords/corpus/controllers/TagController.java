package com.backend.crosswords.corpus.controllers;

import com.backend.crosswords.corpus.dto.CreateTagDTO;
import com.backend.crosswords.corpus.dto.FoldersDTO;
import com.backend.crosswords.corpus.services.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/tags")
@Tag(name = "Tag controller", description = "Controller for all operations with tags")
public class TagController {
    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }
    @Operation(summary = "Create a tag", description = "This is a simple creating tag endpoint", deprecated = true)
    @PostMapping("/create")
    public ResponseEntity<?> createTag(@RequestBody CreateTagDTO createTagDTO) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("This method is deprecated!");
        /*try {
            tagService.createTag(createTagDTO);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
        return ResponseEntity.ok(HttpStatus.OK);*/
    }
    @Operation(summary = "Delete a tag by id", description = "This is a very unsafe end point, as it leads to the deletion of all related information.", deprecated = true)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTagById(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("This method is deprecated!");
        /*try {
            tagService.deleteTagById(id);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
        return ResponseEntity.ok(HttpStatus.OK);*/
    }
}
