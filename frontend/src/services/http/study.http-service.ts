import { apiGet, apiPost } from '@/lib/api-client';
import type { StudyService } from '@/services/study.service';
import type {
  NextDayStudyPlanResponse,
  StudySubmissionResponse,
  StudyTaskDetailResponse,
  SubmitStudyRequest,
} from '@/types';

export const studyHttpService: StudyService = {
  getNextDayPlan: () =>
    apiGet<NextDayStudyPlanResponse>('/api/student/plans/next-day'),

  getTaskDetail: (taskId: number) =>
    apiGet<StudyTaskDetailResponse>(`/api/student/tasks/${taskId}`),

  submitTask: (taskId: number, request: SubmitStudyRequest) =>
    apiPost<StudySubmissionResponse>(
      `/api/student/tasks/${taskId}/submissions`,
      request
    ),

  retryEvaluation: (submissionId: number) =>
    apiPost<StudySubmissionResponse>(
      `/api/student/submissions/${submissionId}/retry-evaluation`,
      {}
    ),
};
