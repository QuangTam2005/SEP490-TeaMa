package com.asc.studycompanion.module.auth.service;

import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.module.auth.dto.AuthResponse;
import com.asc.studycompanion.module.auth.dto.LoginRequest;
import com.asc.studycompanion.module.auth.dto.RegisterRequest;
import com.asc.studycompanion.module.auth.dto.UserResponse;
import com.asc.studycompanion.module.auth.entity.GuardianLink;
import com.asc.studycompanion.module.auth.entity.GuardianLinkStatus;
import com.asc.studycompanion.module.auth.entity.StudentProfile;
import com.asc.studycompanion.module.auth.entity.User;
import com.asc.studycompanion.module.auth.entity.UserRole;
import com.asc.studycompanion.module.auth.entity.UserStatus;
import com.asc.studycompanion.module.auth.mapper.UserMapper;
import com.asc.studycompanion.module.auth.repository.GuardianLinkRepository;
import com.asc.studycompanion.module.auth.repository.StudentProfileRepository;
import com.asc.studycompanion.module.auth.repository.UserRepository;
import com.asc.studycompanion.security.JwtProperties;
import com.asc.studycompanion.security.JwtTokenProvider;
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
    private final StudentProfileRepository studentProfileRepository;
    private final GuardianLinkRepository guardianLinkRepository;
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
                .role(request.role())
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser;
        try {
            savedUser = userRepository.saveAndFlush(newUser);
        } catch (DataIntegrityViolationException ex) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        Integer grade = null;
        if (savedUser.getRole() == UserRole.STUDENT) {
            StudentProfile profile = StudentProfile.builder()
                    .user(savedUser)
                    .displayName(savedUser.getFullName())
                    .grade(StudentProfile.DEFAULT_GRADE)
                    .timezone(StudentProfile.DEFAULT_TIMEZONE)
                    .build();
            studentProfileRepository.saveAndFlush(profile);
            grade = profile.getGrade();
        }

        return buildAuthResponse(savedUser, grade, null);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        if (user.getStatus() == UserStatus.DISABLED) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        UserResponse userResponse = mapUserWithProfileAndLink(user);
        String accessToken = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(
                accessToken,
                BEARER_TOKEN_TYPE,
                jwtProperties.getExpirationMs(),
                userResponse
        );
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return mapUserWithProfileAndLink(user);
    }

    private UserResponse mapUserWithProfileAndLink(User user) {
        Integer grade = null;
        String linkedStudentName = null;

        if (user.getRole() == UserRole.STUDENT) {
            grade = studentProfileRepository.findByUserId(user.getId())
                    .map(StudentProfile::getGrade)
                    .orElse(StudentProfile.DEFAULT_GRADE);
        } else if (user.getRole() == UserRole.GUARDIAN) {
            linkedStudentName = guardianLinkRepository
                    .findFirstByGuardianUserIdAndStatusOrderByLinkedAtDesc(user.getId(), GuardianLinkStatus.ACTIVE)
                    .map(GuardianLink::getStudent)
                    .map(StudentProfile::getDisplayName)
                    .orElse(null);
        }

        return userMapper.toResponse(user, grade, linkedStudentName);
    }

    private AuthResponse buildAuthResponse(User user, Integer grade, String linkedStudentName) {
        String accessToken = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(
                accessToken,
                BEARER_TOKEN_TYPE,
                jwtProperties.getExpirationMs(),
                userMapper.toResponse(user, grade, linkedStudentName)
        );
    }
}
