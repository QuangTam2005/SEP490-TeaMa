import { apiGet, apiPost } from '@/lib/api-client';
import type { StudyService } from '@/services/study.service';
import type {
  NextDayStudyPlanResponse,
  StudentStreakResponse,
  StudyPlanHistoryItem,
  StudySubmissionResponse,
  StudyTaskDetailResponse,
  SubmitStudyRequest,
} from '@/types';

export const studyHttpService: StudyService = {
  getNextDayPlan: () =>
    apiGet<NextDayStudyPlanResponse>('/api/student/plans/next-day'),

  getPlanHistory: (from?: string, to?: string) =>
    apiGet<StudyPlanHistoryItem[]>(
      '/api/student/plans/history',
      from || to ? { params: { ...(from ? { from } : {}), ...(to ? { to } : {}) } } : undefined
    ),

  getStreak: () => apiGet<StudentStreakResponse>('/api/student/streak'),

  getTaskDetail: (taskId: number) =>
    apiGet<StudyTaskDetailResponse>(`/api/student/tasks/${taskId}`),

  submitTask: (taskId: number, request: SubmitStudyRequest) =>
    apiPost<StudySubmissionResponse>(
      `/api/student/tasks/${taskId}/submissions`,
      request,
      {
        headers: {
          'Idempotency-Key': request.idempotencyKey,
        },
      }
    ),

  retryEvaluation: (submissionId: number) =>
    apiPost<StudySubmissionResponse>(
      `/api/student/submissions/${submissionId}/retry-evaluation`,
      {}
    ),
};
