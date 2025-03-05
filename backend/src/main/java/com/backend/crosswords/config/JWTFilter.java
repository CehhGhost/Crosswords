package com.backend.crosswords.config;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.backend.crosswords.admin.services.CrosswordUserDetailsService;
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

@Component
public class JWTFilter extends OncePerRequestFilter {
    private final JWTUtil jwtUtil;
    private final CrosswordUserDetailsService crosswordUserDetailsService;

    public JWTFilter(JWTUtil jwtUtil, CrosswordUserDetailsService crosswordUserDetailsService) {
        this.jwtUtil = jwtUtil;
        this.crosswordUserDetailsService = crosswordUserDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String jwt = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    jwt = cookie.getValue();
                    break;
                }
            }
        }

        if (jwt != null && !jwt.isBlank()) {
            try {
                String username = jwtUtil.validateTokenAndRetrieveClaim(jwt);
                UserDetails userDetails = crosswordUserDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails,
                                userDetails.getPassword(),
                                userDetails.getAuthorities());

                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (UsernameNotFoundException | JWTVerificationException exception) {
                AnonymousAuthenticationToken anonymousToken = new AnonymousAuthenticationToken(
                        "anonymous", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
                SecurityContextHolder.getContext().setAuthentication(anonymousToken);
            }
        } else {
            AnonymousAuthenticationToken anonymousToken = new AnonymousAuthenticationToken(
                    "anonymous", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
            SecurityContextHolder.getContext().setAuthentication(anonymousToken);
        }

        filterChain.doFilter(request, response);
    }
}
