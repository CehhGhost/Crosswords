package com.backend.crosswords.admin.controllers;

import com.auth0.jwt.exceptions.TokenExpiredException;
import com.backend.crosswords.admin.dto.JwtDTO;
import com.backend.crosswords.admin.dto.LoginUserDTO;
import com.backend.crosswords.admin.dto.RefreshUserDTO;
import com.backend.crosswords.admin.dto.RegisterUserDTO;
import com.backend.crosswords.admin.services.UserService;
import com.backend.crosswords.config.JWTUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/users")
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

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterUserDTO registerUserDTO, HttpServletRequest request, HttpServletResponse response) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        String userAgent = request.getHeader("User-Agent");
        List<String> jwt;
        try {
            jwt =  userService.registerUser(registerUserDTO, ipAddress, userAgent);;
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Incorrect credentials!");
        }
        return setCookies(response, jwt);
    }

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
        return setCookies(response, jwt);
    }

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
        return setCookies(response, jwt);
    }
}
