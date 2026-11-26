import { apiDelete, apiGet, apiPost } from '@/lib/api-client';
import type { StudyService } from '@/services/study.service';
import type {
  NextDayStudyPlanResponse,
  StudentCustomSubmitPayload,
  StudentSkipRequestPayload,
  StudentStreakResponse,
  StudyPlanHistoryItem,
  StudySubmissionResponse,
  StudyTaskDetailResponse,
  StudyTaskSummaryResponse,
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

  requestSkip: (taskId: number, request: StudentSkipRequestPayload) =>
    apiPost<StudyTaskSummaryResponse>(
      `/api/student/tasks/${taskId}/skip-request`,
      request
    ),

  cancelSkipRequest: (taskId: number) =>
    apiDelete<StudyTaskSummaryResponse>(
      `/api/student/tasks/${taskId}/skip-request`
    ),

  submitCustomTask: (taskId: number, request: StudentCustomSubmitPayload) =>
    apiPost<StudyTaskSummaryResponse>(
      `/api/student/tasks/${taskId}/custom-submission`,
      request
    ),
};
