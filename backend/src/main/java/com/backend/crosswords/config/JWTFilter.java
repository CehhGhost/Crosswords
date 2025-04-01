package com.backend.crosswords.config;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.backend.crosswords.admin.models.CrosswordUserDetails;
import com.backend.crosswords.admin.models.RefreshToken;
import com.backend.crosswords.admin.services.CrosswordUserDetailsService;
import com.backend.crosswords.admin.services.RefreshTokenService;
import com.backend.crosswords.admin.services.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.NoSuchElementException;

@Component
public class JWTFilter extends OncePerRequestFilter {
    private final JWTUtil jwtUtil;
    private final CrosswordUserDetailsService crosswordUserDetailsService;
    private final RefreshTokenService refreshTokenService;

    public JWTFilter(JWTUtil jwtUtil, CrosswordUserDetailsService crosswordUserDetailsService, RefreshTokenService refreshTokenService) {
        this.jwtUtil = jwtUtil;
        this.crosswordUserDetailsService = crosswordUserDetailsService;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String accessToken = null;
        String oldRefreshToken = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    accessToken = cookie.getValue();
                    System.out.println("Old access token: " + accessToken);
                    if (oldRefreshToken != null) {
                        break;
                    }
                } else if ("refresh_token".equals(cookie.getName())) {
                    oldRefreshToken = cookie.getValue();
                    System.out.println("Old refresh token: " + oldRefreshToken);
                    if (accessToken != null) {
                        break;
                    }
                }
            }
        }

        if (accessToken != null && !accessToken.isBlank()) {
            try {
                this.validateJWTAndAuthenticate(accessToken, request);
            } catch (UsernameNotFoundException | JWTVerificationException exception) {
                refreshUser(oldRefreshToken, request, response, filterChain);
            } catch (IllegalAccessException e) {
                this.setCookies(response, "", "");
                AnonymousAuthenticationToken anonymousToken = new AnonymousAuthenticationToken(
                        "anonymous", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
                SecurityContextHolder.getContext().setAuthentication(anonymousToken);
            }
        } else {
            refreshUser(oldRefreshToken, request, response, filterChain);
        }
        filterChain.doFilter(request, response);
    }

    private void validateJWTAndAuthenticate(String accessToken, HttpServletRequest request) throws IllegalAccessException {
        String username = jwtUtil.validateTokenAndRetrieveClaim(accessToken);
        UserDetails userDetails = crosswordUserDetailsService.loadUserByUsername(username);

        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        String userAgent = request.getHeader("User-Agent");

        if (refreshTokenService.checkExistingRefreshToken(ipAddress, userAgent, ((CrosswordUserDetails)userDetails).getUser()).isEmpty()) {
            throw new IllegalAccessException("There is no such authorized users!");
        }

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(userDetails,
                        userDetails.getPassword(),
                        userDetails.getAuthorities());

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }
        System.out.println("New access token: " + accessToken);
    }

    private void refreshUser(String oldToken, HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (oldToken != null && !oldToken.isBlank()) {
            String ipAddress = request.getHeader("X-Forwarded-For");
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
            }
            String userAgent = request.getHeader("User-Agent");
            RefreshToken newRefreshToken;
            try {
                newRefreshToken = refreshTokenService.refreshUser(oldToken, ipAddress, userAgent);
            } catch (TokenExpiredException | NoSuchElementException e) {
                AnonymousAuthenticationToken anonymousToken = new AnonymousAuthenticationToken(
                        "anonymous", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
                SecurityContextHolder.getContext().setAuthentication(anonymousToken);
                return;
            }
            String newAccessToken = jwtUtil.generateAccessToken(newRefreshToken.getUser().getUsername());
            String username = jwtUtil.validateTokenAndRetrieveClaim(newAccessToken);
            UserDetails userDetails = crosswordUserDetailsService.loadUserByUsername(username);

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userDetails,
                            userDetails.getPassword(),
                            userDetails.getAuthorities());

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
            this.setCookies(response, newAccessToken, newRefreshToken.getToken());
            try {
                this.validateJWTAndAuthenticate(newAccessToken, request);
            } catch (IllegalAccessException e) {
                AnonymousAuthenticationToken anonymousToken = new AnonymousAuthenticationToken(
                        "anonymous", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
                SecurityContextHolder.getContext().setAuthentication(anonymousToken);
            }
            System.out.println("New access token: " + newAccessToken);
            System.out.println("New refresh token: " + newRefreshToken.getToken());
        } else {
            AnonymousAuthenticationToken anonymousToken = new AnonymousAuthenticationToken(
                    "anonymous", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
            SecurityContextHolder.getContext().setAuthentication(anonymousToken);
        }
    }
    private void setCookies(HttpServletResponse response, String newAccessToken, String newRefreshToken) {
        var accessTokenCookie = new Cookie("access_token", newAccessToken);
        accessTokenCookie.setHttpOnly(true);
        // accessTokenCookie.setSecure(true); // TODO Для HTTPS
        accessTokenCookie.setPath("/");
        var refreshTokenCookie = new Cookie("refresh_token", newRefreshToken);
        refreshTokenCookie.setHttpOnly(true);
        // refreshTokenCookie.setSecure(true); // TODO Для HTTPS
        refreshTokenCookie.setPath("/");
        response.addCookie(accessTokenCookie);
        response.addCookie(refreshTokenCookie);
    }
}
