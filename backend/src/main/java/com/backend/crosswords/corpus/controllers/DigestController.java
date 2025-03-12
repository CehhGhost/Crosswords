package com.backend.crosswords.corpus.controllers;

import com.backend.crosswords.admin.models.CrosswordUserDetails;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/digests")
@Tag(name = "Digest controller", description = "Controller for all operations with digests")
public class DigestController {
    private final DigestService digestService;

    public DigestController(DigestService digestService) {
        this.digestService = digestService;
    }

    @Operation(summary = "Create Digest Subscription", description = "This endpoint lets you create a new subscription, owner will be extracted from auth, he also will be added into subscribers")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You successfully created a new subscription and subscribed users on it"),
            @ApiResponse(responseCode = "400", description = "At least one source has an incorrect name"),
            @ApiResponse(responseCode = "404", description = "At least one subscriber cant be found in the DB")
    })
    @PostMapping("/subscriptions/create")
    public ResponseEntity<?> createDigestSubscription(@RequestBody CreateDigestSubscriptionDTO createDigestSubscriptionDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CrosswordUserDetails crosswordUserDetails = (CrosswordUserDetails) authentication.getPrincipal();
        try {
            digestService.createDigestSubscription(crosswordUserDetails.getUser(), createDigestSubscriptionDTO);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
        return ResponseEntity.ok(HttpStatus.OK);
    }
}
