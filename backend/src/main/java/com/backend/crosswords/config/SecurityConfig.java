package com.backend.crosswords.config;

import com.backend.crosswords.admin.services.CrosswordUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JWTFilter jwtFilter;
    private final CrosswordUserDetailsService crosswordUserDetailsService;
    private final UnauthorizedHandler unauthorizedHandler;

    public SecurityConfig(JWTFilter jwtFilter, CrosswordUserDetailsService crosswordUserDetailsService, UnauthorizedHandler unauthorizedHandler) {
        this.jwtFilter = jwtFilter;
        this.crosswordUserDetailsService = crosswordUserDetailsService;
        this.unauthorizedHandler = unauthorizedHandler;
    }
    @Bean
    public SecurityFilterChain applicationSecurity(HttpSecurity http) throws Exception {
        http
                .cors(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                // TODO понять в чем проблема строк выше и переделать их
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .exceptionHandling(h -> h.authenticationEntryPoint(unauthorizedHandler))
                .securityMatcher("/**")
                .authorizeHttpRequests(registry -> registry
                        .requestMatchers(HttpMethod.PATCH, "/documents/{id}/rate").authenticated()
                        .requestMatchers(HttpMethod.POST, "/documents/{id}/add_to_favourites").authenticated()
                        .requestMatchers(HttpMethod.POST, "/documents/{id}/remove_from_favourites").authenticated()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        var builder = http.getSharedObject(AuthenticationManagerBuilder.class);
        builder
                .userDetailsService(crosswordUserDetailsService)
                .passwordEncoder(passwordEncoder());
        return builder.build();
    }
}
