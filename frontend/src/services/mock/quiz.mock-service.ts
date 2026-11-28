import { ApiClientError } from '@/lib/api-client';
import { getTodayDateIso, getWeekdayFromIsoDate } from '@/lib/study-utils';
import type { QuizService } from '@/services/quiz.service';
import type {
  BulkCreateQuizQuestionsPayload,
  CreateQuizQuestionPayload,
  GuardianHardestQuestionItem,
  GuardianQuizDailyStatItem,
  GuardianQuizSessionDetailResponse,
  GuardianQuizSettingsResponse,
  GuardianQuizStatsResponse,
  MoveDirection,
  QuizOption,
  QuizQuestionResponse,
  StudentDailyQuizItemResponse,
  StudentDailyQuizResponse,
  StudentDailyQuizSessionResponse,
  SubmitDailyQuizAnswerPayload,
  SubmitDailyQuizAnswerResponse,
  UpdateQuizSettingsPayload,
} from '@/types';

interface MockStoredItem {
  id: number;
  itemOrder: number;
  questionId: number | null;
  questionText: string;
  optionA: string;
  optionB: string;
  correctOption: QuizOption;
  explanation: string | null;
  topicLabel: string | null;
  selectedOption: QuizOption | null;
  correct: boolean | null;
  answeredAt: string | null;
}

interface MockStoredSession {
  sessionId: number;
  sessionDate: string;
  status: 'IN_PROGRESS' | 'COMPLETED';
  totalQuestions: number;
  answeredCount: number;
  correctCount: number;
  cycleStartIndex: number;
  cycleEndIndex: number;
  startedAt: string;
  completedAt: string | null;
  items: MockStoredItem[];
}

const DEFAULT_STUDENT_ID = 101;
const DEFAULT_STUDENT_NAME = 'Nguyễn Minh Khôi';

let mockNextQuestionId = 13;
let mockNextSessionId = 4;
let mockNextItemId = 40;

let mockSettings: {
  enabled: boolean;
  questionsPerDay: number;
  activeWeekdays: number[];
  selectionMode: 'SEQUENTIAL_ROTATION' | 'RANDOM';
  shuffleOptions: boolean;
  rotationCursor: number;
} = {
  enabled: false,
  questionsPerDay: 10,
  activeWeekdays: [1, 2, 3, 4, 5, 6, 7],
  selectionMode: 'SEQUENTIAL_ROTATION',
  shuffleOptions: false,
  rotationCursor: 0,
};

const nowIso = new Date().toISOString();

let mockQuestions: QuizQuestionResponse[] = Array.from({ length: 12 }, (_, idx) => ({
  id: idx + 1,
  sortOrder: idx + 1,
  questionText: `Câu hỏi mẫu số ${idx + 1}: Chọn đáp án đúng nhất cho kiến thức cơ bản?`,
  optionA: idx % 2 === 0 ? 'Đáp án A (Chính xác)' : 'Đáp án A (Chưa đúng)',
  optionB: idx % 2 === 0 ? 'Đáp án B (Chưa đúng)' : 'Đáp án B (Chính xác)',
  correctOption: idx % 2 === 0 ? 'A' : 'B',
  explanation: `Giải thích ngắn cho câu hỏi mẫu số ${idx + 1}.`,
  topicLabel: idx < 6 ? 'Tiếng Anh' : 'Toán học',
  active: true,
  createdAt: nowIso,
  updatedAt: nowIso,
}));

const mockSessions: MockStoredSession[] = [];

function toPercent(numerator: number, denominator: number): number {
  if (denominator <= 0) {
    return 0;
  }
  return Math.round((numerator * 100) / denominator);
}

function buildNextBatchLabel(): string {
  const active = mockQuestions.filter((q) => q.active);
  if (active.length === 0) {
    return 'Chưa có câu hỏi hoạt động';
  }
  if (mockSettings.selectionMode === 'RANDOM') {
    const count = Math.min(mockSettings.questionsPerDay, active.length);
    return `Ngẫu nhiên ${count} / ${active.length} câu`;
  }
  const start =
    mockSettings.rotationCursor < 0 || mockSettings.rotationCursor >= active.length
      ? 0
      : mockSettings.rotationCursor;
  const end = Math.min(start + mockSettings.questionsPerDay, active.length);
  return `Câu ${start + 1}–${end} / ${active.length}`;
}

