package com.backend.crosswords.corpus.controllers;

import com.backend.crosswords.admin.models.CrosswordUserDetails;
import com.backend.crosswords.corpus.dto.*;
import com.backend.crosswords.corpus.services.DigestSubscriptionService;
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
@RequestMapping("/subscriptions")
@Tag(name = "Subscription controller", description = "Controller for all operations with subscriptions")
public class DigestSubscriptionController {
    private final DigestSubscriptionService digestSubscriptionService;

    public DigestSubscriptionController(DigestSubscriptionService digestSubscriptionService) {
        this.digestSubscriptionService = digestSubscriptionService;
    }

    @Operation(summary = "Create a digest subscription", description = "This endpoint lets you create a new subscription, owner will be extracted from auth, he also will be added into subscribers")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You successfully created a new subscription and subscribed users on it"),
            @ApiResponse(responseCode = "401", description = "You are trying to create a digest subscription while not authenticated"),
            @ApiResponse(responseCode = "400", description = "At least one source has an incorrect name"),
            @ApiResponse(responseCode = "404", description = "At least one subscriber cant be found in the DB")
    })
    @PostMapping("/create")
    public ResponseEntity<?> createDigestSubscription(@RequestBody CreateDigestSubscriptionDTO createDigestSubscriptionDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CrosswordUserDetails crosswordUserDetails = (CrosswordUserDetails) authentication.getPrincipal();
        try {
            digestSubscriptionService.createDigestSubscription(crosswordUserDetails.getUser(), createDigestSubscriptionDTO);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
        return ResponseEntity.ok(HttpStatus.OK);
    }
    @Operation(summary = "Update a digest subscription", description = "This endpoint lets you update a digest subscription, owner must be included in the followers")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You successfully updated a digest subscription and managed subscribers on it"),
            @ApiResponse(responseCode = "401", description = "You are trying to updated a digest subscription while not authenticated"),
            @ApiResponse(responseCode = "400", description = "At least one source has an incorrect name"),
            @ApiResponse(responseCode = "404", description = "At least one subscriber, one digest subscription or one subscription's settings, cant be found in the DB"),
            @ApiResponse(responseCode = "403", description = "You are trying to updated a digest subscription while not owning it")
    })
    @PutMapping("/{id}/update")
    public ResponseEntity<?> updateDigestSubscription(@PathVariable Long id, @RequestBody UpdateDigestSubscriptionDTO updateDigestSubscriptionDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CrosswordUserDetails crosswordUserDetails = (CrosswordUserDetails) authentication.getPrincipal();
        try {
            digestSubscriptionService.updateDigestSubscription(crosswordUserDetails.getUser(), id, updateDigestSubscriptionDTO);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalAccessException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
        return ResponseEntity.ok(HttpStatus.OK);
    }
    @Operation(summary = "Update a digest subscription's settings", description = "This endpoint lets you update a digest subscription's settings")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You successfully updated a digest subscription's settings"),
            @ApiResponse(responseCode = "401", description = "You are trying to updated a digest subscription's settings while not authenticated"),
            @ApiResponse(responseCode = "404", description = "At least one digest subscription or one subscription's settings, cant be found in the DB"),
    })
    @PutMapping("/{id}/settings/update") // TODO тоже самое для дайджестов сделать
    public ResponseEntity<?> updateDigestSubscriptionSettingsForUser(@PathVariable Long id, @RequestBody DigestSubscriptionSettingsDTO subscriptionSettingsDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CrosswordUserDetails crosswordUserDetails = (CrosswordUserDetails) authentication.getPrincipal();
        try {
            digestSubscriptionService.updateDigestSubscriptionSettingsForUser(id, subscriptionSettingsDTO, crosswordUserDetails.getUser());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
        return ResponseEntity.ok(HttpStatus.OK);
    }
    @Operation(summary = "Get all users from the subscription", description = "This endpoint lets you get all users from the subscription")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You successfully get all users from the subscription", content = @Content(schema = @Schema(implementation = FollowersUsernamesDTO.class))),
            @ApiResponse(responseCode = "401", description = "You are trying to updated a digest subscription's settings while not authenticated"), // TODO а нужна ли вообще аутентификация здесь?
            @ApiResponse(responseCode = "404", description = "There is no subscriptions with such id"),
    })
    @GetMapping("/{id}/followers") // TODO тоже самое для дайджестов сделать
    public ResponseEntity<?> getAllDigestSubscriptionsUsers(@PathVariable Long id) {
        FollowersUsernamesDTO usersUsernames = new FollowersUsernamesDTO();
        try {
            usersUsernames.setFollowers(digestSubscriptionService.getAllDigestSubscriptionsUsers(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
        return ResponseEntity.ok(usersUsernames);
    }
    @Operation(summary = "Get all user's subscriptions", description = "This endpoint lets you get all user's subscriptions")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You successfully get all user's subscriptions", content = @Content(schema = @Schema(implementation = UsersDigestSubscriptionsDTO.class))),
            @ApiResponse(responseCode = "401", description = "You are trying to get all user's subscriptions while not authenticated"),
    })
    @GetMapping
    public ResponseEntity<?> getAllUsersDigestSubscriptions() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CrosswordUserDetails crosswordUserDetails = (CrosswordUserDetails) authentication.getPrincipal();
        UsersDigestSubscriptionsDTO usersDigestSubscriptionsDTO = digestSubscriptionService.getAllUsersDigestSubscriptionsAndTransformIntoUsersDigestSubscriptionsDTO(crosswordUserDetails.getUser());
        return ResponseEntity.ok(usersDigestSubscriptionsDTO);
    }
    @Operation(summary = "Get all available user's subscriptions", description = "This endpoint lets you get all available user's subscriptions (public or those on which he is subscribed)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You successfully get all available user's subscriptions", content = @Content(schema = @Schema(implementation = UsersDigestSubscriptionsDTO.class))),
            @ApiResponse(responseCode = "401", description = "You are trying to get all available user's subscriptions while not authenticated"),
    })
    @GetMapping("/available")
    public ResponseEntity<?> getAllUsersAvailableDigestSubscriptions() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CrosswordUserDetails crosswordUserDetails = (CrosswordUserDetails) authentication.getPrincipal();
        UsersDigestSubscriptionsDTO usersDigestSubscriptionsDTO = digestSubscriptionService.getAllUsersAvailableDigestSubscriptionsAndTransformIntoUsersDigestSubscriptionsDTO(crosswordUserDetails.getUser());
        return ResponseEntity.ok(usersDigestSubscriptionsDTO);
    }

    @Operation(summary = "Get digest subscription by id", description = "This endpoint lets you get digest subscription by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You successfully get digest subscription by id", content = @Content(schema = @Schema(implementation = DigestSubscriptionDTO.class))),
            @ApiResponse(responseCode = "401", description = "You are trying to get digest subscription by id while not authenticated"),
            @ApiResponse(responseCode = "404", description = "There is no subscriptions with such id")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getDigestSubscriptionById(@PathVariable Long id) {
        DigestSubscriptionDTO subscription;
        try {
            subscription = digestSubscriptionService.getDigestSubscriptionByIdAndTransformIntoDTO(id);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
        return ResponseEntity.ok(subscription);
    }
}
