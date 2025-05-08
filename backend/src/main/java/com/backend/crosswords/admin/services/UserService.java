package com.backend.crosswords.admin.services;

import com.backend.crosswords.admin.dto.*;
import com.backend.crosswords.admin.enums.RoleEnum;
import com.backend.crosswords.admin.models.CrosswordUserDetails;
import com.backend.crosswords.admin.models.RefreshToken;
import com.backend.crosswords.admin.models.User;
import com.backend.crosswords.admin.repositories.UserRepository;
import com.backend.crosswords.config.JWTUtil;
import com.backend.crosswords.corpus.models.Package;
import com.backend.crosswords.corpus.models.DocRating;
import com.backend.crosswords.corpus.services.PackageService;
import org.apache.http.ConnectionClosedException;
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
    private final VerifyCodeService verifyCodeService;

    public UserService(ModelMapper modelMapper, UserRepository userRepository, AuthenticationManager authenticationManager, JWTUtil jwtUtil, RefreshTokenService refreshTokenService, PasswordEncoder passwordEncoder, PackageService packageService, VerifyCodeService verifyCodeService) {
        this.modelMapper = modelMapper;
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.packageService = packageService;
        this.verifyCodeService = verifyCodeService;
    }

    public List<String> registerUser(RegisterUserDTO registerUserDTO, String ipAddress, String userAgent) {
        var usernameFromEmail = registerUserDTO.getEmail().split("@")[0];
        var user = modelMapper.map(registerUserDTO, User.class);
        user.setUsername(usernameFromEmail);
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

        user.setSubscribable(true);
        user.setPersonalMobileNotifications(false);
        user.setPersonalSendToMail(false);
        user.setMobileNotifications(false);
        user.setSendToMail(false);

        user = userRepository.save(user);
        packageService.createPackage(Package.favouritesName, user);
        return this.loginUser(new LoginUserDTO(registerUserDTO.getEmail(), registerUserDTO.getPassword()), ipAddress, userAgent);
    }

    public List<String> loginUser(LoginUserDTO loginUserDTO, String ipAddress, String userAgent) {
        UsernamePasswordAuthenticationToken authInputToken = new UsernamePasswordAuthenticationToken(loginUserDTO.getUsername(), loginUserDTO.getPassword());
        authenticationManager.authenticate(authInputToken);
        var accessToken = jwtUtil.generateAccessToken(loginUserDTO.getUsername());
        var user = userRepository.findByUsernameOrEmail(loginUserDTO.getUsername(), loginUserDTO.getUsername()).orElseThrow();
        RefreshToken refreshToken;
        var optionalRefreshToken = refreshTokenService.checkExistingRefreshToken(ipAddress, userAgent, user);
        if (optionalRefreshToken == null) {
            refreshToken = refreshTokenService.generateRefreshToken(ipAddress, userAgent, user);
        } else {
            refreshToken = optionalRefreshToken;
        }
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

    public PersonalDigestSubscriptionSettingsDTO checkUsersSubscriptionSettings(String username) {
        var user = userRepository.findByUsernameOrEmail(username, username).orElseThrow(() -> new NoSuchElementException("There is no users with such username/email!"));
        if (!user.getSubscribable()) {
            throw new IllegalArgumentException("This user can't be added into any subscriptions!");
        }
        return new PersonalDigestSubscriptionSettingsDTO(user.getSendToMail(), user.getMobileNotifications(), user.getPersonalSendToMail(), user.getPersonalMobileNotifications(), user.getSubscribable());
    }

    public void setUsersSubscriptionSettings(User user, Boolean sendToMail, Boolean mobileNotifications, Boolean personalSendToMail, Boolean personalMobileNotifications, Boolean subscribable) {
        user.setSendToMail(sendToMail);
        user.setMobileNotifications(mobileNotifications);
        user.setPersonalSendToMail(personalSendToMail);
        user.setPersonalMobileNotifications(personalMobileNotifications);
        user.setSubscribable(subscribable);
        userRepository.save(user);
    }

    public void logoutUser(User user, String ipAddress, String userAgent) {
        refreshTokenService.deleteRefreshForUser(user, ipAddress, userAgent);
    }

    public void logoutUserFull(User user) {
        refreshTokenService.deleteAllRefreshesForUser(user);
    }

    public void changeUsersPassword(User user, String oldPassword, String newPassword) throws IllegalAccessException, IllegalArgumentException {
        String actualPassword = user.getPassword();
        if (!passwordEncoder.matches(oldPassword, actualPassword)) {
            throw new IllegalAccessException("Incorrect password!");
        }
        if (oldPassword.equals(newPassword)) {
            throw new IllegalArgumentException("New password cant be the same as an old password");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void changeUsersEmail(User user, String newEmail) {
        if (newEmail == null || newEmail.equals("")) {
            throw new IllegalArgumentException("Incorrect email!");
        }
        if (user.getEmail().equals(newEmail)) {
            throw new IllegalArgumentException("A new email can't be the same as an old one!");
        }
        if (userRepository.existsUserByEmail(newEmail)) {
            throw new IllegalArgumentException("A user with such email already exists!");
        }
        user.setEmail(newEmail);
        user.setUsername(newEmail.split("@")[0]);
        user.setVerified(false);
        userRepository.save(user);
    }
    public GetPersonalInfoDTO getUsersPersonalInfoAndTransformIntoDTO(User user) {
        return new GetPersonalInfoDTO(user.getName(), user.getSurname(), user.getUsername(), user.getEmail(), user.getSendToMail(), user.getMobileNotifications(), user.getPersonalSendToMail(), user.getPersonalMobileNotifications(), user.getSubscribable());
    }

    public CheckUsersVerificationDTO checkUsersEmailVerification(User user) {
        return new CheckUsersVerificationDTO(!user.getVerified());
    }

    public VerificatingEmailDTO sendVerificationCodeAndReturnVerificatingEmailInDTO(User user) throws ConnectionClosedException {
        user = userRepository.findById(user.getId()).orElseThrow();
        var email = user.getEmail();
        verifyCodeService.sendEmailWithVerificationCode(user);
        return new VerificatingEmailDTO(email);
    }

    public void checkVerificationCodeForUser(String checkingCode, User user) throws NoSuchElementException, IllegalArgumentException {
        user = userRepository.findById(user.getId()).orElseThrow();
        if (verifyCodeService.checkVerificationCodeForUser(checkingCode, user)) {
            user.setVerified(true);
            userRepository.save(user);
        }
    }
}
