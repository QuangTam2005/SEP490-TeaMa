package com.asc.studycompanion.integration;

import com.asc.studycompanion.common.exception.GlobalExceptionHandler;
import com.asc.studycompanion.module.ai.entity.AIEvaluation;
import com.asc.studycompanion.module.ai.entity.AIUsageLog;
import com.asc.studycompanion.module.ai.entity.EvaluationJob;
import com.asc.studycompanion.module.ai.entity.EvaluationJobStatus;
import com.asc.studycompanion.module.ai.entity.KnowledgeCache;
import com.asc.studycompanion.module.ai.repository.AIEvaluationRepository;
import com.asc.studycompanion.module.ai.repository.AIUsageLogRepository;
import com.asc.studycompanion.module.ai.repository.EvaluationJobRepository;
import com.asc.studycompanion.module.ai.repository.KnowledgeCacheRepository;
import com.asc.studycompanion.module.guardian.controller.GuardianController;
import com.asc.studycompanion.module.guardian.service.GuardianService;
import com.asc.studycompanion.module.auth.controller.AuthController;
import com.asc.studycompanion.module.auth.controller.LinkController;
import com.asc.studycompanion.module.auth.controller.StudentProfileController;
import com.asc.studycompanion.module.auth.dto.UserResponse;
import com.asc.studycompanion.module.auth.entity.GuardianLink;
import com.asc.studycompanion.module.auth.entity.GuardianLinkStatus;
import com.asc.studycompanion.module.auth.entity.StudentProfile;
import com.asc.studycompanion.module.auth.entity.User;
import com.asc.studycompanion.module.auth.mapper.UserMapper;
import com.asc.studycompanion.module.auth.repository.GuardianLinkRepository;
import com.asc.studycompanion.module.auth.repository.StudentProfileRepository;
import com.asc.studycompanion.module.auth.repository.UserRepository;
import com.asc.studycompanion.module.auth.service.AuthService;
import com.asc.studycompanion.module.auth.service.LinkService;
import com.asc.studycompanion.module.auth.service.StudyAccessGuard;
import com.asc.studycompanion.module.study.controller.StudyPlanController;
import com.asc.studycompanion.module.study.controller.StudySubmissionController;
import com.asc.studycompanion.module.study.controller.StudyTaskController;
import com.asc.studycompanion.module.study.entity.StudyPlan;
import com.asc.studycompanion.module.study.entity.StudySubmission;
import com.asc.studycompanion.module.study.entity.StudyTask;
import com.asc.studycompanion.module.study.entity.StudyTaskType;
import com.asc.studycompanion.module.study.repository.StudyPlanRepository;
import com.asc.studycompanion.module.study.repository.StudySubmissionRepository;
import com.asc.studycompanion.module.study.repository.StudyTaskRepository;
import com.asc.studycompanion.module.study.service.StudyPlanService;
import com.asc.studycompanion.module.study.service.StudySubmissionService;
import com.asc.studycompanion.module.study.service.StudyTaskService;
import com.asc.studycompanion.module.timetable.controller.SubjectController;
import com.asc.studycompanion.module.timetable.controller.TimetableController;
import com.asc.studycompanion.module.timetable.entity.Subject;
import com.asc.studycompanion.module.timetable.entity.SubjectCode;
import com.asc.studycompanion.module.timetable.entity.Timetable;
import com.asc.studycompanion.module.timetable.entity.TimetableStatus;
import com.asc.studycompanion.module.timetable.repository.SubjectRepository;
import com.asc.studycompanion.module.timetable.repository.TimetableRepository;
import com.asc.studycompanion.module.timetable.service.TimetableService;
import com.asc.studycompanion.security.CustomUserDetailsService;
import com.asc.studycompanion.security.JwtAuthFilter;
import com.asc.studycompanion.security.JwtProperties;
import com.asc.studycompanion.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.Filter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * Stateful in-memory full-stack test harness wiring real Controllers, Services,
 * JwtTokenProvider, JwtAuthFilter, Bean Validation, and GlobalExceptionHandler.
 */
