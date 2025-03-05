package com.backend.crosswords.admin.services;

import com.backend.crosswords.admin.dto.LoginUserDTO;
import com.backend.crosswords.admin.dto.RegisterUserDTO;
import com.backend.crosswords.admin.models.RefreshToken;
import com.backend.crosswords.admin.models.User;
import com.backend.crosswords.admin.repositories.UserRepository;
import com.backend.crosswords.config.JWTUtil;
import com.backend.crosswords.corpus.models.DocMeta;
import com.backend.crosswords.corpus.models.Rating;
import com.backend.crosswords.corpus.models.Tag;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {
    private final ModelMapper modelMapper;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    public UserService(ModelMapper modelMapper, UserRepository userRepository, AuthenticationManager authenticationManager, JWTUtil jwtUtil, RefreshTokenService refreshTokenService, PasswordEncoder passwordEncoder) {
        this.modelMapper = modelMapper;
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
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
        userRepository.save(user);
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
    public void removeRating(Rating rating) {
        var user = rating.getUser();
        user.getRatings().remove(rating);
        userRepository.save(user);
    }

    public User loadUserById(Long id) {
        return userRepository.findById(id).orElseThrow();
    }

    public void addDocToFavourites(DocMeta docMeta, User user) {
        user = this.loadUserById(user.getId()); // подгружаем пользователя из БД, чтобы избежать ленивой загрузки
        user.getFavouriteDocs().add(docMeta);
        userRepository.save(user);
    }

    public void removeDocFromFavourites(DocMeta docMeta, User user) {
        user = userRepository.findById(user.getId()).orElseThrow(); // подгружаем пользователя из БД, чтобы избежать ленивой загрузки
        user.getFavouriteDocs().remove(docMeta);
        userRepository.save(user);
    }

    public void removeDocFromFavouritesForAllUsers(DocMeta docMeta) {
        var users = userRepository.findAllByFavouriteDocsContaining(docMeta);
        for (var user : users) {
            user.getFavouriteDocs().remove(docMeta);
            userRepository.save(user);
        }
    }

    public Boolean checkDocInFavourites(User user, DocMeta docMeta) {
        user = userRepository.findById(user.getId()).orElseThrow();
        return user.getFavouriteDocs() != null && user.getFavouriteDocs().contains(docMeta);
    }
}
