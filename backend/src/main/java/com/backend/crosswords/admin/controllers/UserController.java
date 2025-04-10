package com.backend.crosswords.admin.controllers;

import com.auth0.jwt.exceptions.TokenExpiredException;
import com.backend.crosswords.admin.dto.*;
import com.backend.crosswords.admin.models.CrosswordUserDetails;
import com.backend.crosswords.admin.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/users")
@Tag(name = "User controller", description = "Controller for all operations with users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
    }

    private ResponseEntity<?> setCookies(HttpServletResponse response, List<String> jwt) {
        var accessTokenCookie = new Cookie("access_token", jwt.get(0));
        accessTokenCookie.setPath("/");
        var refreshTokenCookie = new Cookie("refresh_token", jwt.get(1));
        refreshTokenCookie.setPath("/");
        response.addCookie(accessTokenCookie);
        response.addCookie(refreshTokenCookie);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @Operation(
            summary = "Register a user by it's credentials",
            description = "This endpoint lets you register a user by it's credentials and other important information"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You successfully registered the user by it's credentials"),
            @ApiResponse(responseCode = "400", description = "The user with such email or username have been already registered, or the credentials are incorrect")
    })
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterUserDTO registerUserDTO, HttpServletRequest request, HttpServletResponse response) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        String userAgent = request.getHeader("User-Agent");
        List<String> jwt;
        try {
            jwt =  userService.registerUser(registerUserDTO, ipAddress, userAgent);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Incorrect credentials!");
        }
        return this.setCookies(response, jwt);
    }

    @Operation(
            summary = "Login the user by it's credentials",
            description = "This endpoint lets you login the user by it's credentials, email and username can be both used as user's login"

    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You successfully login the user by it's credentials"),
            @ApiResponse(responseCode = "400", description = "The credentials are incorrect")
    })
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginUserDTO loginUserDTO, HttpServletRequest request, HttpServletResponse response) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        String userAgent = request.getHeader("User-Agent");
        List<String> jwt;
        try {
            jwt =  userService.loginUser(loginUserDTO, ipAddress, userAgent);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Incorrect credentials!");
        }
        return this.setCookies(response, jwt);
    }

    @Operation(
            summary = "Refresh the user by it's refresh token",
            description = "This endpoint lets you manually refresh the user by it's refresh token",
            deprecated = true
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You successfully refreshed the user by it's refresh token"),
            @ApiResponse(responseCode = "400", description = "The credentials are incorrect")
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshUser(HttpServletRequest request, HttpServletResponse response) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        String userAgent = request.getHeader("User-Agent");
        List<String> jwt;
        try {
            String token = null;
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if ("refresh_token".equals(cookie.getName())) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }
            if (token == null || token.isBlank()) {
                throw new BadCredentialsException("Incorrect credentials!");
            }
            jwt =  userService.refreshUser(token, ipAddress, userAgent);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No such refresh token!");
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Incorrect credentials!");
        } catch (TokenExpiredException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage() + " At: " + e.getExpiredOn());
        }
        return this.setCookies(response, jwt);
    }
    @Operation(
            summary = "Check the user's authorities",
            description = "This endpoint lets you check the user's authorities, and get them in lowercase"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You successfully checked the user's authorities", content = @Content(schema = @Schema(implementation = AuthorityDTO.class))),
            @ApiResponse(responseCode = "401", description = "The user is not authenticated")
    })
    @GetMapping("/check_auth")
    public ResponseEntity<?> checkUsersAuthorities() {
        List<String> authoritiesNames = userService.getAuthoritiesNamesByUser();
        return ResponseEntity.ok(new AuthorityDTO(authoritiesNames));
    }
    @Operation(
            summary = "Get user's email",
            description = "This endpoint lets you get user's email"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You successfully get user's email", content = @Content(schema = @Schema(implementation = EmailDTO.class))),
            @ApiResponse(responseCode = "401", description = "You are trying to get user's email while not authenticated")
    })
    @GetMapping("/get_email")
    public ResponseEntity<?> getUsersEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CrosswordUserDetails crosswordUserDetails = (CrosswordUserDetails) authentication.getPrincipal();
        String email = userService.getUsersEmail(crosswordUserDetails.getUser());
        return ResponseEntity.ok(new EmailDTO(email));
    }
    @Operation(
            summary = "Check user's subscription settings",
            description = "This endpoint lets you check user's subscription settings by it's username/email"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You successfully checked user's subscription settings", content = @Content(schema = @Schema(implementation = PersonalDigestSubscriptionSettingsDTO.class))),
            @ApiResponse(responseCode = "404", description = "There is no users with such username"),
            @ApiResponse(responseCode = "403", description = "This user can't be added into any subscriptions!"),
    })
    @PostMapping("/subscription_settings/check")
    public ResponseEntity<?> checkUsersSubscriptionSettings(@RequestBody UsernameDTO usernameDTO) {
        PersonalDigestSubscriptionSettingsDTO subscriptionSettingsDTO = null;
        try {
            subscriptionSettingsDTO = userService.checkUsersSubscriptionSettings(usernameDTO.getUsername());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
        return ResponseEntity.ok(subscriptionSettingsDTO);
    }
    @Operation(
            summary = "Set user's subscription settings",
            description = "This endpoint lets you set user's subscription settings"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You successfully set user's subscription settings"),
            @ApiResponse(responseCode = "401", description = "You are trying to set user's subscription settings while not authenticated"),
    })
    @PutMapping("/subscription_settings/set")
    public ResponseEntity<?> setUsersSubscriptionSettings(@RequestBody PersonalDigestSubscriptionSettingsDTO subscriptionSettingsDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CrosswordUserDetails crosswordUserDetails = (CrosswordUserDetails) authentication.getPrincipal();
        userService.setUsersSubscriptionSettings(crosswordUserDetails.getUser(), subscriptionSettingsDTO.getSendToMail(), subscriptionSettingsDTO.getMobileNotifications(), subscriptionSettingsDTO.getPersonalSendToMail(), subscriptionSettingsDTO.getPersonalMobileNotifications(), subscriptionSettingsDTO.getSubscribable());
        return ResponseEntity.ok(HttpStatus.OK);
    }
    @Operation(
            summary = "Logout user",
            description = "This endpoint lets you logout user and erase cookies for him, remember that all tries of requests will automatically erase cookies, if the refresh token from cookies doesn't exist"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You successfully logout user"),
            @ApiResponse(responseCode = "401", description = "You are trying to logout user while not authenticated"),
            @ApiResponse(responseCode = "404", description = "You are trying to logout user that hasn't been authenticated yet, this error will occur really rare and exist for just in case")
    })
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CrosswordUserDetails crosswordUserDetails = (CrosswordUserDetails) authentication.getPrincipal();
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        String userAgent = request.getHeader("User-Agent");
        try {
            userService.logoutUser(crosswordUserDetails.getUser(), ipAddress, userAgent);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
        return this.setCookies(response, List.of("", ""));
    }
    @Operation(
            summary = "Fully logout user",
            description = "This endpoint lets you fully logout user, deleting all his refreshes and erase cookies for him, remember that all tries of requests will automatically erase cookies, if the refresh token from cookies doesn't exist"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "You successfully fully logout user"),
            @ApiResponse(responseCode = "401", description = "You are trying to fully logout user while not authenticated")
    })
    @PostMapping("/logout/full")
    public ResponseEntity<?> logoutUserFull(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CrosswordUserDetails crosswordUserDetails = (CrosswordUserDetails) authentication.getPrincipal();
        userService.logoutUserFull(crosswordUserDetails.getUser());
        return this.setCookies(response, List.of("", ""));
    }
    // TODO добавить удаление пользователя, учтя тот факт, что перед удалением необходимо очистить связанные с ним данные
    /*@DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUserById(@PathVariable Long id) {
        userService.deleteUserById(id);
        return ResponseEntity.ok(HttpStatus.OK);
    }*/
}
