import { apiDelete, apiGet, apiPost, apiPut } from '@/lib/api-client';
import type { QuizService } from '@/services/quiz.service';
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

function buildStudentQuery(studentId?: number | null, extra?: Record<string, string | number | undefined>) {
  const query = new URLSearchParams();
  if (studentId) {
    query.set('studentId', String(studentId));
  }
  if (extra) {
    for (const [k, v] of Object.entries(extra)) {
      if (v !== undefined && v !== null) {
        query.set(k, String(v));
      }
    }
  }
  const qs = query.toString();
  return qs ? `?${qs}` : '';
}

export const quizHttpService: QuizService = {
  getTodayQuiz: () => apiGet<StudentDailyQuizResponse>('/api/student/daily-quiz/today'),

  submitStudentAnswer: (
    sessionId: number,
    itemId: number,
    payload: SubmitDailyQuizAnswerPayload
  ) =>
    apiPost<SubmitDailyQuizAnswerResponse>(
      `/api/student/daily-quiz/sessions/${sessionId}/items/${itemId}/answer`,
      payload
    ),

  getGuardianSettings: (studentId?: number | null) =>
    apiGet<GuardianQuizSettingsResponse>(
      `/api/guardian/quiz/settings${buildStudentQuery(studentId)}`
    ),

  updateGuardianSettings: (
    payload: UpdateQuizSettingsPayload,
    studentId?: number | null
  ) =>
    apiPut<GuardianQuizSettingsResponse>(
      `/api/guardian/quiz/settings${buildStudentQuery(studentId)}`,
      payload
    ),

  listGuardianQuestions: (studentId?: number | null) =>
    apiGet<QuizQuestionResponse[]>(
      `/api/guardian/quiz/questions${buildStudentQuery(studentId)}`
    ),

  createGuardianQuestion: (
    payload: CreateQuizQuestionPayload,
    studentId?: number | null
  ) =>
    apiPost<QuizQuestionResponse>(
      `/api/guardian/quiz/questions${buildStudentQuery(studentId)}`,
      payload
    ),

  bulkCreateGuardianQuestions: (
    payload: BulkCreateQuizQuestionsPayload,
    studentId?: number | null
  ) =>
    apiPost<QuizQuestionResponse[]>(
      `/api/guardian/quiz/questions/bulk${buildStudentQuery(studentId)}`,
      payload
    ),

  updateGuardianQuestion: (
    questionId: number,
    payload: CreateQuizQuestionPayload,
    studentId?: number | null
  ) =>
    apiPut<QuizQuestionResponse>(
      `/api/guardian/quiz/questions/${questionId}${buildStudentQuery(studentId)}`,
      payload
    ),

  moveGuardianQuestion: (
    questionId: number,
    direction: MoveDirection,
    studentId?: number | null
  ) =>
    apiPost<QuizQuestionResponse[]>(
      `/api/guardian/quiz/questions/${questionId}/move${buildStudentQuery(studentId)}`,
      { direction }
    ),

  deleteGuardianQuestion: (questionId: number, studentId?: number | null) =>
    apiDelete<void>(
      `/api/guardian/quiz/questions/${questionId}${buildStudentQuery(studentId)}`
    ),

  getGuardianStats: (params?: { studentId?: number | null; days?: number }) =>
    apiGet<GuardianQuizStatsResponse>(
      `/api/guardian/quiz/stats${buildStudentQuery(params?.studentId, { days: params?.days })}`
    ),

  getGuardianSessionDetail: (sessionId: number, studentId?: number | null) =>
    apiGet<GuardianQuizSessionDetailResponse>(
      `/api/guardian/quiz/sessions/${sessionId}${buildStudentQuery(studentId)}`
    ),
};
