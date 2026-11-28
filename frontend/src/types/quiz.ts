export type QuizOption = 'A' | 'B';

export type QuizSelectionMode = 'SEQUENTIAL_ROTATION' | 'RANDOM';

export type DailyQuizSessionStatus = 'IN_PROGRESS' | 'COMPLETED';

export type MoveDirection = 'UP' | 'DOWN';

export interface QuizQuestionResponse {
  id: number;
  sortOrder: number;
  questionText: string;
  optionA: string;
  optionB: string;
  correctOption: QuizOption;
  explanation: string | null;
  topicLabel: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateQuizQuestionPayload {
  questionText: string;
  optionA: string;
  optionB: string;
  correctOption: QuizOption;
  explanation?: string | null;
  topicLabel?: string | null;
  active?: boolean;
}

export interface BulkCreateQuizQuestionsPayload {
  questions: CreateQuizQuestionPayload[];
}

export interface GuardianQuizSettingsResponse {
  studentId: number;
  enabled: boolean;
  questionsPerDay: number;
  activeWeekdays: number[];
  selectionMode: QuizSelectionMode;
  shuffleOptions: boolean;
  rotationCursor: number;
  totalQuestionCount: number;
  activeQuestionCount: number;
  nextBatchLabel: string;
}

export interface UpdateQuizSettingsPayload {
  enabled: boolean;
  questionsPerDay: number;
  activeWeekdays: number[];
  selectionMode: QuizSelectionMode;
  shuffleOptions: boolean;
  resetCursor?: boolean;
}

export interface StudentDailyQuizItemResponse {
  id: number;
  itemOrder: number;
  questionText: string;
  optionA: string;
  optionB: string;
  topicLabel: string | null;
  selectedOption: QuizOption | null;
  correctOption: QuizOption | null;
  correct: boolean | null;
  explanation: string | null;
  answeredAt: string | null;
}

export interface StudentDailyQuizSessionResponse {
  sessionId: number;
  sessionDate: string;
  status: DailyQuizSessionStatus;
  totalQuestions: number;
  answeredCount: number;
  correctCount: number;
  accuracyPercent: number;
  cycleStartIndex: number;
  cycleEndIndex: number;
  completedAt: string | null;
  items: StudentDailyQuizItemResponse[];
}

export interface StudentDailyQuizResponse {
  studentId: number;
  sessionDate: string;
  enabled: boolean;
  activeToday: boolean;
  requiredToday: boolean;
  gateLocked: boolean;
  nextBatchLabel: string;
  session: StudentDailyQuizSessionResponse | null;
}

export interface SubmitDailyQuizAnswerPayload {
  selectedOption: QuizOption;
}

export interface SubmitDailyQuizAnswerResponse {
  sessionId: number;
  itemId: number;
  selectedOption: QuizOption;
  correctOption: QuizOption;
  correct: boolean;
  explanation: string | null;
  answeredCount: number;
  correctCount: number;
  totalQuestions: number;
  accuracyPercent: number;
  sessionStatus: DailyQuizSessionStatus;
  gateLocked: boolean;
  completedAt: string | null;
}

export interface GuardianQuizDailyStatItem {
  sessionId: number;
  date: string;
  status: DailyQuizSessionStatus;
  totalQuestions: number;
  answeredCount: number;
  correctCount: number;
  accuracyPercent: number;
  cycleLabel: string;
  completedAt: string | null;
}

export interface GuardianHardestQuestionItem {
  questionId: number | null;
  questionText: string;
  topicLabel: string | null;
  attemptCount: number;
  wrongCount: number;
  accuracyPercent: number;
}

export interface GuardianQuizStatsResponse {
  studentId: number;
  studentName: string;
  totalQuestionCount: number;
  activeQuestionCount: number;
  totalCompletedSessions: number;
  totalQuestionsAnswered: number;
  totalCorrectAnswers: number;
  averageAccuracyPercent: number;
  quizStreakDays: number;
  todayStatus: DailyQuizSessionStatus | null;
  dailyStats: GuardianQuizDailyStatItem[];
  hardestQuestions: GuardianHardestQuestionItem[];
}

export interface GuardianQuizSessionDetailItem {
  itemId: number;
  itemOrder: number;
  questionId: number | null;
  questionText: string;
  optionA: string;
  optionB: string;
  topicLabel: string | null;
  selectedOption: QuizOption | null;
  correctOption: QuizOption;
  correct: boolean | null;
  explanation: string | null;
  answeredAt: string | null;
}

export interface GuardianQuizSessionDetailResponse {
  sessionId: number;
  studentId: number;
  studentName: string;
  sessionDate: string;
  status: DailyQuizSessionStatus;
  totalQuestions: number;
  answeredCount: number;
  correctCount: number;
  accuracyPercent: number;
  cycleLabel: string;
  startedAt: string;
  completedAt: string | null;
  items: GuardianQuizSessionDetailItem[];
}
