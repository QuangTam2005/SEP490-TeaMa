package com.fptu.sep490.module.auth.service;

import com.fptu.sep490.common.exception.AppException;
import com.fptu.sep490.common.exception.ErrorCode;
import com.fptu.sep490.module.auth.dto.AuthResponse;
import com.fptu.sep490.module.auth.dto.LoginRequest;
import com.fptu.sep490.module.auth.dto.RegisterRequest;
import com.fptu.sep490.module.auth.dto.UserResponse;
import com.fptu.sep490.module.auth.entity.User;
import com.fptu.sep490.module.auth.mapper.UserMapper;
import com.fptu.sep490.module.auth.repository.UserRepository;
import com.fptu.sep490.security.JwtProperties;
import com.fptu.sep490.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String BEARER_TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User newUser = User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .studentCode(request.studentCode())
                .phone(request.phone())
                .build();

        User savedUser;
        try {
            // Flush immediately so concurrent duplicate email inserts trigger DataIntegrityViolationException here
            savedUser = userRepository.saveAndFlush(newUser);
        } catch (DataIntegrityViolationException ex) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        return buildAuthResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return userMapper.toResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(
                accessToken,
                BEARER_TOKEN_TYPE,
                jwtProperties.getExpirationMs(),
                userMapper.toResponse(user)
        );
    }
}
