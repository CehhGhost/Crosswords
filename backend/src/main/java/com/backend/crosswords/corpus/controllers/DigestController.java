package com.backend.crosswords.corpus.controllers;

import com.backend.crosswords.admin.models.CrosswordUserDetails;
import com.backend.crosswords.admin.models.User;
import com.backend.crosswords.corpus.dto.*;
import com.backend.crosswords.corpus.services.DigestService;
import com.backend.crosswords.corpus.services.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/digests")
@Tag(name = "Digest controller", description = "Controller for all operations with digests")
public class DigestController {
    private final DigestService digestService;

    public DigestController(DigestService digestService) {
        this.digestService = digestService;
    }

    @Operation(summary = "Get the digest by id", description = "This endpoint lets get the digest by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You successfully get the digest by id", content = @Content(schema = @Schema(implementation = CertainDigestDTO.class))),
            @ApiResponse(responseCode = "401", description = "You are trying to get the digest by id while not authenticated"),
            @ApiResponse(responseCode = "404", description = "There is no digests with such id")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getDigestById(@PathVariable String id) {
        User user;
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CrosswordUserDetails crosswordUserDetails = (CrosswordUserDetails) authentication.getPrincipal();
            user = crosswordUserDetails.getUser();
        } catch (ClassCastException e) {
            user = null;
        }
        try {
            return ResponseEntity.ok(digestService.getDigestById(id, user));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
    @Operation(summary = "Get all digests", description = "This endpoint lets get all digests.\n P.S. this method is for mobile, also, Mathew, remind me to remind you about snake_case)))")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You successfully get all digests", content = @Content(schema = @Schema(implementation = DigestsDTO.class))),
            @ApiResponse(responseCode = "401", description = "You are trying to get all digests while not authenticated")
    })
    @GetMapping
    public ResponseEntity<?> getAllDigests() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CrosswordUserDetails crosswordUserDetails = (CrosswordUserDetails) authentication.getPrincipal();
        var digests = digestService.getAllDigests(crosswordUserDetails.getUser());
        return ResponseEntity.ok(digests);
    }
    @Operation(summary = "Rate digest core by id", description = "This endpoint lets you rate a digest core, parameters can be null or numbers from 1 to 5, user must be authenticated")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You successfully ratted digest core by id"),
            @ApiResponse(responseCode = "401", description = "You are trying to rate the digest core by id while unauthorized"),
            @ApiResponse(responseCode = "400", description = "Rating's arguments must be in range of 1 to 5"),
            @ApiResponse(responseCode = "404", description = "There is no digest cores with such id")
    })
    @PatchMapping("/{id}/rate")
    public ResponseEntity<?> rateDigestCoreByDigestId(@PathVariable String id, @RequestBody RateDigestCoreDTO rateDigestCoreDTO) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CrosswordUserDetails crosswordUserDetails = (CrosswordUserDetails) authentication.getPrincipal();
            digestService.rateDigestCoreByDigestId(id, rateDigestCoreDTO.getDigestCoreRating(), crosswordUserDetails.getUser());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
        return ResponseEntity.ok(HttpStatus.OK);
    }
}
