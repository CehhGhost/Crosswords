package com.backend.crosswords.admin.services;

import com.auth0.jwt.exceptions.TokenExpiredException;
import com.backend.crosswords.admin.models.RefreshToken;
import com.backend.crosswords.admin.models.User;
import com.backend.crosswords.admin.repositories.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {
    @Value("${jwt_refreshExpirationDate}")
    private Integer refreshExpirationMinutes;
    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }
    @Transactional
    public RefreshToken generateRefreshToken(String ipAddress, String userAgent, User user) {
        RefreshToken refreshToken = new RefreshToken(UUID.randomUUID().toString(), ipAddress, userAgent, user,
                ZonedDateTime.now().plusMinutes(refreshExpirationMinutes).toInstant());
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken refreshUser(String token, String ipAddress, String userAgent) {
        var oldToken = refreshTokenRepository.findByToken(token).orElseThrow();
        if (!Objects.equals(oldToken.getIp(), ipAddress) || !Objects.equals(oldToken.getUserAgent(), userAgent)) {
            System.out.println("Подозрительная активность!!!!");
            // TODO добавить обработчик подозрительной активности
        }
        if (oldToken.getExpiryDate().isBefore(ZonedDateTime.now().toInstant())) {
            refreshTokenRepository.delete(oldToken);
            throw new TokenExpiredException("Your refresh token is expired!", oldToken.getExpiryDate());
        }
        User user = oldToken.getUser();
        refreshTokenRepository.deleteById(oldToken.getId());
        refreshTokenRepository.flush();
        return this.generateRefreshToken(ipAddress, userAgent, user);
    }

    public Optional<RefreshToken> checkExistingRefreshToken(String ipAddress, String userAgent, User user) {
        return refreshTokenRepository.findByUserAndUserAgentAndIp(user, userAgent, ipAddress);
    }

    public void deleteRefreshForUser(User user, String ipAddress, String userAgent) {
        var refresh = refreshTokenRepository.findByUserAndUserAgentAndIp(user, userAgent, ipAddress).orElseThrow(() -> new NoSuchElementException("There is no such authorized users!"));
        refreshTokenRepository.delete(refresh);
    }

    public void deleteAllRefreshesForUser(User user) {
        var refreshes = refreshTokenRepository.findAllByUser(user);
        refreshTokenRepository.deleteAll(refreshes);
    }
}
