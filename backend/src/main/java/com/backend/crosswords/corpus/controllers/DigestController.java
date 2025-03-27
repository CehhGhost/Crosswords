package com.backend.crosswords.corpus.controllers;

import com.backend.crosswords.admin.models.CrosswordUserDetails;
import com.backend.crosswords.admin.models.User;
import com.backend.crosswords.corpus.dto.CreateDigestSubscriptionDTO;
import com.backend.crosswords.corpus.dto.DocDTO;
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
    // TODO сделать scheduler для создания ядер дайджеста

}
