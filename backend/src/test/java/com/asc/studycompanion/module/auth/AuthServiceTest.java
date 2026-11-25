package com.asc.studycompanion.module.auth;

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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Mock
    private GuardianLinkRepository guardianLinkRepository;

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
        jwtProperties.setSecret("study_companion_unit_test_secret_key_32_bytes");
        jwtProperties.setExpirationMs(3_600_000L);
        jwtTokenProvider = new JwtTokenProvider(jwtProperties);

        authService = new AuthService(
                userRepository,
                studentProfileRepository,
                guardianLinkRepository,
                userMapper,
                passwordEncoder,
                jwtTokenProvider,
                jwtProperties
        );
    }

    @Test
    void registerStudentProvisionsProfileAndHashesPassword() {
        RegisterRequest request = new RegisterRequest(
                "  HocSinh.Lop6@Gmail.com ",
                "secret123",
                "  Nguyen Minh Khoi  ",
                UserRole.STUDENT
        );

        when(userRepository.existsByEmailIgnoreCase("hocsinh.lop6@gmail.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(10L);
            u.setCreatedAt(Instant.parse("2026-11-25T02:00:00Z"));
            return u;
        });
        when(studentProfileRepository.saveAndFlush(any(StudentProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        User saved = userCaptor.getValue();

        assertEquals("hocsinh.lop6@gmail.com", saved.getEmail());
        assertEquals("Nguyen Minh Khoi", saved.getFullName());
        assertEquals(UserRole.STUDENT, saved.getRole());
        assertNotEquals("secret123", saved.getPasswordHash());
        assertTrue(passwordEncoder.matches("secret123", saved.getPasswordHash()));

        ArgumentCaptor<StudentProfile> profileCaptor = ArgumentCaptor.forClass(StudentProfile.class);
        verify(studentProfileRepository).saveAndFlush(profileCaptor.capture());
        assertEquals(6, profileCaptor.getValue().getGrade());
        assertEquals("Asia/Ho_Chi_Minh", profileCaptor.getValue().getTimezone());

        assertNotNull(response.accessToken());
        assertEquals(6, response.user().grade());
        assertNull(response.user().linkedStudentName());
    }

    @Test
    void registerGuardianDoesNotCreateStudentProfile() {
        RegisterRequest request = new RegisterRequest(
                "phuhuynh@yahoo.com",
                "secret123",
                "Nguyen Thu Hang",
                UserRole.GUARDIAN
        );

        when(userRepository.existsByEmailIgnoreCase("phuhuynh@yahoo.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(11L);
            u.setCreatedAt(Instant.parse("2026-11-25T02:00:00Z"));
            return u;
        });

        AuthResponse response = authService.register(request);

        verify(studentProfileRepository, never()).saveAndFlush(any());
        assertEquals(UserRole.GUARDIAN, response.user().role());
        assertNull(response.user().grade());
        assertNull(response.user().linkedStudentName());
    }

    @Test
    void registerThrowsConflictWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("dup@outlook.com", "secret123", "User", UserRole.STUDENT);
        when(userRepository.existsByEmailIgnoreCase("dup@outlook.com")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> authService.register(request));
        assertEquals(ErrorCode.EMAIL_ALREADY_EXISTS, ex.getErrorCode());
    }

    @Test
    void registerTranslatesConcurrentDataIntegrityViolationToConflict() {
        RegisterRequest request = new RegisterRequest("race@yahoo.com", "secret123", "User", UserRole.GUARDIAN);
        when(userRepository.existsByEmailIgnoreCase("race@yahoo.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        AppException ex = assertThrows(AppException.class, () -> authService.register(request));
        assertEquals(ErrorCode.EMAIL_ALREADY_EXISTS, ex.getErrorCode());
    }

    @Test
    void loginRejectsDisabledAccountAndReturnsLinkedStudentForGuardian() {
        User disabled = User.builder()
                .id(4L)
                .email("disabled@gmail.com")
                .passwordHash(passwordEncoder.encode("secret123"))
                .role(UserRole.STUDENT)
                .status(UserStatus.DISABLED)
                .build();
        when(userRepository.findByEmailIgnoreCase("disabled@gmail.com")).thenReturn(Optional.of(disabled));

        AppException disabledEx = assertThrows(
                AppException.class,
                () -> authService.login(new LoginRequest("disabled@gmail.com", "secret123"))
        );
        assertEquals(ErrorCode.ACCESS_DENIED, disabledEx.getErrorCode());

        User guardian = User.builder()
                .id(5L)
                .email("phuhuynh@gmail.com")
                .passwordHash(passwordEncoder.encode("secret123"))
                .fullName("Nguyen Thu Hang")
                .role(UserRole.GUARDIAN)
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();
        StudentProfile student = StudentProfile.builder()
                .id(101L)
                .displayName("Nguyen Minh Khoi")
                .grade(6)
                .build();
        GuardianLink activeLink = GuardianLink.builder()
                .id(1L)
                .guardianUser(guardian)
                .student(student)
                .status(GuardianLinkStatus.ACTIVE)
                .build();

        when(userRepository.findByEmailIgnoreCase("phuhuynh@gmail.com")).thenReturn(Optional.of(guardian));
        when(guardianLinkRepository.findFirstByGuardianUserIdAndStatusOrderByLinkedAtDesc(5L, GuardianLinkStatus.ACTIVE))
                .thenReturn(Optional.of(activeLink));

        AuthResponse response = authService.login(new LoginRequest("  PHUHUYNH@gmail.com ", "secret123"));

        assertNotNull(response.accessToken());
        assertEquals(UserRole.GUARDIAN, response.user().role());
        assertEquals("Nguyen Minh Khoi", response.user().linkedStudentName());
    }

    @Test
    void getCurrentUserReturnsStudentGradeOrThrowsNotFound() {
        User student = User.builder()
                .id(8L)
                .email("me@gmail.com")
                .passwordHash("hashed")
                .fullName("Le Gia Bao")
                .role(UserRole.STUDENT)
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.parse("2026-11-25T02:00:00Z"))
                .build();
        StudentProfile profile = StudentProfile.builder()
                .id(20L)
                .user(student)
                .displayName("Le Gia Bao")
                .grade(6)
                .build();

        when(userRepository.findById(8L)).thenReturn(Optional.of(student));
        when(studentProfileRepository.findByUserId(8L)).thenReturn(Optional.of(profile));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        UserResponse me = authService.getCurrentUser(8L);
        assertEquals(8L, me.id());
        assertEquals(UserRole.STUDENT, me.role());
        assertEquals(6, me.grade());

        AppException ex = assertThrows(AppException.class, () -> authService.getCurrentUser(999L));
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }
}
