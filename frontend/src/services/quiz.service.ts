import type {
  BulkCreateQuizQuestionsPayload,
  CreateQuizQuestionPayload,
  GuardianQuizSessionDetailResponse,
  GuardianQuizSettingsResponse,
  GuardianQuizStatsResponse,
  MoveDirection,
  QuizQuestionResponse,
  StudentDailyQuizResponse,
  SubmitDailyQuizAnswerPayload,
  SubmitDailyQuizAnswerResponse,
  UpdateQuizSettingsPayload,
} from '@/types';

export interface QuizService {
  // Student
  getTodayQuiz: () => Promise<StudentDailyQuizResponse>;
  submitStudentAnswer: (
    sessionId: number,
    itemId: number,
    payload: SubmitDailyQuizAnswerPayload
  ) => Promise<SubmitDailyQuizAnswerResponse>;

  // Guardian
  getGuardianSettings: (studentId?: number | null) => Promise<GuardianQuizSettingsResponse>;
  updateGuardianSettings: (
    payload: UpdateQuizSettingsPayload,
    studentId?: number | null
  ) => Promise<GuardianQuizSettingsResponse>;
  listGuardianQuestions: (studentId?: number | null) => Promise<QuizQuestionResponse[]>;
  createGuardianQuestion: (
    payload: CreateQuizQuestionPayload,
    studentId?: number | null
  ) => Promise<QuizQuestionResponse>;
  bulkCreateGuardianQuestions: (
    payload: BulkCreateQuizQuestionsPayload,
    studentId?: number | null
  ) => Promise<QuizQuestionResponse[]>;
  updateGuardianQuestion: (
    questionId: number,
    payload: CreateQuizQuestionPayload,
    studentId?: number | null
  ) => Promise<QuizQuestionResponse>;
  moveGuardianQuestion: (
    questionId: number,
    direction: MoveDirection,
    studentId?: number | null
  ) => Promise<QuizQuestionResponse[]>;
  deleteGuardianQuestion: (
    questionId: number,
    studentId?: number | null
  ) => Promise<void>;
  getGuardianStats: (
    params?: { studentId?: number | null; days?: number }
  ) => Promise<GuardianQuizStatsResponse>;
  getGuardianSessionDetail: (
    sessionId: number,
    studentId?: number | null
  ) => Promise<GuardianQuizSessionDetailResponse>;
}
