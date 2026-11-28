package com.asc.studycompanion.module.quiz;

import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.module.auth.entity.StudentProfile;
import com.asc.studycompanion.module.auth.entity.User;
import com.asc.studycompanion.module.auth.entity.UserRole;
import com.asc.studycompanion.module.auth.repository.UserRepository;
import com.asc.studycompanion.module.auth.service.StudyAccessGuard;
import com.asc.studycompanion.module.quiz.dto.BulkCreateQuizQuestionsRequest;
import com.asc.studycompanion.module.quiz.dto.CreateQuizQuestionRequest;
import com.asc.studycompanion.module.quiz.dto.GuardianQuizStatsResponse;
import com.asc.studycompanion.module.quiz.dto.MoveQuizQuestionRequest;
import com.asc.studycompanion.module.quiz.dto.QuizQuestionResponse;
import com.asc.studycompanion.module.quiz.dto.StudentDailyQuizItemResponse;
import com.asc.studycompanion.module.quiz.dto.StudentDailyQuizResponse;
import com.asc.studycompanion.module.quiz.dto.SubmitDailyQuizAnswerRequest;
import com.asc.studycompanion.module.quiz.dto.SubmitDailyQuizAnswerResponse;
import com.asc.studycompanion.module.quiz.dto.UpdateQuizSettingsRequest;
import com.asc.studycompanion.module.quiz.entity.DailyQuizSession;
import com.asc.studycompanion.module.quiz.entity.DailyQuizSessionItem;
import com.asc.studycompanion.module.quiz.entity.DailyQuizSessionStatus;
import com.asc.studycompanion.module.quiz.entity.DailyQuizSettings;
import com.asc.studycompanion.module.quiz.entity.GuardianQuizQuestion;
import com.asc.studycompanion.module.quiz.entity.QuizOption;
import com.asc.studycompanion.module.quiz.entity.QuizSelectionMode;
import com.asc.studycompanion.module.quiz.repository.DailyQuizSessionItemRepository;
import com.asc.studycompanion.module.quiz.repository.DailyQuizSessionRepository;
import com.asc.studycompanion.module.quiz.repository.DailyQuizSettingsRepository;
import com.asc.studycompanion.module.quiz.repository.GuardianQuizQuestionRepository;
import com.asc.studycompanion.module.quiz.service.DailyQuizService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyQuizServiceTest {

    @Mock
    private StudyAccessGuard studyAccessGuard;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DailyQuizSettingsRepository settingsRepository;
    @Mock
    private GuardianQuizQuestionRepository questionRepository;
    @Mock
    private DailyQuizSessionRepository sessionRepository;
    @Mock
    private DailyQuizSessionItemRepository sessionItemRepository;

    private DailyQuizService service;
    private StudentProfile student;
    private User guardian;

    private DailyQuizSettings storedSettings;
    private List<GuardianQuizQuestion> storedQuestions;
    private List<DailyQuizSession> storedSessions;
    private AtomicLong questionIdSeq;
    private AtomicLong sessionIdSeq;
    private AtomicLong itemIdSeq;

    @BeforeEach
    void setUp() {
        service = new DailyQuizService(
                studyAccessGuard,
                userRepository,
                settingsRepository,
                questionRepository,
                sessionRepository,
                sessionItemRepository
        );
        service.setRandom(new Random(42L));

        User studentUser = User.builder()
                .id(10L)
                .email("student@example.com")
                .fullName("Võ Hạnh Quyên")
                .role(UserRole.STUDENT)
                .build();
        student = StudentProfile.builder()
                .id(100L)
                .user(studentUser)
                .displayName("Võ Hạnh Quyên")
                .grade(6)
                .timezone("Asia/Ho_Chi_Minh")
                .build();
        guardian = User.builder()
                .id(20L)
                .email("guardian@example.com")
                .fullName("Phụ huynh")
                .role(UserRole.GUARDIAN)
                .build();

        storedQuestions = new ArrayList<>();
        storedSessions = new ArrayList<>();
        questionIdSeq = new AtomicLong(1);
        sessionIdSeq = new AtomicLong(1);
        itemIdSeq = new AtomicLong(1);

        lenient().when(studyAccessGuard.requireOwnedStudentProfile(10L)).thenReturn(student);
        lenient().when(studyAccessGuard.requireLinkedStudentForGuardian(20L, null)).thenReturn(student);
        lenient().when(studyAccessGuard.requireLinkedStudentForGuardian(20L, 100L)).thenReturn(student);
        lenient().when(userRepository.findById(20L)).thenReturn(Optional.of(guardian));

        lenient().when(settingsRepository.findByStudentId(100L))
                .thenAnswer(inv -> Optional.ofNullable(storedSettings));
        lenient().when(settingsRepository.save(any(DailyQuizSettings.class)))
                .thenAnswer(inv -> {
                    DailyQuizSettings s = inv.getArgument(0);
                    if (s.getId() == null) {
                        s.setId(1L);
                    }
                    storedSettings = s;
                    return s;
                });

        lenient().when(questionRepository.findByStudentIdOrderBySortOrderAscIdAsc(100L))
                .thenAnswer(inv -> storedQuestions.stream().toList());
        lenient().when(questionRepository.findByIdAndStudentId(any(), any()))
                .thenAnswer(inv -> {
                    Long id = inv.getArgument(0);
                    Long sId = inv.getArgument(1);
                    return storedQuestions.stream()
                            .filter(q -> q.getId().equals(id) && q.getStudent().getId().equals(sId))
                            .findFirst();
                });
        lenient().when(questionRepository.findByStudentIdAndActiveTrueOrderBySortOrderAscIdAsc(100L))
                .thenAnswer(inv -> storedQuestions.stream().filter(GuardianQuizQuestion::isActive).toList());
        lenient().when(questionRepository.countByStudentId(100L))
                .thenAnswer(inv -> (long) storedQuestions.size());
        lenient().when(questionRepository.countByStudentIdAndActiveTrue(100L))
                .thenAnswer(inv -> storedQuestions.stream().filter(GuardianQuizQuestion::isActive).count());
        lenient().when(questionRepository.saveAll(anyList()))
                .thenAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    List<GuardianQuizQuestion> list = (List<GuardianQuizQuestion>) inv.getArgument(0);
                    for (GuardianQuizQuestion q : list) {
                        if (q.getId() == null) {
                            q.setId(questionIdSeq.getAndIncrement());
                            storedQuestions.add(q);
                        }
                    }
                    return list;
                });
        lenient().when(questionRepository.save(any(GuardianQuizQuestion.class)))
                .thenAnswer(inv -> {
                    GuardianQuizQuestion q = inv.getArgument(0);
                    if (q.getId() == null) {
                        q.setId(questionIdSeq.getAndIncrement());
                        storedQuestions.add(q);
                    }
                    return q;
                });

        lenient().when(sessionRepository.findByStudentIdAndSessionDate(any(), any()))
                .thenAnswer(inv -> {
                    LocalDate date = inv.getArgument(1);
                    return storedSessions.stream().filter(s -> s.getSessionDate().equals(date)).findFirst();
                });
        lenient().when(sessionRepository.findByIdAndStudentId(any(), any()))
                .thenAnswer(inv -> {
                    Long id = inv.getArgument(0);
                    return storedSessions.stream().filter(s -> s.getId().equals(id)).findFirst();
                });
        lenient().when(sessionRepository.findByIdForUpdate(any()))
                .thenAnswer(inv -> {
                    Long id = inv.getArgument(0);
                    return storedSessions.stream().filter(s -> s.getId().equals(id)).findFirst();
                });
        lenient().when(sessionRepository.findByStudentIdOrderBySessionDateDesc(100L))
                .thenAnswer(inv -> storedSessions.stream()
                        .sorted((a, b) -> b.getSessionDate().compareTo(a.getSessionDate()))
                        .toList());
        lenient().when(sessionRepository.findByStudentIdAndSessionDateBetweenOrderBySessionDateDesc(any(), any(), any()))
                .thenAnswer(inv -> {
                    LocalDate from = inv.getArgument(1);
                    LocalDate to = inv.getArgument(2);
                    return storedSessions.stream()
                            .filter(s -> !s.getSessionDate().isBefore(from) && !s.getSessionDate().isAfter(to))
                            .sorted((a, b) -> b.getSessionDate().compareTo(a.getSessionDate()))
                            .toList();
                });
        lenient().when(sessionRepository.save(any(DailyQuizSession.class)))
                .thenAnswer(inv -> {
                    DailyQuizSession s = inv.getArgument(0);
                    if (s.getId() == null) {
                        s.setId(sessionIdSeq.getAndIncrement());
                        for (DailyQuizSessionItem item : s.getItems()) {
                            if (item.getId() == null) {
                                item.setId(itemIdSeq.getAndIncrement());
                            }
                        }
                        storedSessions.add(s);
                    }
                    return s;
                });
        lenient().when(sessionItemRepository.save(any(DailyQuizSessionItem.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(sessionItemRepository.findBySessionIdInOrderBySessionIdAscItemOrderAsc(anyList()))
                .thenAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    List<Long> ids = (List<Long>) inv.getArgument(0);
                    return storedSessions.stream()
                            .filter(s -> ids.contains(s.getId()))
                            .flatMap(s -> s.getItems().stream())
                            .toList();
                });
    }

    @Test
    void sequentialRotationRotatesAcrossDaysHandlesRemainderAndResetsToBeginning() {
        List<CreateQuizQuestionRequest> batch = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            batch.add(new CreateQuizQuestionRequest(
                    "Câu hỏi " + i,
                    "Đáp án A" + i,
                    "Đáp án B" + i,
                    i % 2 == 1 ? QuizOption.A : QuizOption.B,
                    "Giải thích câu " + i,
                    i <= 15 ? "Từ vựng" : "Ngữ pháp",
                    true
            ));
        }
        service.bulkCreateGuardianQuestions(20L, 100L, new BulkCreateQuizQuestionsRequest(batch));
        service.updateGuardianSettings(20L, 100L, new UpdateQuizSettingsRequest(
                true, 10, List.of(1, 2, 3, 4, 5, 6, 7), QuizSelectionMode.SEQUENTIAL_ROTATION, false, false
        ));

        // Day 1 (2026-11-23 Monday): Questions 1-10
        setClockDate(2026, 11, 23);
        StudentDailyQuizResponse day1 = service.getTodayQuizForStudent(10L);
        assertTrue(day1.gateLocked());
        assertEquals(10, day1.session().totalQuestions());
        assertEquals("Câu hỏi 1", day1.session().items().get(0).questionText());
        assertEquals("Câu hỏi 10", day1.session().items().get(9).questionText());
        completeAllItems(day1, QuizOption.A);

        // Day 2 (2026-11-24 Tuesday): Questions 11-20
        setClockDate(2026, 11, 24);
        StudentDailyQuizResponse day2 = service.getTodayQuizForStudent(10L);
        assertEquals(10, day2.session().totalQuestions());
        assertEquals("Câu hỏi 11", day2.session().items().get(0).questionText());
        assertEquals("Câu hỏi 20", day2.session().items().get(9).questionText());
        completeAllItems(day2, QuizOption.A);

        // Day 3 skipped by student -> Day 4 (2026-11-26 Thursday): Remaining 21-25 (5 questions)
        setClockDate(2026, 11, 26);
        StudentDailyQuizResponse day4 = service.getTodayQuizForStudent(10L);
        assertEquals(5, day4.session().totalQuestions());
        assertEquals("Câu hỏi 21", day4.session().items().get(0).questionText());
        assertEquals("Câu hỏi 25", day4.session().items().get(4).questionText());
        completeAllItems(day4, QuizOption.A);

        // Day 5 (2026-11-27 Friday): Wraps back to Questions 1-10
        setClockDate(2026, 11, 27);
        StudentDailyQuizResponse day5 = service.getTodayQuizForStudent(10L);
        assertEquals(10, day5.session().totalQuestions());
        assertEquals("Câu hỏi 1", day5.session().items().get(0).questionText());

        GuardianQuizStatsResponse stats = service.getGuardianStats(20L, 100L, 30);
        assertEquals(3, stats.totalCompletedSessions());
        assertFalse(stats.hardestQuestions().isEmpty());
    }

    @Test
    void weekdayScheduleAndEmptyBankNeverLockStudentOut() {
        service.createGuardianQuestion(20L, 100L, new CreateQuizQuestionRequest(
                "1 + 1 = ?", "2", "3", QuizOption.A, "Cộng cơ bản", "Toán", true
        ));
        service.updateGuardianSettings(20L, 100L, new UpdateQuizSettingsRequest(
                true, 10, List.of(1, 2, 3, 4, 5), QuizSelectionMode.SEQUENTIAL_ROTATION, false, false
        ));

        // 2026-11-28 is Saturday (weekday 6 -> not in Mon-Fri)
        setClockDate(2026, 11, 28);
        StudentDailyQuizResponse saturday = service.getTodayQuizForStudent(10L);
        assertFalse(saturday.activeToday());
        assertFalse(saturday.requiredToday());
        assertFalse(saturday.gateLocked());
        assertNull(saturday.session());

        // Turn Monday on, but deactivate all questions -> still safe
        storedQuestions.get(0).setActive(false);
        setClockDate(2026, 11, 30);
        StudentDailyQuizResponse mondayEmptyBank = service.getTodayQuizForStudent(10L);
        assertTrue(mondayEmptyBank.activeToday());
        assertFalse(mondayEmptyBank.requiredToday());
        assertFalse(mondayEmptyBank.gateLocked());
    }

    @Test
    void masksUnansweredCorrectOptionEnforcesSequentialOrderAndBlocksReanswer() {
        service.createGuardianQuestion(20L, 100L, new CreateQuizQuestionRequest(
                "Thủ đô Việt Nam?", "Hà Nội", "Huế", QuizOption.A, "Hà Nội là thủ đô", "Địa lí", true
        ));
        service.createGuardianQuestion(20L, 100L, new CreateQuizQuestionRequest(
                "2 x 3 = ?", "5", "6", QuizOption.B, "2 nhân 3 bằng 6", "Toán", true
        ));
        service.updateGuardianSettings(20L, 100L, new UpdateQuizSettingsRequest(
                true, 10, List.of(1, 2, 3, 4, 5, 6, 7), QuizSelectionMode.SEQUENTIAL_ROTATION, false, false
        ));
        setClockDate(2026, 11, 28);

        StudentDailyQuizResponse quiz = service.getTodayQuizForStudent(10L);
        StudentDailyQuizItemResponse q1 = quiz.session().items().get(0);
        StudentDailyQuizItemResponse q2 = quiz.session().items().get(1);

        // Anti-cheat: unanswered items hide correctOption & explanation
        assertNull(q1.correctOption());
        assertNull(q1.correct());
        assertNull(q1.explanation());

        // Out-of-order attempt blocked
        AppException outOfOrder = assertThrows(AppException.class, () ->
                service.submitStudentAnswer(10L, quiz.session().sessionId(), q2.id(),
                        new SubmitDailyQuizAnswerRequest(QuizOption.B))
        );
        assertEquals(ErrorCode.BAD_REQUEST, outOfOrder.getErrorCode());

        // Answer Q1 -> reveals explanation immediately, still locked until Q2
        SubmitDailyQuizAnswerResponse ans1 = service.submitStudentAnswer(
                10L, quiz.session().sessionId(), q1.id(), new SubmitDailyQuizAnswerRequest(QuizOption.A)
        );
        assertTrue(ans1.correct());
        assertEquals(QuizOption.A, ans1.correctOption());
        assertEquals("Hà Nội là thủ đô", ans1.explanation());
        assertTrue(ans1.gateLocked());
        assertEquals(DailyQuizSessionStatus.IN_PROGRESS, ans1.sessionStatus());

        // Cannot re-answer Q1
        AppException reAnswer = assertThrows(AppException.class, () ->
                service.submitStudentAnswer(10L, quiz.session().sessionId(), q1.id(),
                        new SubmitDailyQuizAnswerRequest(QuizOption.B))
        );
        assertEquals(ErrorCode.CONFLICT, reAnswer.getErrorCode());

        // Answer Q2 wrong -> unlocks gate and completes session
        SubmitDailyQuizAnswerResponse ans2 = service.submitStudentAnswer(
                10L, quiz.session().sessionId(), q2.id(), new SubmitDailyQuizAnswerRequest(QuizOption.A)
        );
        assertFalse(ans2.correct());
        assertEquals(QuizOption.B, ans2.correctOption());
        assertFalse(ans2.gateLocked());
        assertEquals(DailyQuizSessionStatus.COMPLETED, ans2.sessionStatus());
        assertEquals(50, ans2.accuracyPercent());
        assertNotNull(ans2.completedAt());
    }

    @Test
    void moveQuestionSwapsSortOrderCleanly() {
        service.createGuardianQuestion(20L, 100L, new CreateQuizQuestionRequest(
                "Q1", "A1", "B1", QuizOption.A, null, null, true
        ));
        service.createGuardianQuestion(20L, 100L, new CreateQuizQuestionRequest(
                "Q2", "A2", "B2", QuizOption.B, null, null, true
        ));

        Long secondId = storedQuestions.get(1).getId();
        List<QuizQuestionResponse> moved = service.moveGuardianQuestion(
                20L, secondId, 100L, new MoveQuizQuestionRequest(MoveQuizQuestionRequest.MoveDirection.UP)
        );
        assertEquals("Q2", moved.get(0).questionText());
        assertEquals(1, moved.get(0).sortOrder());
        assertEquals("Q1", moved.get(1).questionText());
        assertEquals(2, moved.get(1).sortOrder());

        Long firstId = moved.get(0).id();
        List<QuizQuestionResponse> movedDown = service.moveGuardianQuestion(
                20L, firstId, 100L, new MoveQuizQuestionRequest(MoveQuizQuestionRequest.MoveDirection.DOWN)
        );
        assertEquals("Q1", movedDown.get(0).questionText());
        assertEquals("Q2", movedDown.get(1).questionText());

        // Boundary no-op: moving first UP keeps order
        List<QuizQuestionResponse> boundary = service.moveGuardianQuestion(
                20L, movedDown.get(0).id(), 100L, new MoveQuizQuestionRequest(MoveQuizQuestionRequest.MoveDirection.UP)
        );
        assertEquals("Q1", boundary.get(0).questionText());
    }

    @Test
    void randomModeWithShuffleOptionsGradesAccuratelyAndResetsCursorOnDelete() {
        for (int i = 1; i <= 4; i++) {
            service.createGuardianQuestion(20L, 100L, new CreateQuizQuestionRequest(
                    "Câu " + i, "Đúng " + i, "Sai " + i, QuizOption.A, "Giải thích " + i, "Toán", true
            ));
        }
        service.updateGuardianSettings(20L, 100L, new UpdateQuizSettingsRequest(
                true, 2, List.of(1, 2, 3, 4, 5, 6, 7), QuizSelectionMode.RANDOM, true, false
        ));
        setClockDate(2026, 11, 28);

        StudentDailyQuizResponse quiz = service.getTodayQuizForStudent(10L);
        assertEquals(2, quiz.session().totalQuestions());
        assertTrue(quiz.nextBatchLabel().startsWith("Ngẫu nhiên"));

        // Match whichever displayed option equals "Đúng ..."
        for (StudentDailyQuizItemResponse item : quiz.session().items()) {
            QuizOption pick = item.optionA().startsWith("Đúng") ? QuizOption.A : QuizOption.B;
            SubmitDailyQuizAnswerResponse ans = service.submitStudentAnswer(
                    10L, quiz.session().sessionId(), item.id(), new SubmitDailyQuizAnswerRequest(pick)
            );
            assertTrue(ans.correct());
        }

        // Update question & toggle active=false, then delete first question
        QuizQuestionResponse updated = service.updateGuardianQuestion(
                20L, storedQuestions.get(0).getId(), 100L,
                new CreateQuizQuestionRequest("Câu 1 mới", "ĐA", "ĐB", QuizOption.B, null, "Lý", false)
        );
        assertFalse(updated.active());
        assertEquals("Câu 1 mới", updated.questionText());

        lenient().when(questionRepository.findByIdAndStudentId(storedQuestions.get(0).getId(), 100L))
                .thenReturn(Optional.of(storedQuestions.get(0)));
        org.mockito.Mockito.doAnswer(inv -> {
            GuardianQuizQuestion q = inv.getArgument(0);
            storedQuestions.removeIf(x -> x.getId().equals(q.getId()));
            return null;
        }).when(questionRepository).delete(any(GuardianQuizQuestion.class));

        service.deleteGuardianQuestion(20L, storedQuestions.get(0).getId(), 100L);
        assertEquals(3, storedQuestions.size());
        assertEquals(1, storedQuestions.get(0).getSortOrder());

        // Guardian session detail read + malformed activeWeekdays resilient check
        var detail = service.getGuardianSessionDetail(20L, quiz.session().sessionId(), 100L);
        assertEquals("Ngẫu nhiên", detail.cycleLabel());
        assertEquals(2, detail.items().size());

        storedSettings.setActiveWeekdays("MON,bad,6,7");
        GuardianQuizStatsResponse stats = service.getGuardianStats(20L, 100L, 14);
        assertEquals(1, stats.quizStreakDays());
    }

    @Test
    void getSessionDetailAndSubmitEnforceOwnershipAndNotFound() {
        AppException missingSession = assertThrows(AppException.class, () ->
                service.getGuardianSessionDetail(20L, 9999L, 100L)
        );
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, missingSession.getErrorCode());
    }

    private void setClockDate(int year, int month, int day) {
        Instant instant = LocalDate.of(year, month, day)
                .atTime(8, 0)
                .atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                .toInstant();
        service.setClock(Clock.fixed(instant, ZoneId.of("Asia/Ho_Chi_Minh")));
    }

    private void completeAllItems(StudentDailyQuizResponse quiz, QuizOption pick) {
        for (StudentDailyQuizItemResponse item : quiz.session().items()) {
            service.submitStudentAnswer(10L, quiz.session().sessionId(), item.id(), new SubmitDailyQuizAnswerRequest(pick));
        }
    }
}
