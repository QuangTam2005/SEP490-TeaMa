import type {
  NextDayStudyPlanResponse,
  StudySubmissionResponse,
  StudyTaskDetailResponse,
  SubmitStudyRequest,
} from '@/types';

export interface StudyService {
  getNextDayPlan(): Promise<NextDayStudyPlanResponse>;
  getTaskDetail(taskId: number): Promise<StudyTaskDetailResponse>;
  submitTask(
    taskId: number,
    request: SubmitStudyRequest
  ): Promise<StudySubmissionResponse>;
  retryEvaluation(submissionId: number): Promise<StudySubmissionResponse>;
}