final class InMemoryStudyHarness {

    private static final String TEST_JWT_SECRET =
            "asc-study-companion-integration-test-jwt-secret-key-2026-minimum-32-bytes";

    final AtomicLong seq = new AtomicLong(100L);
    final List<User> users = new ArrayList<>();
    final List<StudentProfile> profiles = new ArrayList<>();
    final List<GuardianLink> links = new ArrayList<>();
    final List<Subject> subjects = new ArrayList<>();
    final List<Timetable> timetables = new ArrayList<>();
    final List<StudyPlan> plans = new ArrayList<>();
    final List<StudyTask> tasks = new ArrayList<>();
    final List<StudySubmission> submissions = new ArrayList<>();
    final List<EvaluationJob> jobs = new ArrayList<>();
    final List<AIEvaluation> evaluations = new ArrayList<>();
    final List<AIUsageLog> usageLogs = new ArrayList<>();
    final List<KnowledgeCache> knowledgeCaches = new ArrayList<>();

    final UserRepository userRepository = mock(UserRepository.class);
    final StudentProfileRepository studentProfileRepository = mock(StudentProfileRepository.class);
    final GuardianLinkRepository guardianLinkRepository = mock(GuardianLinkRepository.class);
    final SubjectRepository subjectRepository = mock(SubjectRepository.class);
    final TimetableRepository timetableRepository = mock(TimetableRepository.class);
    final StudyPlanRepository studyPlanRepository = mock(StudyPlanRepository.class);
    final StudyTaskRepository studyTaskRepository = mock(StudyTaskRepository.class);
    final StudySubmissionRepository studySubmissionRepository = mock(StudySubmissionRepository.class);
    final EvaluationJobRepository evaluationJobRepository = mock(EvaluationJobRepository.class);
    final AIEvaluationRepository aiEvaluationRepository = mock(AIEvaluationRepository.class);
    final AIUsageLogRepository aiUsageLogRepository = mock(AIUsageLogRepository.class);
    final KnowledgeCacheRepository knowledgeCacheRepository = mock(KnowledgeCacheRepository.class);

    final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    final JwtProperties jwtProperties;
    final JwtTokenProvider jwtTokenProvider;
    final CustomUserDetailsService customUserDetailsService;
    final JwtAuthFilter jwtAuthFilter;

    final StudyAccessGuard studyAccessGuard;
    final AuthService authService;
    final LinkService linkService;
    final TimetableService timetableService;
    final StudyPlanService studyPlanService;
    final StudyTaskService studyTaskService;
    final StudySubmissionService studySubmissionService;
    final GuardianService guardianService;
    final ObjectMapper objectMapper;
    final MockMvc mockMvc;

    InMemoryStudyHarness(Clock fixedClock, Object... extraControllers) {
        seedSubjects();
        bindRepositories();

        jwtProperties = new JwtProperties();
        jwtProperties.setSecret(TEST_JWT_SECRET);
        jwtProperties.setExpirationMs(86_400_000L);

        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
        customUserDetailsService = new CustomUserDetailsService(userRepository);
        jwtAuthFilter = new JwtAuthFilter(jwtTokenProvider, customUserDetailsService);

        UserMapper userMapper = (user, grade, linkedStudentName) -> new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                grade,
                linkedStudentName,
                user.getCreatedAt()
        );

