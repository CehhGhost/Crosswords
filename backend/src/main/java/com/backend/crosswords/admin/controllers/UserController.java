package com.backend.crosswords.admin.controllers;

import com.auth0.jwt.exceptions.TokenExpiredException;
import com.backend.crosswords.admin.dto.AuthorityDTO;
import com.backend.crosswords.admin.dto.LoginUserDTO;
import com.backend.crosswords.admin.dto.RegisterUserDTO;
import com.backend.crosswords.admin.models.CrosswordUserDetails;
import com.backend.crosswords.admin.services.UserService;
import com.backend.crosswords.corpus.dto.AnnotationsIdDTO;
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
        System.out.println("User agent: " + userAgent);
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
            @ApiResponse(responseCode = "200", description = "You successfully registered the user by it's credentials"),
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
    // @PostMapping("")
    // TODO добавить удаление пользователя, учтя тот факт, что перед удалением необходимо очистить связанные с ним данные
    /*@DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUserById(@PathVariable Long id) {
        userService.deleteUserById(id);
        return ResponseEntity.ok(HttpStatus.OK);
    }*/
}
