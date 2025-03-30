package com.backend.crosswords.admin.services;

import com.backend.crosswords.admin.dto.LoginUserDTO;
import com.backend.crosswords.admin.dto.RegisterUserDTO;
import com.backend.crosswords.admin.dto.SubscriptionSettingsDTO;
import com.backend.crosswords.admin.enums.RoleEnum;
import com.backend.crosswords.admin.models.CrosswordUserDetails;
import com.backend.crosswords.admin.models.RefreshToken;
import com.backend.crosswords.admin.models.User;
import com.backend.crosswords.admin.repositories.UserRepository;
import com.backend.crosswords.config.JWTUtil;
import com.backend.crosswords.corpus.models.Package;
import com.backend.crosswords.corpus.models.DocRating;
import com.backend.crosswords.corpus.services.PackageService;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
public class UserService {
    private final ModelMapper modelMapper;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final PackageService packageService;

    public UserService(ModelMapper modelMapper, UserRepository userRepository, AuthenticationManager authenticationManager, JWTUtil jwtUtil, RefreshTokenService refreshTokenService, PasswordEncoder passwordEncoder, PackageService packageService) {
        this.modelMapper = modelMapper;
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.packageService = packageService;
    }

    public List<String> registerUser(RegisterUserDTO registerUserDTO, String ipAddress, String userAgent) {
        var user = modelMapper.map(registerUserDTO, User.class);
        if (userRepository.existsUserByUsername(user.getUsername())) {
            throw new IllegalArgumentException("This username is already existed");
        }
        if (userRepository.existsUserByEmail(user.getEmail())) {
            throw new IllegalArgumentException("This email is already existed");
        }
        user.setPassword(passwordEncoder.encode(registerUserDTO.getPassword()));
        for (var role : RoleEnum.values()) {
            for (var username : role.getUsersWhiteList()) {
                if (Objects.equals(username, user.getUsername()) || Objects.equals(username, user.getEmail())) {
                    user.setRole(role);
                }
            }
        }
        user = userRepository.save(user);
        packageService.createPackage(Package.favouritesName, user);
        return this.loginUser(new LoginUserDTO(registerUserDTO.getUsername(), registerUserDTO.getPassword()), ipAddress, userAgent);
    }

    public List<String> loginUser(LoginUserDTO loginUserDTO, String ipAddress, String userAgent) {
        UsernamePasswordAuthenticationToken authInputToken = new UsernamePasswordAuthenticationToken(loginUserDTO.getUsername(), loginUserDTO.getPassword());
        authenticationManager.authenticate(authInputToken);
        var accessToken = jwtUtil.generateAccessToken(loginUserDTO.getUsername());
        var user = userRepository.findByUsernameOrEmail(loginUserDTO.getUsername(), loginUserDTO.getUsername()).orElseThrow();
        RefreshToken refreshToken;
        var optionalRefreshToken = refreshTokenService.checkExistingRefreshToken(ipAddress, userAgent, user);
        refreshToken = optionalRefreshToken.orElseGet(() -> refreshTokenService.generateRefreshToken(ipAddress, userAgent, user));
        List<String> jwt = new ArrayList<>();
        jwt.add(accessToken);
        jwt.add(refreshToken.getToken());
        System.out.println("Generated access token: " + accessToken);
        System.out.println("Generated refresh token: " + refreshToken.getToken());
        return jwt;
    }

    public List<String> refreshUser(String oldRefreshToken, String ipAddress, String userAgent) {
        var newRefreshToken = refreshTokenService.refreshUser(oldRefreshToken, ipAddress, userAgent);
        var accessToken = jwtUtil.generateAccessToken(newRefreshToken.getUser().getUsername());
        List<String> jwt = new ArrayList<>();
        jwt.add(accessToken);
        jwt.add(newRefreshToken.getToken());
        return jwt;
    }

    @Transactional
    public void removeRating(DocRating rating) {
        var user = rating.getUser();
        user.getRatings().remove(rating);
        userRepository.save(user);
    }

    public User loadUserById(Long id) {
        return userRepository.findById(id).orElseThrow();
    }

    public List<String> getAuthoritiesNamesByUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CrosswordUserDetails crosswordUserDetails = (CrosswordUserDetails) authentication.getPrincipal();
        List<String> authoritiesNames = new ArrayList<>();
        for (var authority : crosswordUserDetails.getAuthorities()) {
            authoritiesNames.add(authority.getAuthority().toLowerCase());
        }
        return authoritiesNames;
    }

    public void createDefaultUsers() {
        for (var role : RoleEnum.values()) {
            for (var username : role.getUsersWhiteList()) {
                var user = userRepository.findByUsernameOrEmail(username, username);
                if (user.isPresent() && !user.get().getRole().name().equals(role.name())) {
                    user.get().setRole(role);
                }
            }
        }
        // String name, String surname, String username, String password, RoleEnum role
        for (var username : RoleEnum.ROLE_ADMIN.getUsersWhiteList()) {
            if (userRepository.findByUsernameOrEmail(username, username).isEmpty()) {
                // TODO сделать пароль admin настраиваемым через параметры среды запуска или придумать другой более безопасный и удобный способ
                var user = userRepository.save(new User(username, username, username, passwordEncoder.encode(username), RoleEnum.ROLE_ADMIN));
                packageService.createPackage(Package.favouritesName, user);
            }
        }
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsernameOrEmail(username, username).orElseThrow(() -> new NoSuchElementException("There is no users with such username/email!"));
    }

    public String getUsersEmail(User user) {
        return user.getEmail();
    }

    public SubscriptionSettingsDTO checkUsersSubscriptionSettings(String username) {
        var user = userRepository.findByUsernameOrEmail(username, username).orElseThrow(() -> new NoSuchElementException("There is no users with such username/email!"));
        return new SubscriptionSettingsDTO(user.getSendToMail(), user.getMobileNotifications());
    }

    public void setUsersSubscriptionSettings(User user, Boolean sendToMail, Boolean mobileNotifications) {
        user.setSendToMail(sendToMail);
        user.setMobileNotifications(mobileNotifications);
        userRepository.save(user);
    }
}