        studyAccessGuard = new StudyAccessGuard(
                userRepository,
                studentProfileRepository,
                guardianLinkRepository
        );
        authService = new AuthService(
                userRepository,
                studentProfileRepository,
                guardianLinkRepository,
                userMapper,
                passwordEncoder,
                jwtTokenProvider,
                jwtProperties
        );
        linkService = new LinkService(
                userRepository,
                studentProfileRepository,
                guardianLinkRepository
        );
        timetableService = new TimetableService(
                studyAccessGuard,
                timetableRepository,
                subjectRepository,
                userRepository
        );
        studyPlanService = new StudyPlanService(
                studyAccessGuard,
                studyPlanRepository,
                timetableRepository,
                studySubmissionRepository,
                aiEvaluationRepository
        );
        if (fixedClock != null) {
            studyPlanService.setClock(fixedClock);
        }
        studyTaskService = new StudyTaskService(
                studyAccessGuard,
                studyTaskRepository,
                studySubmissionRepository,
                aiEvaluationRepository,
                evaluationJobRepository
        );
        studySubmissionService = new StudySubmissionService(
                studyAccessGuard,
                studyTaskRepository,
                studySubmissionRepository,
                evaluationJobRepository,
                aiEvaluationRepository,
                studyTaskService
        );
        guardianService = new GuardianService(
                studyAccessGuard,
                studyPlanRepository,
                studySubmissionRepository,
                aiEvaluationRepository,
                evaluationJobRepository,
                studyTaskService
        );

        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        List<Object> controllers = new ArrayList<>(List.of(
                new AuthController(authService),
                new StudentProfileController(authService),
                new LinkController(linkService),
                new SubjectController(subjectRepository),
                new TimetableController(timetableService),
                new StudyPlanController(studyPlanService),
                new StudyTaskController(studyTaskService, studySubmissionService),
                new StudySubmissionController(studySubmissionService),
                new GuardianController(guardianService)
        ));
        controllers.addAll(List.of(extraControllers));

        Filter clearContextFilter = (req, res, chain) -> {
            SecurityContextHolder.clearContext();
            try {
                chain.doFilter(req, res);
            } finally {
                SecurityContextHolder.clearContext();
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controllers.toArray())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .addFilters(clearContextFilter, jwtAuthFilter)
                .build();
    }

    private void seedSubjects() {
        subjects.addAll(List.of(
                Subject.builder().id(1L).code(SubjectCode.MATH).name("Toán").isActive(true).build(),
                Subject.builder().id(2L).code(SubjectCode.LIT).name("Ngữ văn").isActive(true).build(),
                Subject.builder().id(3L).code(SubjectCode.ENG).name("Tiếng Anh").isActive(true).build(),
                Subject.builder().id(4L).code(SubjectCode.SCI).name("Khoa học tự nhiên").isActive(true).build(),
                Subject.builder().id(5L).code(SubjectCode.HISGEO).name("Lịch sử & Địa lí").isActive(true).build()
        ));
    }

