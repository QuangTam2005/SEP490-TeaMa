package com.asc.studycompanion.module.auth;

import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.module.auth.dto.AuthResponse;
import com.asc.studycompanion.module.auth.dto.LoginRequest;
import com.asc.studycompanion.module.auth.dto.RegisterRequest;
import com.asc.studycompanion.module.auth.dto.UserResponse;
import com.asc.studycompanion.module.auth.entity.User;
import com.asc.studycompanion.module.auth.mapper.UserMapper;
import com.asc.studycompanion.module.auth.repository.UserRepository;
import com.asc.studycompanion.module.auth.service.AuthService;
import com.asc.studycompanion.security.JwtProperties;
import com.asc.studycompanion.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private JwtProperties jwtProperties;
    private JwtTokenProvider jwtTokenProvider;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userMapper = Mappers.getMapper(UserMapper.class);
        passwordEncoder = new BCryptPasswordEncoder();
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("sep490_teama_unit_test_secret_key_32_bytes");
        jwtProperties.setExpirationMs(3_600_000L);
        jwtTokenProvider = new JwtTokenProvider(jwtProperties);

        authService = new AuthService(
                userRepository,
                userMapper,
                passwordEncoder,
                jwtTokenProvider,
                jwtProperties
        );
    }

    @Test
    void registerAcceptsAnyValidEmailNormalizesAndHashesPassword() {
        RegisterRequest request = new RegisterRequest(
                "  Student.Team@Gmail.com ",
                "secret123",
                "  Nguyen Van A  ",
                " SE180001 ",
                "   "
        );

        when(userRepository.existsByEmailIgnoreCase("student.team@gmail.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(10L);
            u.setCreatedAt(Instant.parse("2026-11-16T05:00:00Z"));
            return u;
        });

        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        User saved = userCaptor.getValue();

        assertEquals("student.team@gmail.com", saved.getEmail());
        assertEquals("Nguyen Van A", saved.getFullName());
        assertEquals("SE180001", saved.getStudentCode());
        assertEquals(null, saved.getPhone());
        assertNotEquals("secret123", saved.getPasswordHash());
        assertTrue(passwordEncoder.matches("secret123", saved.getPasswordHash()));

        assertNotNull(response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(3_600_000L, response.expiresInMs());
        assertEquals("student.team@gmail.com", response.user().email());
    }

    @Test
    void registerThrowsConflictWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("dup@outlook.com", "secret123", "User", null, null);
        when(userRepository.existsByEmailIgnoreCase("dup@outlook.com")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> authService.register(request));
        assertEquals(ErrorCode.EMAIL_ALREADY_EXISTS, ex.getErrorCode());
    }

    @Test
    void registerTranslatesConcurrentDataIntegrityViolationToConflict() {
        RegisterRequest request = new RegisterRequest("race@yahoo.com", "secret123", "User", null, null);
        when(userRepository.existsByEmailIgnoreCase("race@yahoo.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        AppException ex = assertThrows(AppException.class, () -> authService.register(request));
        assertEquals(ErrorCode.EMAIL_ALREADY_EXISTS, ex.getErrorCode());
    }

    @Test
    void loginSucceedsWithValidCredentials() {
        User existing = User.builder()
                .id(5L)
                .email("member@gmail.com")
                .passwordHash(passwordEncoder.encode("secret123"))
                .fullName("Tran Thi B")
                .createdAt(Instant.now())
                .build();
        when(userRepository.findByEmailIgnoreCase("member@gmail.com")).thenReturn(Optional.of(existing));

        AuthResponse response = authService.login(new LoginRequest("  MEMBER@gmail.com ", "secret123"));

        assertNotNull(response.accessToken());
        assertEquals(5L, response.user().id());
        assertEquals("member@gmail.com", response.user().email());
    }

    @Test
    void loginThrowsInvalidCredentialsWhenPasswordMismatch() {
        User existing = User.builder()
                .id(5L)
                .email("member@gmail.com")
                .passwordHash(passwordEncoder.encode("secret123"))
                .fullName("Tran Thi B")
                .build();
        when(userRepository.findByEmailIgnoreCase("member@gmail.com")).thenReturn(Optional.of(existing));

        AppException ex = assertThrows(
                AppException.class,
                () -> authService.login(new LoginRequest("member@gmail.com", "wrong-pass"))
        );
        assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
    }

    @Test
    void getCurrentUserReturnsMappedProfileOrThrowsNotFound() {
        User existing = User.builder()
                .id(8L)
                .email("me@gmail.com")
                .passwordHash("hashed")
                .fullName("Le Van C")
                .studentCode("SE180123")
                .createdAt(Instant.parse("2026-11-16T05:00:00Z"))
                .build();
        when(userRepository.findById(8L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        UserResponse profile = authService.getCurrentUser(8L);
        assertEquals(8L, profile.id());
        assertEquals("me@gmail.com", profile.email());
        assertEquals("SE180123", profile.studentCode());

        AppException ex = assertThrows(AppException.class, () -> authService.getCurrentUser(999L));
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }
}
