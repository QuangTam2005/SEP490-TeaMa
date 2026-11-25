import type {
  NextDayStudyPlanResponse,
  StudentStreakResponse,
  StudyPlanHistoryItem,
  StudySubmissionResponse,
  StudyTaskDetailResponse,
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
}
