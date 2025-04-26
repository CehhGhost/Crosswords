package com.backend.crosswords.corpus.controllers;

import com.backend.crosswords.admin.models.CrosswordUserDetails;
import com.backend.crosswords.admin.models.User;
import com.backend.crosswords.corpus.dto.*;
import com.backend.crosswords.corpus.models.Digest;
import com.backend.crosswords.corpus.services.DigestService;
import com.backend.crosswords.corpus.services.DigestSubscriptionService;
import com.backend.crosswords.corpus.services.PdfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/digests")
@Tag(name = "Digest controller", description = "Controller for all operations with digests")
public class DigestController {
    private final DigestService digestService;
    private final DigestSubscriptionService subscriptionService;
    private final PdfService pdfService;

    public DigestController(DigestService digestService, DigestSubscriptionService subscriptionService, PdfService pdfService) {
        this.digestService = digestService;
        this.subscriptionService = subscriptionService;
        this.pdfService = pdfService;
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
            return ResponseEntity.ok(digestService.getDigestByIdAndTransformIntoDTO(id, user));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
    @GetMapping("/{id}/pdf")
    public ResponseEntity<?> getDigestByIdAndConvertIntoPDF(@PathVariable String id) {
        User user;
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CrosswordUserDetails crosswordUserDetails = (CrosswordUserDetails) authentication.getPrincipal();
            user = crosswordUserDetails.getUser();
        } catch (ClassCastException e) {
            user = null;
        }
        try {
            var digestDTO = digestService.getDigestByIdAndTransformIntoDTO(id, user);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"documents_export.pdf\"")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                    .body(pdfService.generateDigestPdf(digestDTO));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("An error occurred while creating a pdf!");
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

    @Operation(summary = "Get digests by search", description = "This endpoint gets you all digests by your filtration's and by a certain search with title")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Searched digests", content = @Content(schema = @Schema(implementation = DigestsDTO.class))),
            @ApiResponse(responseCode = "401", description = "You are trying to get digests by search while unauthorized"),
            @ApiResponse(responseCode = "400", description = "Invalid date format. Expected: dd/MM/yyyy")
    })
    @GetMapping("/search")
    public ResponseEntity<?> getDigestsBySearch(
            @RequestParam(required = false, name = "search_body") String searchBody,
            @RequestParam(required = false, name = "date_from") String dateFrom,
            @RequestParam(required = false, name = "date_to") String dateTo,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) List<String> sources,
            @RequestParam(required = false, defaultValue = "false", name = "subscribe_only") Boolean subscribeOnly,
            @RequestParam(required = false, name = "page_number") Integer pageNumber,
            @RequestParam(required = false, name = "matches_per_page") Integer matchesPerPage) {
        User user;
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CrosswordUserDetails crosswordUserDetails = (CrosswordUserDetails) authentication.getPrincipal();
            user = crosswordUserDetails.getUser();
        } catch (ClassCastException e) {
            user = null;
        }
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
        Timestamp timestampFrom = null;
        Timestamp timestampTo = null;
        try {
            if (dateFrom != null) {
                Date parsedDateFrom = format.parse(dateFrom);
                timestampFrom = new Timestamp(parsedDateFrom.getTime());
            }

            if (dateTo != null) {
                Date parsedDateTo = format.parse(dateTo);
                timestampTo = new Timestamp(parsedDateTo.getTime());
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid date format. Expected: dd/MM/yyyy");
        }
        return ResponseEntity.ok(digestService.getDigestsBySearchAndTransformIntoDTO(searchBody, timestampFrom, timestampTo, tags, sources, subscribeOnly, user, pageNumber, matchesPerPage));
    }
    @Operation(summary = "Update a digest subscription's settings", description = "This endpoint lets you update a digest subscription's settings by digest itself, remember that after no followers remains the subscription is being deleted")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You successfully updated a digest subscription's settings"),
            @ApiResponse(responseCode = "401", description = "You are trying to updated a digest subscription's settings while not authenticated"),
            @ApiResponse(responseCode = "404", description = "There is no digests with such id, there is no such digest subscription or you are trying to edit private subscription while not its follower"),
    })
    @PutMapping("/{id}/subscription/settings")
    public ResponseEntity<?> updateDigestSubscriptionSettingsForUserByDigestId(@PathVariable String id, @RequestBody DigestSubscriptionSettingsDTO subscriptionSettingsDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CrosswordUserDetails crosswordUserDetails = (CrosswordUserDetails) authentication.getPrincipal();
        try {
            digestService.updateDigestSubscriptionSettingsForUserByDigestId(id, subscriptionSettingsDTO, crosswordUserDetails.getUser());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
        return ResponseEntity.ok(HttpStatus.OK);
    }
    @Operation(summary = "Get digest subscription by digest's id", description = "This endpoint lets you get digest subscription by digest's id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You successfully get digest subscription by digest's id", content = @Content(schema = @Schema(implementation = DigestSubscriptionDTO.class))),
            @ApiResponse(responseCode = "401", description = "You are trying to get digest subscription by digest's id while not authenticated"),
            @ApiResponse(responseCode = "404", description = "There is no digests or subscriptions with such id")
    })
    @GetMapping("/{id}/subscription")
    public ResponseEntity<?> getDigestSubscriptionByDigestId(@PathVariable String id) {
        Digest digest;
        try {
            digest = digestService.getDigestById(id);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
        DigestSubscriptionDTO subscription;
        try {
            subscription = subscriptionService.getDigestSubscriptionByIdAndTransformIntoDTO(digest.getSubscription().getId());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
        return ResponseEntity.ok(subscription);
    }
    @Operation(summary = "Get all users from the subscription by its digest", description = "This endpoint lets you get all users from the subscription by its digest")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You successfully get all users from the subscription by its digest", content = @Content(schema = @Schema(implementation = FollowersUsernamesDTO.class))),
            @ApiResponse(responseCode = "401", description = "You are trying to get all users from the subscription by its digest while not authenticated"),
            @ApiResponse(responseCode = "404", description = "There is no digests with such id"),
    })
    @GetMapping("/{id}/followers")

    public ResponseEntity<?> getAllDigestSubscriptionsUsersByDigestId(@PathVariable String id) {
        FollowersUsernamesDTO usersUsernames = new FollowersUsernamesDTO();
        try {
            usersUsernames.setFollowers(digestService.getAllDigestSubscriptionsUsersExceptOwner(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
        return ResponseEntity.ok(usersUsernames);
    }
    @Operation(summary = "Change digest subscription's owner by digest itself", description = "This endpoint lets you change digest subscription's owner by digest itself")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You successfully changed digest subscription's owner by digest itself"),
            @ApiResponse(responseCode = "401", description = "You are trying to change digest subscription's owner by digest itself while not authenticated"),
            @ApiResponse(responseCode = "400", description = "A user must be a follower of a digest subscription to become a new owner of it and he can't be its old owner!"),
            @ApiResponse(responseCode = "404", description = "There is no such digest subscriptions, such digests or such users that you are trying to give an ownership"),
            @ApiResponse(responseCode = "403", description = "You are trying to set a new digest subscription's owner by digest itself while not owning it")
    })
    @PatchMapping ("/{id}/change_owner")
    public ResponseEntity<?> changeDigestSubscriptionsOwner(@PathVariable String id, @RequestBody ChangeDigestSubscriptionsOwnerDTO changeDigestSubscriptionOwnerDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CrosswordUserDetails crosswordUserDetails = (CrosswordUserDetails) authentication.getPrincipal();
        try {
            digestService.changeDigestSubscriptionsOwnerByDigestId(crosswordUserDetails.getUser(), id, changeDigestSubscriptionOwnerDTO.getOwner());
        } catch (IllegalAccessException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
        return ResponseEntity.ok(HttpStatus.OK);
    }
    @GetMapping ("/ES")
    public ResponseEntity<?> findAll() {
        return ResponseEntity.ok(digestService.findAllES());
    }
}