function toStudentSessionDto(s: MockStoredSession): StudentDailyQuizSessionResponse {
  const items: StudentDailyQuizItemResponse[] = s.items.map((item) => {
    const answered = item.selectedOption !== null;
    return {
      id: item.id,
      itemOrder: item.itemOrder,
      questionText: item.questionText,
      optionA: item.optionA,
      optionB: item.optionB,
      topicLabel: item.topicLabel,
      selectedOption: item.selectedOption,
      correctOption: answered ? item.correctOption : null,
      correct: answered ? item.correct : null,
      explanation: answered ? item.explanation : null,
      answeredAt: item.answeredAt,
    };
  });

  return {
    sessionId: s.sessionId,
    sessionDate: s.sessionDate,
    status: s.status,
    totalQuestions: s.totalQuestions,
    answeredCount: s.answeredCount,
    correctCount: s.correctCount,
    accuracyPercent: toPercent(s.correctCount, s.answeredCount),
    cycleStartIndex: s.cycleStartIndex,
    cycleEndIndex: s.cycleEndIndex,
    completedAt: s.completedAt,
    items,
  };
}

export const quizMockService: QuizService = {
  getTodayQuiz: async (): Promise<StudentDailyQuizResponse> => {
    const today = getTodayDateIso();
    const weekday = getWeekdayFromIsoDate(today);
    const activeQuestions = mockQuestions
      .filter((q) => q.active)
      .sort((a, b) => a.sortOrder - b.sortOrder);
    const activeToday =
      mockSettings.enabled && mockSettings.activeWeekdays.includes(weekday);
    const existing = mockSessions.find((s) => s.sessionDate === today) ?? null;

    if (!activeToday || activeQuestions.length === 0) {
      return {
        studentId: DEFAULT_STUDENT_ID,
        sessionDate: today,
        enabled: mockSettings.enabled,
        activeToday,
        requiredToday: false,
        gateLocked: false,
        nextBatchLabel: buildNextBatchLabel(),
        session:
          existing && existing.status === 'COMPLETED'
            ? toStudentSessionDto(existing)
            : null,
      };
    }

    let session = existing;
    if (!session) {
      const start =
        mockSettings.rotationCursor < 0 ||
        mockSettings.rotationCursor >= activeQuestions.length
          ? 0
          : mockSettings.rotationCursor;
      const end = Math.min(start + mockSettings.questionsPerDay, activeQuestions.length);
      const selected =
        mockSettings.selectionMode === 'RANDOM'
          ? [...activeQuestions]
              .sort(() => Math.random() - 0.5)
              .slice(0, Math.min(mockSettings.questionsPerDay, activeQuestions.length))
          : activeQuestions.slice(start, end);

      session = {
        sessionId: mockNextSessionId++,
        sessionDate: today,
        status: 'IN_PROGRESS',
        totalQuestions: selected.length,
        answeredCount: 0,
        correctCount: 0,
        cycleStartIndex: mockSettings.selectionMode === 'RANDOM' ? 0 : start,
        cycleEndIndex: mockSettings.selectionMode === 'RANDOM' ? 0 : end,
        startedAt: new Date().toISOString(),
        completedAt: null,
        items: selected.map((q, idx) => {
          const flip = mockSettings.shuffleOptions && Math.random() >= 0.5;
          return {
            id: mockNextItemId++,
            itemOrder: idx + 1,
            questionId: q.id,
            questionText: q.questionText,
            optionA: flip ? q.optionB : q.optionA,
            optionB: flip ? q.optionA : q.optionB,
            correctOption: flip ? (q.correctOption === 'A' ? 'B' : 'A') : q.correctOption,
            explanation: q.explanation,
            topicLabel: q.topicLabel,
            selectedOption: null,
            correct: null,
            answeredAt: null,
          };
        }),
      };
      mockSessions.unshift(session);
    }

    return {
      studentId: DEFAULT_STUDENT_ID,
      sessionDate: today,
      enabled: mockSettings.enabled,
      activeToday: true,
      requiredToday: true,
      gateLocked: session.status !== 'COMPLETED',
      nextBatchLabel: buildNextBatchLabel(),
      session: toStudentSessionDto(session),
    };
  },

  submitStudentAnswer: async (
    sessionId: number,
    itemId: number,
    payload: SubmitDailyQuizAnswerPayload
  ): Promise<SubmitDailyQuizAnswerResponse> => {
    const session = mockSessions.find((s) => s.sessionId === sessionId);
    if (!session) {
      throw new ApiClientError({
        message: 'Không tìm thấy phiên trắc nghiệm',
        status: 404,
        errorCode: 'SYS_404',
      });
    }
    if (session.status === 'COMPLETED') {
      throw new ApiClientError({
        message: 'Phiên trắc nghiệm hôm nay đã hoàn thành',
        status: 409,
        errorCode: 'SYS_409',
      });
    }
    const item = session.items.find((i) => i.id === itemId);
    if (!item) {
      throw new ApiClientError({
        message: 'Không tìm thấy câu hỏi',
        status: 404,
        errorCode: 'SYS_404',
      });
    }
    if (item.selectedOption !== null) {
      throw new ApiClientError({
        message: 'Câu hỏi này đã được trả lời rồi',
        status: 409,
        errorCode: 'SYS_409',
      });
    }
    const firstUnanswered = session.items.find((i) => i.selectedOption === null);
    if (firstUnanswered && firstUnanswered.id !== item.id) {
      throw new ApiClientError({
        message: 'Vui lòng trả lời lần lượt từng câu hỏi',
        status: 400,
        errorCode: 'SYS_400',
      });
    }

    const correct = payload.selectedOption === item.correctOption;
    item.selectedOption = payload.selectedOption;
    item.correct = correct;
    item.answeredAt = new Date().toISOString();

    session.answeredCount = session.items.filter((i) => i.selectedOption !== null).length;
    session.correctCount = session.items.filter((i) => i.correct === true).length;

    const completedNow = session.answeredCount >= session.totalQuestions;
    if (completedNow) {
      session.status = 'COMPLETED';
      session.completedAt = new Date().toISOString();
      if (mockSettings.selectionMode === 'SEQUENTIAL_ROTATION') {
        const activeCount = mockQuestions.filter((q) => q.active).length;
        mockSettings.rotationCursor =
          activeCount <= 0 || session.cycleEndIndex >= activeCount
            ? 0
            : session.cycleEndIndex;
      }
    }

    return {
      sessionId: session.sessionId,
      itemId: item.id,
      selectedOption: payload.selectedOption,
      correctOption: item.correctOption,
      correct,
      explanation: item.explanation,
      answeredCount: session.answeredCount,
      correctCount: session.correctCount,
      totalQuestions: session.totalQuestions,
      accuracyPercent: toPercent(session.correctCount, session.answeredCount),
      sessionStatus: session.status,
      gateLocked: !completedNow,
      completedAt: session.completedAt,
    };
  },

  getGuardianSettings: async (studentId?: number | null): Promise<GuardianQuizSettingsResponse> => {
    const activeCount = mockQuestions.filter((q) => q.active).length;
    return {
      studentId: studentId ?? DEFAULT_STUDENT_ID,
      enabled: mockSettings.enabled,
      questionsPerDay: mockSettings.questionsPerDay,
      activeWeekdays: [...mockSettings.activeWeekdays],
      selectionMode: mockSettings.selectionMode,
      shuffleOptions: mockSettings.shuffleOptions,
      rotationCursor: mockSettings.rotationCursor,
      totalQuestionCount: mockQuestions.length,
      activeQuestionCount: activeCount,
      nextBatchLabel: buildNextBatchLabel(),
    };
  },

  updateGuardianSettings: async (
    payload: UpdateQuizSettingsPayload,
    studentId?: number | null
  ): Promise<GuardianQuizSettingsResponse> => {
    const activeCount = mockQuestions.filter((q) => q.active).length;
    mockSettings = {
      enabled: payload.enabled,
      questionsPerDay: payload.questionsPerDay,
      activeWeekdays: [...new Set(payload.activeWeekdays)].sort((a, b) => a - b),
      selectionMode: payload.selectionMode,
      shuffleOptions: payload.shuffleOptions,
      rotationCursor:
        payload.resetCursor || mockSettings.rotationCursor >= activeCount
          ? 0
          : mockSettings.rotationCursor,
    };
    return quizMockService.getGuardianSettings(studentId);
  },

  listGuardianQuestions: async (): Promise<QuizQuestionResponse[]> =>
    [...mockQuestions].sort((a, b) => a.sortOrder - b.sortOrder),

  createGuardianQuestion: async (
    payload: CreateQuizQuestionPayload
  ): Promise<QuizQuestionResponse> => {
    const now = new Date().toISOString();
    const created: QuizQuestionResponse = {
      id: mockNextQuestionId++,
      sortOrder: mockQuestions.length + 1,
      questionText: payload.questionText.trim(),
      optionA: payload.optionA.trim(),
      optionB: payload.optionB.trim(),
      correctOption: payload.correctOption,
      explanation: payload.explanation?.trim() || null,
      topicLabel: payload.topicLabel?.trim() || null,
      active: payload.active ?? true,
      createdAt: now,
      updatedAt: now,
    };
    mockQuestions.push(created);
    return created;
  },

  bulkCreateGuardianQuestions: async (
    payload: BulkCreateQuizQuestionsPayload
  ): Promise<QuizQuestionResponse[]> => {
    const added: QuizQuestionResponse[] = [];
    for (const q of payload.questions) {
      added.push(await quizMockService.createGuardianQuestion(q));
    }
    return added;
  },

  updateGuardianQuestion: async (
    questionId: number,
    payload: CreateQuizQuestionPayload
  ): Promise<QuizQuestionResponse> => {
    const target = mockQuestions.find((q) => q.id === questionId);
    if (!target) {
      throw new ApiClientError({
        message: 'Không tìm thấy câu hỏi',
        status: 404,
        errorCode: 'SYS_404',
      });
    }
    target.questionText = payload.questionText.trim();
    target.optionA = payload.optionA.trim();
    target.optionB = payload.optionB.trim();
    target.correctOption = payload.correctOption;
    target.explanation = payload.explanation?.trim() || null;
    target.topicLabel = payload.topicLabel?.trim() || null;
    if (payload.active !== undefined) {
      target.active = payload.active;
    }
    target.updatedAt = new Date().toISOString();
    return { ...target };
  },

  moveGuardianQuestion: async (
    questionId: number,
    direction: MoveDirection
  ): Promise<QuizQuestionResponse[]> => {
    const sorted = [...mockQuestions].sort((a, b) => a.sortOrder - b.sortOrder);
    const idx = sorted.findIndex((q) => q.id === questionId);
    if (idx < 0) {
      return sorted;
    }
    const swapIdx = direction === 'UP' ? idx - 1 : idx + 1;
    if (swapIdx >= 0 && swapIdx < sorted.length) {
      [sorted[idx], sorted[swapIdx]] = [sorted[swapIdx], sorted[idx]];
      sorted.forEach((q, i) => {
        q.sortOrder = i + 1;
      });
      mockQuestions = sorted;
    }
    return [...mockQuestions];
  },

  deleteGuardianQuestion: async (questionId: number): Promise<void> => {
    mockQuestions = mockQuestions
      .filter((q) => q.id !== questionId)
      .map((q, idx) => ({ ...q, sortOrder: idx + 1 }));
    const activeCount = mockQuestions.filter((q) => q.active).length;
    if (mockSettings.rotationCursor >= activeCount) {
      mockSettings.rotationCursor = 0;
    }
  },

  getGuardianStats: async (params?: {
    studentId?: number | null;
    days?: number;
  }): Promise<GuardianQuizStatsResponse> => {
    const totalAnswered = mockSessions.reduce((acc, s) => acc + s.answeredCount, 0);
    const totalCorrect = mockSessions.reduce((acc, s) => acc + s.correctCount, 0);
    const dailyStats: GuardianQuizDailyStatItem[] = mockSessions.map((s) => ({
      sessionId: s.sessionId,
      date: s.sessionDate,
      status: s.status,
      totalQuestions: s.totalQuestions,
      answeredCount: s.answeredCount,
      correctCount: s.correctCount,
      accuracyPercent: toPercent(s.correctCount, s.answeredCount),
      cycleLabel:
        s.cycleStartIndex === 0 && s.cycleEndIndex === 0
          ? 'Ngẫu nhiên'
          : `Câu ${s.cycleStartIndex + 1}–${s.cycleEndIndex}`,
      completedAt: s.completedAt,
    }));

    const wrongMap = new Map<string, GuardianHardestQuestionItem>();
    for (const s of mockSessions) {
      for (const item of s.items) {
        if (item.selectedOption === null) continue;
        const key = String(item.questionId ?? item.questionText);
        const existing = wrongMap.get(key) ?? {
          questionId: item.questionId,
          questionText: item.questionText,
          topicLabel: item.topicLabel,
          attemptCount: 0,
          wrongCount: 0,
          accuracyPercent: 100,
        };
        existing.attemptCount += 1;
        if (item.correct === false) {
          existing.wrongCount += 1;
        }
        existing.accuracyPercent = toPercent(
          existing.attemptCount - existing.wrongCount,
          existing.attemptCount
        );
        wrongMap.set(key, existing);
      }
    }

    const hardestQuestions = Array.from(wrongMap.values())
      .filter((x) => x.wrongCount > 0)
      .sort((a, b) => b.wrongCount - a.wrongCount)
      .slice(0, 5);

    const today = getTodayDateIso();
    const todayStatus =
      mockSessions.find((s) => s.sessionDate === today)?.status ?? null;

    return {
      studentId: params?.studentId ?? DEFAULT_STUDENT_ID,
      studentName: DEFAULT_STUDENT_NAME,
      totalQuestionCount: mockQuestions.length,
      activeQuestionCount: mockQuestions.filter((q) => q.active).length,
      totalCompletedSessions: mockSessions.filter((s) => s.status === 'COMPLETED')
        .length,
      totalQuestionsAnswered: totalAnswered,
      totalCorrectAnswers: totalCorrect,
      averageAccuracyPercent: toPercent(totalCorrect, totalAnswered),
      quizStreakDays: todayStatus === 'COMPLETED' ? 1 : 0,
      todayStatus,
      dailyStats,
      hardestQuestions,
    };
  },

  getGuardianSessionDetail: async (
    sessionId: number,
    studentId?: number | null
  ): Promise<GuardianQuizSessionDetailResponse> => {
    const s = mockSessions.find((x) => x.sessionId === sessionId);
    if (!s) {
      throw new ApiClientError({
        message: 'Không tìm thấy phiên làm bài',
        status: 404,
        errorCode: 'SYS_404',
      });
    }
    return {
      sessionId: s.sessionId,
      studentId: studentId ?? DEFAULT_STUDENT_ID,
      studentName: DEFAULT_STUDENT_NAME,
      sessionDate: s.sessionDate,
      status: s.status,
      totalQuestions: s.totalQuestions,
      answeredCount: s.answeredCount,
      correctCount: s.correctCount,
      accuracyPercent: toPercent(s.correctCount, s.answeredCount),
      cycleLabel:
        s.cycleStartIndex === 0 && s.cycleEndIndex === 0
          ? 'Ngẫu nhiên'
          : `Câu ${s.cycleStartIndex + 1}–${s.cycleEndIndex}`,
      startedAt: s.startedAt,
      completedAt: s.completedAt,
      items: s.items.map((item) => ({
        itemId: item.id,
        itemOrder: item.itemOrder,
        questionId: item.questionId,
        questionText: item.questionText,
        optionA: item.optionA,
        optionB: item.optionB,
        topicLabel: item.topicLabel,
        selectedOption: item.selectedOption,
        correctOption: item.correctOption,
        correct: item.correct,
        explanation: item.explanation,
        answeredAt: item.answeredAt,
      })),
    };
  },
};