    private void bindRepositories() {
        // Users
        lenient().when(userRepository.existsByEmailIgnoreCase(anyString())).thenAnswer(inv -> {
            String email = inv.getArgument(0);
            return users.stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(email));
        });
        lenient().when(userRepository.findByEmailIgnoreCase(anyString())).thenAnswer(inv -> {
            String email = inv.getArgument(0);
            return users.stream().filter(u -> u.getEmail().equalsIgnoreCase(email)).findFirst();
        });
        lenient().when(userRepository.findById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return users.stream().filter(u -> Objects.equals(u.getId(), id)).findFirst();
        });
        lenient().when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> persistUser(inv.getArgument(0)));
        lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> persistUser(inv.getArgument(0)));

        // Student Profiles
        lenient().when(studentProfileRepository.findByUserId(anyLong())).thenAnswer(inv -> {
            Long userId = inv.getArgument(0);
            return profiles.stream()
                    .filter(p -> p.getUser() != null && Objects.equals(p.getUser().getId(), userId))
                    .findFirst();
        });
        lenient().when(studentProfileRepository.saveAndFlush(any(StudentProfile.class))).thenAnswer(inv -> persistProfile(inv.getArgument(0)));
        lenient().when(studentProfileRepository.save(any(StudentProfile.class))).thenAnswer(inv -> persistProfile(inv.getArgument(0)));

        // Guardian Links
        lenient().when(guardianLinkRepository.findByLinkCodeIgnoreCase(anyString())).thenAnswer(inv -> {
            String code = inv.getArgument(0);
            return links.stream().filter(l -> l.getLinkCode().equalsIgnoreCase(code)).findFirst();
        });
        lenient().when(guardianLinkRepository.findFirstByGuardianUserIdAndStatusOrderByLinkedAtDesc(anyLong(), any()))
                .thenAnswer(inv -> {
                    Long gId = inv.getArgument(0);
                    GuardianLinkStatus status = inv.getArgument(1);
                    return links.stream()
                            .filter(l -> l.getGuardianUser() != null
                                    && Objects.equals(l.getGuardianUser().getId(), gId)
                                    && l.getStatus() == status)
                            .max(Comparator.comparing(GuardianLink::getLinkedAt, Comparator.nullsFirst(Comparator.naturalOrder())));
                });
        lenient().when(guardianLinkRepository.findFirstByStudentIdOrderByCreatedAtDesc(anyLong())).thenAnswer(inv -> {
            Long studentId = inv.getArgument(0);
            return links.stream()
                    .filter(l -> Objects.equals(l.getStudent().getId(), studentId))
                    .max(Comparator.comparing(GuardianLink::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder())));
        });
        lenient().when(guardianLinkRepository.findAllByStudentIdAndStatus(anyLong(), any())).thenAnswer(inv -> {
            Long studentId = inv.getArgument(0);
            GuardianLinkStatus status = inv.getArgument(1);
            return links.stream()
                    .filter(l -> Objects.equals(l.getStudent().getId(), studentId) && l.getStatus() == status)
                    .toList();
        });
        lenient().when(guardianLinkRepository.existsByGuardianUserIdAndStudentIdAndStatus(anyLong(), anyLong(), any()))
                .thenAnswer(inv -> {
                    Long gId = inv.getArgument(0);
                    Long sId = inv.getArgument(1);
                    GuardianLinkStatus status = inv.getArgument(2);
                    return links.stream().anyMatch(l -> l.getGuardianUser() != null
                            && Objects.equals(l.getGuardianUser().getId(), gId)
                            && Objects.equals(l.getStudent().getId(), sId)
                            && l.getStatus() == status);
                });
        lenient().when(guardianLinkRepository.save(any(GuardianLink.class))).thenAnswer(inv -> persistLink(inv.getArgument(0)));
        lenient().when(guardianLinkRepository.saveAll(any())).thenAnswer(inv -> {
            Iterable<GuardianLink> items = inv.getArgument(0);
            List<GuardianLink> saved = new ArrayList<>();
            items.forEach(item -> saved.add(persistLink(item)));
            return saved;
        });

        // Subjects
        lenient().when(subjectRepository.findAllByIsActiveTrueOrderByIdAsc()).thenAnswer(inv -> List.copyOf(subjects));
        lenient().when(subjectRepository.findByCode(any(SubjectCode.class))).thenAnswer(inv -> {
            SubjectCode code = inv.getArgument(0);
            return subjects.stream().filter(s -> s.getCode() == code).findFirst();
        });

        // Timetables
        lenient().when(timetableRepository.findFirstByStudentIdAndStatusOrderByVersionDesc(anyLong(), any()))
                .thenAnswer(inv -> {
                    Long sId = inv.getArgument(0);
                    TimetableStatus status = inv.getArgument(1);
                    return timetables.stream()
                            .filter(t -> Objects.equals(t.getStudent().getId(), sId) && t.getStatus() == status)
                            .max(Comparator.comparing(Timetable::getVersion));
                });
        lenient().when(timetableRepository.findTopByStudentIdOrderByVersionDesc(anyLong())).thenAnswer(inv -> {
            Long sId = inv.getArgument(0);
            return timetables.stream()
                    .filter(t -> Objects.equals(t.getStudent().getId(), sId))
                    .max(Comparator.comparing(Timetable::getVersion));
        });
        lenient().when(timetableRepository.findAllByStudentIdAndStatus(anyLong(), any())).thenAnswer(inv -> {
            Long sId = inv.getArgument(0);
            TimetableStatus status = inv.getArgument(1);
            return timetables.stream()
                    .filter(t -> Objects.equals(t.getStudent().getId(), sId) && t.getStatus() == status)
                    .toList();
        });
        lenient().when(timetableRepository.save(any(Timetable.class))).thenAnswer(inv -> persistTimetable(inv.getArgument(0)));
        lenient().when(timetableRepository.saveAll(any())).thenAnswer(inv -> {
            Iterable<Timetable> items = inv.getArgument(0);
            List<Timetable> saved = new ArrayList<>();
            items.forEach(item -> saved.add(persistTimetable(item)));
            return saved;
        });

        // StudyPlans
        lenient().when(studyPlanRepository.findByStudentIdAndTargetDate(anyLong(), any(LocalDate.class)))
                .thenAnswer(inv -> {
                    Long sId = inv.getArgument(0);
                    LocalDate date = inv.getArgument(1);
                    return plans.stream()
                            .filter(p -> Objects.equals(p.getStudent().getId(), sId)
                                    && Objects.equals(p.getTargetDate(), date))
                            .findFirst();
                });
        lenient().when(studyPlanRepository.saveAndFlush(any(StudyPlan.class))).thenAnswer(inv -> persistPlan(inv.getArgument(0)));
        lenient().when(studyPlanRepository.save(any(StudyPlan.class))).thenAnswer(inv -> persistPlan(inv.getArgument(0)));

        // StudyTasks
        lenient().when(studyTaskRepository.findById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return tasks.stream().filter(t -> Objects.equals(t.getId(), id)).findFirst();
        });
        lenient().when(studyTaskRepository.findByPlanIdAndSubjectIdAndTaskType(anyLong(), anyLong(), any(StudyTaskType.class)))
                .thenAnswer(inv -> {
                    Long planId = inv.getArgument(0);
                    Long subjectId = inv.getArgument(1);
                    StudyTaskType type = inv.getArgument(2);
                    return tasks.stream()
                            .filter(t -> Objects.equals(t.getPlan().getId(), planId)
                                    && Objects.equals(t.getSubject().getId(), subjectId)
                                    && t.getTaskType() == type)
                            .findFirst();
                });
        lenient().when(studyTaskRepository.save(any(StudyTask.class))).thenAnswer(inv -> persistTask(inv.getArgument(0)));

        // StudySubmissions
        lenient().when(studySubmissionRepository.findAllByTaskIdOrderByVersionDesc(anyLong())).thenAnswer(inv -> {
            Long taskId = inv.getArgument(0);
            return submissions.stream()
                    .filter(s -> Objects.equals(s.getTask().getId(), taskId))
                    .sorted(Comparator.comparing(StudySubmission::getVersion).reversed())
                    .toList();
        });
        lenient().when(studySubmissionRepository.findFirstByTaskIdAndIsCurrentTrue(anyLong())).thenAnswer(inv -> {
            Long taskId = inv.getArgument(0);
            return submissions.stream()
                    .filter(s -> Objects.equals(s.getTask().getId(), taskId) && Boolean.TRUE.equals(s.getIsCurrent()))
                    .findFirst();
        });
        lenient().when(studySubmissionRepository.findByIdempotencyKey(anyString())).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            return submissions.stream().filter(s -> key.equals(s.getIdempotencyKey())).findFirst();
        });
        lenient().when(studySubmissionRepository.findById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return submissions.stream().filter(s -> Objects.equals(s.getId(), id)).findFirst();
        });
        lenient().when(studySubmissionRepository.existsByTaskId(anyLong())).thenAnswer(inv -> {
            Long taskId = inv.getArgument(0);
            return submissions.stream().anyMatch(s -> Objects.equals(s.getTask().getId(), taskId));
        });
        lenient().when(studySubmissionRepository.countByTaskId(anyLong())).thenAnswer(inv -> {
            Long taskId = inv.getArgument(0);
            return (int) submissions.stream().filter(s -> Objects.equals(s.getTask().getId(), taskId)).count();
        });
        lenient().when(studySubmissionRepository.saveAndFlush(any(StudySubmission.class))).thenAnswer(inv -> persistSubmission(inv.getArgument(0)));
        lenient().when(studySubmissionRepository.save(any(StudySubmission.class))).thenAnswer(inv -> persistSubmission(inv.getArgument(0)));
        lenient().when(studySubmissionRepository.saveAll(any())).thenAnswer(inv -> {
            Iterable<StudySubmission> items = inv.getArgument(0);
            List<StudySubmission> saved = new ArrayList<>();
            items.forEach(item -> saved.add(persistSubmission(item)));
            return saved;
        });

        // EvaluationJobs
        lenient().when(evaluationJobRepository.findById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return jobs.stream().filter(j -> Objects.equals(j.getId(), id)).findFirst();
        });
        lenient().when(evaluationJobRepository.findBySubmissionId(anyLong())).thenAnswer(inv -> {
            Long subId = inv.getArgument(0);
            return jobs.stream().filter(j -> Objects.equals(j.getSubmissionId(), subId)).findFirst();
        });
        lenient().when(evaluationJobRepository.findBySubmissionIdIn(anyCollection())).thenAnswer(inv -> {
            Collection<Long> ids = inv.getArgument(0);
            return jobs.stream().filter(j -> ids.contains(j.getSubmissionId())).toList();
        });
        lenient().when(evaluationJobRepository.findTop5ByStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
                any(EvaluationJobStatus.class), any(Instant.class)))
                .thenAnswer(inv -> {
                    EvaluationJobStatus status = inv.getArgument(0);
                    Instant cutoff = inv.getArgument(1);
                    return jobs.stream()
                            .filter(j -> j.getStatus() == status && !j.getNextRetryAt().isAfter(cutoff))
                            .limit(5)
                            .toList();
                });
        lenient().when(evaluationJobRepository.save(any(EvaluationJob.class))).thenAnswer(inv -> persistJob(inv.getArgument(0)));

        // AIEvaluations
        lenient().when(aiEvaluationRepository.findBySubmissionId(anyLong())).thenAnswer(inv -> {
            Long subId = inv.getArgument(0);
            return evaluations.stream().filter(e -> Objects.equals(e.getSubmissionId(), subId)).findFirst();
        });
        lenient().when(aiEvaluationRepository.findBySubmissionIdIn(anyCollection())).thenAnswer(inv -> {
            Collection<Long> ids = inv.getArgument(0);
            return evaluations.stream().filter(e -> ids.contains(e.getSubmissionId())).toList();
        });
        lenient().when(aiEvaluationRepository.save(any(AIEvaluation.class))).thenAnswer(inv -> persistEvaluation(inv.getArgument(0)));

        // AIUsageLogs
        lenient().when(aiUsageLogRepository.findAllByJobIdOrderByCreatedAtDesc(anyLong())).thenAnswer(inv -> {
            Long jobId = inv.getArgument(0);
            return usageLogs.stream()
                    .filter(u -> Objects.equals(u.getJobId(), jobId))
                    .sorted(Comparator.comparing(AIUsageLog::getCreatedAt).reversed())
                    .toList();
        });
        lenient().when(aiUsageLogRepository.save(any(AIUsageLog.class))).thenAnswer(inv -> persistUsageLog(inv.getArgument(0)));

        // KnowledgeCache
        lenient().when(knowledgeCacheRepository.findByCacheKey(anyString())).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            return knowledgeCaches.stream().filter(c -> key.equals(c.getCacheKey())).findFirst();
        });
        lenient().when(knowledgeCacheRepository.save(any(KnowledgeCache.class))).thenAnswer(inv -> persistKnowledgeCache(inv.getArgument(0)));
    }

    private User persistUser(User user) {
        if (user.getId() == null) {
            user.setId(seq.incrementAndGet());
        }
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(Instant.now());
        }
        user.setUpdatedAt(Instant.now());
        users.removeIf(u -> Objects.equals(u.getId(), user.getId()));
        users.add(user);
        return user;
    }

    private StudentProfile persistProfile(StudentProfile profile) {
        if (profile.getId() == null) {
            profile.setId(seq.incrementAndGet());
        }
        if (profile.getCreatedAt() == null) {
            profile.setCreatedAt(Instant.now());
        }
        profile.setUpdatedAt(Instant.now());
        profiles.removeIf(p -> Objects.equals(p.getId(), profile.getId()));
        profiles.add(profile);
        return profile;
    }

    private GuardianLink persistLink(GuardianLink link) {
        if (link.getId() == null) {
            link.setId(seq.incrementAndGet());
        }
        if (link.getCreatedAt() == null) {
            link.setCreatedAt(Instant.now());
        }
        links.removeIf(l -> Objects.equals(l.getId(), link.getId()));
        links.add(link);
        return link;
    }

    private Timetable persistTimetable(Timetable timetable) {
        if (timetable.getId() == null) {
            timetable.setId(seq.incrementAndGet());
        }
        if (timetable.getCreatedAt() == null) {
            timetable.setCreatedAt(Instant.now());
        }
        timetable.setUpdatedAt(Instant.now());
        if (timetable.getEntries() != null) {
            timetable.getEntries().forEach(e -> {
                if (e.getId() == null) {
                    e.setId(seq.incrementAndGet());
                }
            });
        }
        timetables.removeIf(t -> Objects.equals(t.getId(), timetable.getId()));
        timetables.add(timetable);
        return timetable;
    }

    private StudyPlan persistPlan(StudyPlan plan) {
        if (plan.getId() == null) {
            plan.setId(seq.incrementAndGet());
        }
        if (plan.getCreatedAt() == null) {
            plan.setCreatedAt(Instant.now());
        }
        if (plan.getTasks() != null) {
            plan.getTasks().forEach(this::persistTask);
        }
        plans.removeIf(p -> Objects.equals(p.getId(), plan.getId()));
        plans.add(plan);
        return plan;
    }

    private StudyTask persistTask(StudyTask task) {
        if (task.getId() == null) {
            task.setId(seq.incrementAndGet());
        }
        if (task.getCreatedAt() == null) {
            task.setCreatedAt(Instant.now());
        }
        task.setUpdatedAt(Instant.now());
        tasks.removeIf(t -> Objects.equals(t.getId(), task.getId()));
        tasks.add(task);
        return task;
    }

    private StudySubmission persistSubmission(StudySubmission submission) {
        if (submission.getId() == null) {
            submission.setId(seq.incrementAndGet());
        }
        if (submission.getSubmittedAt() == null) {
            submission.setSubmittedAt(Instant.now());
        }
        submissions.removeIf(s -> Objects.equals(s.getId(), submission.getId()));
        submissions.add(submission);
        return submission;
    }

    private EvaluationJob persistJob(EvaluationJob job) {
        if (job.getId() == null) {
            job.setId(seq.incrementAndGet());
        }
        if (job.getCreatedAt() == null) {
            job.setCreatedAt(Instant.now());
        }
        job.setUpdatedAt(Instant.now());
        jobs.removeIf(j -> Objects.equals(j.getId(), job.getId()));
        jobs.add(job);
        return job;
    }

    private AIEvaluation persistEvaluation(AIEvaluation evaluation) {
        if (evaluation.getId() == null) {
            evaluation.setId(seq.incrementAndGet());
        }
        if (evaluation.getEvaluatedAt() == null) {
            evaluation.setEvaluatedAt(Instant.now());
        }
        evaluations.removeIf(e -> Objects.equals(e.getId(), evaluation.getId()));
        evaluations.add(evaluation);
        return evaluation;
    }

    private AIUsageLog persistUsageLog(AIUsageLog log) {
        if (log.getId() == null) {
            log.setId(seq.incrementAndGet());
        }
        if (log.getCreatedAt() == null) {
            log.setCreatedAt(Instant.now());
        }
        usageLogs.removeIf(u -> Objects.equals(u.getId(), log.getId()));
        usageLogs.add(log);
        return log;
    }

    private KnowledgeCache persistKnowledgeCache(KnowledgeCache cache) {
        if (cache.getId() == null) {
            cache.setId(seq.incrementAndGet());
        }
        knowledgeCaches.removeIf(c -> Objects.equals(c.getId(), cache.getId()));
        knowledgeCaches.add(cache);
        return cache;
    }
}
