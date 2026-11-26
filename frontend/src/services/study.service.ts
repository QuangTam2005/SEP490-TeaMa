import type {
  NextDayStudyPlanResponse,
  StudentSkipRequestPayload,
  StudentStreakResponse,
  StudyPlanHistoryItem,
  StudySubmissionResponse,
  StudyTaskDetailResponse,
  StudyTaskSummaryResponse,
  SubmitStudyRequest,
} from '@/types';

export interface StudyService {
  getNextDayPlan(): Promise<NextDayStudyPlanResponse>;
  getPlanHistory(from?: string, to?: string): Promise<StudyPlanHistoryItem[]>;
  getStreak(): Promise<StudentStreakResponse>;
  getTaskDetail(taskId: number): Promise<StudyTaskDetailResponse>;
  submitTask(
    taskId: number,
    request: SubmitStudyRequest
  ): Promise<StudySubmissionResponse>;
  retryEvaluation(submissionId: number): Promise<StudySubmissionResponse>;
  requestSkip(
    taskId: number,
    request: StudentSkipRequestPayload
  ): Promise<StudyTaskSummaryResponse>;
  cancelSkipRequest(taskId: number): Promise<StudyTaskSummaryResponse>;
}
