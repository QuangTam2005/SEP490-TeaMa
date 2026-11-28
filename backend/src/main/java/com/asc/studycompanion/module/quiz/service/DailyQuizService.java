package com.asc.studycompanion.module.quiz.service;

import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.module.auth.entity.StudentProfile;
import com.asc.studycompanion.module.auth.entity.User;
import com.asc.studycompanion.module.auth.repository.UserRepository;
import com.asc.studycompanion.module.auth.service.StudyAccessGuard;
import com.asc.studycompanion.module.quiz.dto.BulkCreateQuizQuestionsRequest;
import com.asc.studycompanion.module.quiz.dto.CreateQuizQuestionRequest;
import com.asc.studycompanion.module.quiz.dto.GuardianHardestQuestionItem;
import com.asc.studycompanion.module.quiz.dto.GuardianQuizDailyStatItem;
import com.asc.studycompanion.module.quiz.dto.GuardianQuizSessionDetailItem;
import com.asc.studycompanion.module.quiz.dto.GuardianQuizSessionDetailResponse;
import com.asc.studycompanion.module.quiz.dto.GuardianQuizSettingsResponse;
import com.asc.studycompanion.module.quiz.dto.GuardianQuizStatsResponse;
import com.asc.studycompanion.module.quiz.dto.MoveQuizQuestionRequest;
import com.asc.studycompanion.module.quiz.dto.QuizQuestionResponse;
import com.asc.studycompanion.module.quiz.dto.StudentDailyQuizItemResponse;
import com.asc.studycompanion.module.quiz.dto.StudentDailyQuizResponse;
import com.asc.studycompanion.module.quiz.dto.StudentDailyQuizSessionResponse;
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
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailyQuizService {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String DEFAULT_ACTIVE_WEEKDAYS = "1,2,3,4,5,6,7";
    private static final int DEFAULT_QUESTIONS_PER_DAY = 10;

    private final StudyAccessGuard studyAccessGuard;
    private final UserRepository userRepository;
    private final DailyQuizSettingsRepository settingsRepository;
    private final GuardianQuizQuestionRepository questionRepository;
    private final DailyQuizSessionRepository sessionRepository;
    private final DailyQuizSessionItemRepository sessionItemRepository;

    @Autowired(required = false)
    private EntityManager entityManager;

    private Clock clock = Clock.systemUTC();
    private Random random = new Random();

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    public void setRandom(Random random) {
        this.random = random;
    }

    @Transactional(readOnly = true)
    public GuardianQuizSettingsResponse getGuardianSettings(Long guardianUserId, Long requestedStudentId) {
        StudentProfile student = studyAccessGuard.requireLinkedStudentForGuardian(guardianUserId, requestedStudentId);
        DailyQuizSettings settings = settingsRepository.findByStudentId(student.getId())
                .orElseGet(() -> defaultSettings(student));
        List<GuardianQuizQuestion> allQuestions = questionRepository.findByStudentIdOrderBySortOrderAscIdAsc(student.getId());
        long activeCount = allQuestions.stream().filter(GuardianQuizQuestion::isActive).count();
        return toSettingsResponse(student.getId(), settings, allQuestions.size(), activeCount);
    }

    @Transactional
    public GuardianQuizSettingsResponse updateGuardianSettings(
            Long guardianUserId,
            Long requestedStudentId,
            UpdateQuizSettingsRequest request
    ) {
        StudentProfile student = studyAccessGuard.requireLinkedStudentForGuardian(guardianUserId, requestedStudentId);
        lockStudentProfile(student.getId());
        User guardian = userRepository.findById(guardianUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        DailyQuizSettings settings = settingsRepository.findByStudentId(student.getId())
                .orElseGet(() -> defaultSettings(student));

        List<Integer> normalizedWeekdays = normalizeWeekdays(request.activeWeekdays());
        settings.setEnabled(Boolean.TRUE.equals(request.enabled()));
        settings.setQuestionsPerDay(request.questionsPerDay());
        settings.setActiveWeekdays(serializeWeekdays(normalizedWeekdays));
        settings.setSelectionMode(request.selectionMode());
        settings.setShuffleOptions(Boolean.TRUE.equals(request.shuffleOptions()));
        settings.setUpdatedBy(guardian);

        long activeCount = questionRepository.countByStudentIdAndActiveTrue(student.getId());
        if (Boolean.TRUE.equals(request.resetCursor()) || settings.getRotationCursor() >= activeCount) {
            settings.setRotationCursor(0);
        }

        DailyQuizSettings saved = settingsRepository.save(settings);
        long totalCount = questionRepository.countByStudentId(student.getId());
        return toSettingsResponse(student.getId(), saved, totalCount, activeCount);
    }

    @Transactional(readOnly = true)
    public List<QuizQuestionResponse> listGuardianQuestions(Long guardianUserId, Long requestedStudentId) {
        StudentProfile student = studyAccessGuard.requireLinkedStudentForGuardian(guardianUserId, requestedStudentId);
        return questionRepository.findByStudentIdOrderBySortOrderAscIdAsc(student.getId())
                .stream()
                .map(this::toQuestionResponse)
                .toList();
    }

    @Transactional
    public QuizQuestionResponse createGuardianQuestion(
            Long guardianUserId,
            Long requestedStudentId,
            CreateQuizQuestionRequest request
    ) {
        StudentProfile student = studyAccessGuard.requireLinkedStudentForGuardian(guardianUserId, requestedStudentId);
        lockStudentProfile(student.getId());
        User guardian = userRepository.findById(guardianUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        List<GuardianQuizQuestion> existing = questionRepository.findByStudentIdOrderBySortOrderAscIdAsc(student.getId());
        int nextSortOrder = existing.isEmpty() ? 1 : existing.get(existing.size() - 1).getSortOrder() + 1;

        GuardianQuizQuestion question = buildQuestionEntity(student, guardian, nextSortOrder, request);
        GuardianQuizQuestion saved = questionRepository.save(question);
        return toQuestionResponse(saved);
    }

    @Transactional
    public List<QuizQuestionResponse> bulkCreateGuardianQuestions(
            Long guardianUserId,
            Long requestedStudentId,
            BulkCreateQuizQuestionsRequest request
    ) {
        StudentProfile student = studyAccessGuard.requireLinkedStudentForGuardian(guardianUserId, requestedStudentId);
        lockStudentProfile(student.getId());
        User guardian = userRepository.findById(guardianUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        List<GuardianQuizQuestion> existing = questionRepository.findByStudentIdOrderBySortOrderAscIdAsc(student.getId());
        int nextSortOrder = existing.isEmpty() ? 1 : existing.get(existing.size() - 1).getSortOrder() + 1;

        List<GuardianQuizQuestion> toSave = new ArrayList<>();
        for (CreateQuizQuestionRequest item : request.questions()) {
            toSave.add(buildQuestionEntity(student, guardian, nextSortOrder++, item));
        }

        List<GuardianQuizQuestion> saved = questionRepository.saveAll(toSave);
        return saved.stream().map(this::toQuestionResponse).toList();
    }

    @Transactional
    public QuizQuestionResponse updateGuardianQuestion(
            Long guardianUserId,
            Long questionId,
            Long requestedStudentId,
            CreateQuizQuestionRequest request
    ) {
        StudentProfile student = studyAccessGuard.requireLinkedStudentForGuardian(guardianUserId, requestedStudentId);
        lockStudentProfile(student.getId());
        GuardianQuizQuestion question = questionRepository.findByIdAndStudentId(questionId, student.getId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy câu hỏi"));

        question.setQuestionText(request.questionText().trim());
        question.setOptionA(request.optionA().trim());
        question.setOptionB(request.optionB().trim());
        question.setCorrectOption(request.correctOption());
        question.setExplanation(normalizeNullable(request.explanation()));
        question.setTopicLabel(normalizeNullable(request.topicLabel()));
        if (request.active() != null) {
            question.setActive(request.active());
        }

        GuardianQuizQuestion saved = questionRepository.save(question);
        clampCursorToActiveCount(student.getId());
        return toQuestionResponse(saved);
    }

    @Transactional
    public List<QuizQuestionResponse> moveGuardianQuestion(
            Long guardianUserId,
            Long questionId,
            Long requestedStudentId,
            MoveQuizQuestionRequest request
    ) {
        StudentProfile student = studyAccessGuard.requireLinkedStudentForGuardian(guardianUserId, requestedStudentId);
        lockStudentProfile(student.getId());
        List<GuardianQuizQuestion> questions = new ArrayList<>(
                questionRepository.findByStudentIdOrderBySortOrderAscIdAsc(student.getId())
        );

        int idx = -1;
        for (int i = 0; i < questions.size(); i++) {
            if (questions.get(i).getId().equals(questionId)) {
                idx = i;
                break;
            }
        }
        if (idx < 0) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy câu hỏi");
        }

        int swapIdx = request.direction() == MoveQuizQuestionRequest.MoveDirection.UP ? idx - 1 : idx + 1;
        if (swapIdx >= 0 && swapIdx < questions.size()) {
            Collections.swap(questions, idx, swapIdx);
            for (int i = 0; i < questions.size(); i++) {
                questions.get(i).setSortOrder(i + 1);
            }
            questionRepository.saveAll(questions);
        }

        return questions.stream().map(this::toQuestionResponse).toList();
    }

    @Transactional
    public void deleteGuardianQuestion(Long guardianUserId, Long questionId, Long requestedStudentId) {
        StudentProfile student = studyAccessGuard.requireLinkedStudentForGuardian(guardianUserId, requestedStudentId);
        lockStudentProfile(student.getId());
        GuardianQuizQuestion question = questionRepository.findByIdAndStudentId(questionId, student.getId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy câu hỏi"));

        questionRepository.delete(question);

        List<GuardianQuizQuestion> remaining = questionRepository.findByStudentIdOrderBySortOrderAscIdAsc(student.getId());
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setSortOrder(i + 1);
        }
        if (!remaining.isEmpty()) {
            questionRepository.saveAll(remaining);
        }
        clampCursorToActiveCount(student.getId());
    }

    @Transactional
    public StudentDailyQuizResponse getTodayQuizForStudent(Long studentUserId) {
        StudentProfile student = studyAccessGuard.requireOwnedStudentProfile(studentUserId);
        lockStudentProfile(student.getId());

        LocalDate today = LocalDate.now(clock.withZone(resolveZoneId(student)));
        int todayWeekday = today.getDayOfWeek().getValue();

        DailyQuizSettings settings = settingsRepository.findByStudentId(student.getId())
                .orElseGet(() -> defaultSettings(student));
        List<GuardianQuizQuestion> activeQuestions =
                questionRepository.findByStudentIdAndActiveTrueOrderBySortOrderAscIdAsc(student.getId());

        boolean activeToday = settings.isEnabled() && parseWeekdays(settings.getActiveWeekdays()).contains(todayWeekday);
        Optional<DailyQuizSession> existingSessionOpt =
                sessionRepository.findByStudentIdAndSessionDate(student.getId(), today);

        String nextBatchLabel = buildNextBatchLabel(settings, activeQuestions.size());

        // Never trap the student behind a quiz gate when parent disabled it, weekday is off, or bank is empty
        if (!activeToday || activeQuestions.isEmpty()) {
            StudentDailyQuizSessionResponse completedOrExisting = existingSessionOpt
                    .filter(s -> s.getStatus() == DailyQuizSessionStatus.COMPLETED)
                    .map(s -> toStudentSessionResponse(s, loadSessionItems(s)))
                    .orElse(null);
            return new StudentDailyQuizResponse(
                    student.getId(),
                    today,
                    settings.isEnabled(),
                    activeToday,
                    false,
                    false,
                    nextBatchLabel,
                    completedOrExisting
            );
        }

        DailyQuizSession session = existingSessionOpt.orElseGet(() ->
                createSessionForToday(student, today, settings, activeQuestions)
        );
        List<DailyQuizSessionItem> items = loadSessionItems(session);
        boolean gateLocked = session.getStatus() != DailyQuizSessionStatus.COMPLETED;

        return new StudentDailyQuizResponse(
                student.getId(),
                today,
                settings.isEnabled(),
                true,
                true,
                gateLocked,
                nextBatchLabel,
                toStudentSessionResponse(session, items)
        );
    }

    @Transactional
    public SubmitDailyQuizAnswerResponse submitStudentAnswer(
            Long studentUserId,
            Long sessionId,
            Long itemId,
            SubmitDailyQuizAnswerRequest request
    ) {
        StudentProfile student = studyAccessGuard.requireOwnedStudentProfile(studentUserId);
        lockStudentProfile(student.getId());

        DailyQuizSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy phiên trắc nghiệm"));
        if (!session.getStudent().getId().equals(student.getId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        if (session.getStatus() == DailyQuizSessionStatus.COMPLETED) {
            throw new AppException(ErrorCode.CONFLICT, "Phiên trắc nghiệm hôm nay đã hoàn thành");
        }

        List<DailyQuizSessionItem> items = loadSessionItems(session);
        DailyQuizSessionItem targetItem = items.stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy câu hỏi"));

        if (targetItem.getSelectedOption() != null) {
            throw new AppException(ErrorCode.CONFLICT, "Câu hỏi này đã được trả lời rồi");
        }

        Optional<DailyQuizSessionItem> firstUnanswered = items.stream()
                .filter(i -> i.getSelectedOption() == null)
                .findFirst();
        if (firstUnanswered.isPresent() && !firstUnanswered.get().getId().equals(targetItem.getId())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Vui lòng trả lời lần lượt từng câu hỏi");
        }

        boolean isCorrect = request.selectedOption() == targetItem.getCorrectDisplayOption();
        Instant now = clock.instant();
        targetItem.setSelectedOption(request.selectedOption());
        targetItem.setIsCorrect(isCorrect);
        targetItem.setAnsweredAt(now);
        sessionItemRepository.save(targetItem);

        int answeredCount = (int) items.stream().filter(i -> i.getSelectedOption() != null).count();
        int correctCount = (int) items.stream().filter(i -> Boolean.TRUE.equals(i.getIsCorrect())).count();
        session.setAnsweredCount(answeredCount);
        session.setCorrectCount(correctCount);

        boolean completedNow = answeredCount >= session.getTotalQuestions();
        if (completedNow) {
            session.setStatus(DailyQuizSessionStatus.COMPLETED);
            session.setCompletedAt(now);
            advanceRotationCursorIfNeeded(student, session);
        }
        sessionRepository.save(session);

        int accuracyPercent = toPercent(correctCount, answeredCount);
        return new SubmitDailyQuizAnswerResponse(
                session.getId(),
                targetItem.getId(),
                targetItem.getSelectedOption(),
                targetItem.getCorrectDisplayOption(),
                isCorrect,
                targetItem.getExplanation(),
                answeredCount,
                correctCount,
                session.getTotalQuestions(),
                accuracyPercent,
                session.getStatus(),
                !completedNow,
                session.getCompletedAt()
        );
    }

    @Transactional(readOnly = true)
    public GuardianQuizStatsResponse getGuardianStats(Long guardianUserId, Long requestedStudentId, Integer days) {
        StudentProfile student = studyAccessGuard.requireLinkedStudentForGuardian(guardianUserId, requestedStudentId);
        int windowDays = clampDays(days);
        LocalDate today = LocalDate.now(clock.withZone(resolveZoneId(student)));
        LocalDate fromDate = today.minusDays(windowDays - 1L);

        DailyQuizSettings settings = settingsRepository.findByStudentId(student.getId())
                .orElseGet(() -> defaultSettings(student));
        List<GuardianQuizQuestion> allQuestions =
                questionRepository.findByStudentIdOrderBySortOrderAscIdAsc(student.getId());
        long activeQuestionCount = allQuestions.stream().filter(GuardianQuizQuestion::isActive).count();

        List<DailyQuizSession> sessionsInWindow = sessionRepository
                .findByStudentIdAndSessionDateBetweenOrderBySessionDateDesc(student.getId(), fromDate, today);

        List<GuardianQuizDailyStatItem> dailyStats = sessionsInWindow.stream()
                .map(this::toDailyStatItem)
                .toList();

        List<DailyQuizSession> completedSessions = sessionsInWindow.stream()
                .filter(s -> s.getStatus() == DailyQuizSessionStatus.COMPLETED)
                .toList();

        int totalAnswered = sessionsInWindow.stream().mapToInt(DailyQuizSession::getAnsweredCount).sum();
        int totalCorrect = sessionsInWindow.stream().mapToInt(DailyQuizSession::getCorrectCount).sum();
        int averageAccuracy = toPercent(totalCorrect, totalAnswered);

        DailyQuizSessionStatus todayStatus = sessionsInWindow.stream()
                .filter(s -> s.getSessionDate().equals(today))
                .map(DailyQuizSession::getStatus)
                .findFirst()
                .orElse(null);

        List<DailyQuizSession> allSessions = sessionRepository.findByStudentIdOrderBySessionDateDesc(student.getId());
        int streakDays = computeQuizStreak(allSessions, settings, today);

        List<GuardianHardestQuestionItem> hardestQuestions = computeHardestQuestions(sessionsInWindow);

        return new GuardianQuizStatsResponse(
                student.getId(),
                student.getDisplayName(),
                allQuestions.size(),
                activeQuestionCount,
                completedSessions.size(),
                totalAnswered,
                totalCorrect,
                averageAccuracy,
                streakDays,
                todayStatus,
                dailyStats,
                hardestQuestions
        );
    }

    @Transactional(readOnly = true)
    public GuardianQuizSessionDetailResponse getGuardianSessionDetail(
            Long guardianUserId,
            Long sessionId,
            Long requestedStudentId
    ) {
        StudentProfile student = studyAccessGuard.requireLinkedStudentForGuardian(guardianUserId, requestedStudentId);
        DailyQuizSession session = sessionRepository.findByIdAndStudentId(sessionId, student.getId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy phiên làm bài"));

        List<DailyQuizSessionItem> items = loadSessionItems(session);
        List<GuardianQuizSessionDetailItem> detailItems = items.stream()
                .map(item -> new GuardianQuizSessionDetailItem(
                        item.getId(),
                        item.getItemOrder(),
                        item.getQuestionId(),
                        item.getQuestionText(),
                        item.getDisplayOptionA(),
                        item.getDisplayOptionB(),
                        item.getTopicLabel(),
                        item.getSelectedOption(),
                        item.getCorrectDisplayOption(),
                        item.getIsCorrect(),
                        item.getExplanation(),
                        item.getAnsweredAt()
                ))
                .toList();

        return new GuardianQuizSessionDetailResponse(
                session.getId(),
                student.getId(),
                student.getDisplayName(),
                session.getSessionDate(),
                session.getStatus(),
                session.getTotalQuestions(),
                session.getAnsweredCount(),
                session.getCorrectCount(),
                toPercent(session.getCorrectCount(), session.getAnsweredCount()),
                formatCycleLabel(session),
                session.getStartedAt(),
                session.getCompletedAt(),
                detailItems
        );
    }

    private DailyQuizSession createSessionForToday(
            StudentProfile student,
            LocalDate today,
            DailyQuizSettings settings,
            List<GuardianQuizQuestion> activeQuestions
    ) {
        int activeCount = activeQuestions.size();
        int batchSize = Math.max(1, settings.getQuestionsPerDay());
        List<GuardianQuizQuestion> selected;
        int startIndex = 0;
        int endIndex = 0;

        if (settings.getSelectionMode() == QuizSelectionMode.RANDOM) {
            List<GuardianQuizQuestion> shuffled = new ArrayList<>(activeQuestions);
            Collections.shuffle(shuffled, random);
            selected = shuffled.subList(0, Math.min(batchSize, activeCount));
        } else {
            int rawCursor = settings.getRotationCursor();
            startIndex = (rawCursor < 0 || rawCursor >= activeCount) ? 0 : rawCursor;
            endIndex = Math.min(startIndex + batchSize, activeCount);
            selected = activeQuestions.subList(startIndex, endIndex);
        }

        DailyQuizSession session = DailyQuizSession.builder()
                .student(student)
                .sessionDate(today)
                .status(DailyQuizSessionStatus.IN_PROGRESS)
                .totalQuestions(selected.size())
                .answeredCount(0)
                .correctCount(0)
                .cycleStartIndex(startIndex)
                .cycleEndIndex(endIndex)
                .startedAt(clock.instant())
                .build();

        int order = 1;
        for (GuardianQuizQuestion question : selected) {
            boolean flip = settings.isShuffleOptions() && random.nextBoolean();
            String displayA = flip ? question.getOptionB() : question.getOptionA();
            String displayB = flip ? question.getOptionA() : question.getOptionB();
            QuizOption correctDisplay = flip
                    ? (question.getCorrectOption() == QuizOption.A ? QuizOption.B : QuizOption.A)
                    : question.getCorrectOption();

            session.getItems().add(DailyQuizSessionItem.builder()
                    .session(session)
                    .questionId(question.getId())
                    .itemOrder(order++)
                    .questionText(question.getQuestionText())
                    .displayOptionA(displayA)
                    .displayOptionB(displayB)
                    .correctDisplayOption(correctDisplay)
                    .explanation(question.getExplanation())
                    .topicLabel(question.getTopicLabel())
                    .build());
        }

        return sessionRepository.save(session);
    }

    private void advanceRotationCursorIfNeeded(StudentProfile student, DailyQuizSession session) {
        DailyQuizSettings settings = settingsRepository.findByStudentId(student.getId())
                .orElseGet(() -> defaultSettings(student));
        if (settings.getSelectionMode() != QuizSelectionMode.SEQUENTIAL_ROTATION) {
            return;
        }
        long activeCount = questionRepository.countByStudentIdAndActiveTrue(student.getId());
        int nextCursor = (activeCount <= 0 || session.getCycleEndIndex() >= activeCount)
                ? 0
                : session.getCycleEndIndex();
        settings.setRotationCursor(nextCursor);
        settingsRepository.save(settings);
    }

    private void clampCursorToActiveCount(Long studentId) {
        settingsRepository.findByStudentId(studentId).ifPresent(settings -> {
            long activeCount = questionRepository.countByStudentIdAndActiveTrue(studentId);
            if (activeCount <= 0 || settings.getRotationCursor() >= activeCount) {
                settings.setRotationCursor(0);
                settingsRepository.save(settings);
            }
        });
    }

    private List<DailyQuizSessionItem> loadSessionItems(DailyQuizSession session) {
        if (session.getItems() != null && !session.getItems().isEmpty()) {
            return session.getItems().stream()
                    .sorted(Comparator.comparingInt(DailyQuizSessionItem::getItemOrder))
                    .toList();
        }
        return sessionItemRepository.findBySessionIdOrderByItemOrderAsc(session.getId());
    }

    private List<GuardianHardestQuestionItem> computeHardestQuestions(List<DailyQuizSession> sessions) {
        List<Long> sessionIds = sessions.stream().map(DailyQuizSession::getId).toList();
        if (sessionIds.isEmpty()) {
            return List.of();
        }
        List<DailyQuizSessionItem> allItems = sessionItemRepository.findBySessionIdInOrderBySessionIdAscItemOrderAsc(sessionIds);
        Map<String, HardestAccumulator> map = new LinkedHashMap<>();

        for (DailyQuizSessionItem item : allItems) {
            if (item.getSelectedOption() == null) {
                continue;
            }
            String key = item.getQuestionId() != null
                    ? "Q:" + item.getQuestionId()
                    : "TXT:" + item.getQuestionText();
            HardestAccumulator acc = map.computeIfAbsent(key, k -> new HardestAccumulator(
                    item.getQuestionId(),
                    item.getQuestionText(),
                    item.getTopicLabel()
            ));
            acc.attemptCount++;
            if (!Boolean.TRUE.equals(item.getIsCorrect())) {
                acc.wrongCount++;
            }
        }

        return map.values().stream()
                .filter(acc -> acc.wrongCount > 0)
                .sorted(Comparator
                        .comparingInt((HardestAccumulator a) -> a.wrongCount).reversed()
                        .thenComparingInt(a -> toPercent(a.attemptCount - a.wrongCount, a.attemptCount)))
                .limit(5)
                .map(acc -> new GuardianHardestQuestionItem(
                        acc.questionId,
                        acc.questionText,
                        acc.topicLabel,
                        acc.attemptCount,
                        acc.wrongCount,
                        toPercent(acc.attemptCount - acc.wrongCount, acc.attemptCount)
                ))
                .toList();
    }

    private int computeQuizStreak(List<DailyQuizSession> allSessions, DailyQuizSettings settings, LocalDate today) {
        Map<LocalDate, DailyQuizSessionStatus> byDate = allSessions.stream()
                .collect(Collectors.toMap(
                        DailyQuizSession::getSessionDate,
                        DailyQuizSession::getStatus,
                        (a, b) -> a
                ));
        Set<Integer> activeWeekdays = parseWeekdays(settings.getActiveWeekdays());
        if (activeWeekdays.isEmpty()) {
            return 0;
        }

        LocalDate cursor = today;
        if (byDate.get(today) != DailyQuizSessionStatus.COMPLETED) {
            // If today hasn't been completed yet (or today is rest day), allow streak up through yesterday
            cursor = today.minusDays(1);
        }

        int streak = 0;
        for (int i = 0; i < 365; i++) {
            int weekday = cursor.getDayOfWeek().getValue();
            DailyQuizSessionStatus status = byDate.get(cursor);
            if (status == DailyQuizSessionStatus.COMPLETED) {
                streak++;
                cursor = cursor.minusDays(1);
                continue;
            }
            if (!activeWeekdays.contains(weekday)) {
                cursor = cursor.minusDays(1);
                continue;
            }
            break;
        }
        return streak;
    }

    private GuardianQuizQuestion buildQuestionEntity(
            StudentProfile student,
            User guardian,
            int sortOrder,
            CreateQuizQuestionRequest request
    ) {
        return GuardianQuizQuestion.builder()
                .student(student)
                .sortOrder(sortOrder)
                .questionText(request.questionText().trim())
                .optionA(request.optionA().trim())
                .optionB(request.optionB().trim())
                .correctOption(request.correctOption())
                .explanation(normalizeNullable(request.explanation()))
                .topicLabel(normalizeNullable(request.topicLabel()))
                .active(request.active() == null || request.active())
                .createdBy(guardian)
                .build();
    }

    private GuardianQuizSettingsResponse toSettingsResponse(
            Long studentId,
            DailyQuizSettings settings,
            long totalCount,
            long activeCount
    ) {
        return new GuardianQuizSettingsResponse(
                studentId,
                settings.isEnabled(),
                settings.getQuestionsPerDay(),
                new ArrayList<>(parseWeekdays(settings.getActiveWeekdays())),
                settings.getSelectionMode(),
                settings.isShuffleOptions(),
                settings.getRotationCursor(),
                totalCount,
                activeCount,
                buildNextBatchLabel(settings, (int) activeCount)
        );
    }

    private QuizQuestionResponse toQuestionResponse(GuardianQuizQuestion question) {
        return new QuizQuestionResponse(
                question.getId(),
                question.getSortOrder(),
                question.getQuestionText(),
                question.getOptionA(),
                question.getOptionB(),
                question.getCorrectOption(),
                question.getExplanation(),
                question.getTopicLabel(),
                question.isActive(),
                question.getCreatedAt(),
                question.getUpdatedAt()
        );
    }

    private StudentDailyQuizSessionResponse toStudentSessionResponse(
            DailyQuizSession session,
            List<DailyQuizSessionItem> items
    ) {
        List<StudentDailyQuizItemResponse> itemDtos = items.stream()
                .map(item -> {
                    boolean answered = item.getSelectedOption() != null;
                    return new StudentDailyQuizItemResponse(
                            item.getId(),
                            item.getItemOrder(),
                            item.getQuestionText(),
                            item.getDisplayOptionA(),
                            item.getDisplayOptionB(),
                            item.getTopicLabel(),
                            item.getSelectedOption(),
                            answered ? item.getCorrectDisplayOption() : null,
                            answered ? item.getIsCorrect() : null,
                            answered ? item.getExplanation() : null,
                            item.getAnsweredAt()
                    );
                })
                .toList();

        return new StudentDailyQuizSessionResponse(
                session.getId(),
                session.getSessionDate(),
                session.getStatus(),
                session.getTotalQuestions(),
                session.getAnsweredCount(),
                session.getCorrectCount(),
                toPercent(session.getCorrectCount(), session.getAnsweredCount()),
                session.getCycleStartIndex(),
                session.getCycleEndIndex(),
                session.getCompletedAt(),
                itemDtos
        );
    }

    private GuardianQuizDailyStatItem toDailyStatItem(DailyQuizSession session) {
        return new GuardianQuizDailyStatItem(
                session.getId(),
                session.getSessionDate(),
                session.getStatus(),
                session.getTotalQuestions(),
                session.getAnsweredCount(),
                session.getCorrectCount(),
                toPercent(session.getCorrectCount(), session.getAnsweredCount()),
                formatCycleLabel(session),
                session.getCompletedAt()
        );
    }

    private DailyQuizSettings defaultSettings(StudentProfile student) {
        return DailyQuizSettings.builder()
                .student(student)
                .enabled(false)
                .questionsPerDay(DEFAULT_QUESTIONS_PER_DAY)
                .activeWeekdays(DEFAULT_ACTIVE_WEEKDAYS)
                .selectionMode(QuizSelectionMode.SEQUENTIAL_ROTATION)
                .shuffleOptions(false)
                .rotationCursor(0)
                .build();
    }

    private String buildNextBatchLabel(DailyQuizSettings settings, int activeCount) {
        if (activeCount <= 0) {
            return "Chưa có câu hỏi hoạt động";
        }
        if (settings.getSelectionMode() == QuizSelectionMode.RANDOM) {
            int count = Math.min(Math.max(1, settings.getQuestionsPerDay()), activeCount);
            return String.format("Ngẫu nhiên %d / %d câu", count, activeCount);
        }
        int start = (settings.getRotationCursor() < 0 || settings.getRotationCursor() >= activeCount)
                ? 0
                : settings.getRotationCursor();
        int end = Math.min(start + Math.max(1, settings.getQuestionsPerDay()), activeCount);
        return String.format("Câu %d–%d / %d", start + 1, end, activeCount);
    }

    private String formatCycleLabel(DailyQuizSession session) {
        if (session.getCycleStartIndex() == 0 && session.getCycleEndIndex() == 0) {
            return "Ngẫu nhiên";
        }
        return String.format("Câu %d–%d", session.getCycleStartIndex() + 1, session.getCycleEndIndex());
    }

    private void lockStudentProfile(Long studentId) {
        if (entityManager != null && studentId != null) {
            entityManager.find(StudentProfile.class, studentId, LockModeType.PESSIMISTIC_WRITE);
        }
    }

    private ZoneId resolveZoneId(StudentProfile student) {
        String raw = student.getTimezone();
        if (raw == null || raw.isBlank()) {
            return DEFAULT_ZONE;
        }
        try {
            return ZoneId.of(raw.trim());
        } catch (DateTimeException ex) {
            return DEFAULT_ZONE;
        }
    }

    private List<Integer> normalizeWeekdays(List<Integer> rawWeekdays) {
        if (rawWeekdays == null || rawWeekdays.isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Vui lòng chọn ít nhất 1 ngày áp dụng trong tuần");
        }
        Set<Integer> sorted = new TreeSet<>();
        for (Integer day : rawWeekdays) {
            if (day == null || day < 1 || day > 7) {
                throw new AppException(ErrorCode.VALIDATION_FAILED, "Ngày trong tuần không hợp lệ (chỉ nhận Thứ 2–CN)");
            }
            sorted.add(day);
        }
        return new ArrayList<>(sorted);
    }

    private Set<Integer> parseWeekdays(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of(1, 2, 3, 4, 5, 6, 7);
        }
        Set<Integer> parsed = new TreeSet<>();
        for (String token : raw.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                int day = Integer.parseInt(trimmed);
                if (day >= 1 && day <= 7) {
                    parsed.add(day);
                }
            } catch (NumberFormatException ignored) {
                // Skip malformed weekday tokens instead of failing with 500
            }
        }
        return parsed.isEmpty() ? Set.of(1, 2, 3, 4, 5, 6, 7) : parsed;
    }

    private String serializeWeekdays(List<Integer> weekdays) {
        return weekdays.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private String normalizeNullable(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int clampDays(Integer days) {
        if (days == null) {
            return 30;
        }
        return Math.max(1, Math.min(90, days));
    }

    private int toPercent(int numerator, int denominator) {
        if (denominator <= 0) {
            return 0;
        }
        return (int) Math.round((numerator * 100.0) / denominator);
    }

    private static final class HardestAccumulator {
        final Long questionId;
        final String questionText;
        final String topicLabel;
        int attemptCount;
        int wrongCount;

        HardestAccumulator(Long questionId, String questionText, String topicLabel) {
            this.questionId = questionId;
            this.questionText = questionText;
            this.topicLabel = topicLabel;
        }
    }
}
