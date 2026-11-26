import type { AIEvaluationResponse } from './ai';
import type { SubjectCode } from './timetable';

export type StudyTaskType = 'REVIEW' | 'PREVIEW';

export type SkipRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export type TaskAssignmentMode = 'STANDARD_AI' | 'CUSTOM_DRIVE';

export type StudyTaskStatus =
  | 'NOT_STARTED'
  | 'IN_PROGRESS'
  | 'SUBMITTED'
  | 'EVALUATING'
  | 'EVALUATED'
  | 'EVALUATION_FAILED'
  | 'BLOCKED'
  | 'SKIPPED';

export interface StudySubmissionResponse {
  id: number;
  taskId: number;
  version: number;
  isCurrent: boolean;
  lessonTitle: string;
  content: string;
  charCount: number;
  submittedAt: string;
  evaluation: AIEvaluationResponse | null;
  lastError: string | null;
}

export interface StudyTaskSummaryResponse {
  id: number;
  subjectCode: SubjectCode;
  subjectName: string;
  taskType: StudyTaskType;
  status: StudyTaskStatus;
  revisionCount: number;
  maxRevisions: number;
  currentLessonTitle: string | null;
  latestEvaluationStatus: AIEvaluationResponse['overallStatus'] | null;
  guardianAcknowledgedAt?: string | null;
  guardianNote?: string | null;
  skipRequestStatus?: SkipRequestStatus | null;
  skipRequestReason?: string | null;
  skipRequestedAt?: string | null;
  skipReviewedAt?: string | null;
  assignmentMode?: TaskAssignmentMode;
  customTaskTitle?: string | null;
  customTaskDescription?: string | null;
  customDriveFolderUrl?: string | null;
  customAssignedAt?: string | null;
  customSubmissionNote?: string | null;
  customSubmissionUrl?: string | null;
  customSubmittedAt?: string | null;
}

export interface StudentSkipRequestPayload {
  reason: string;
}

export interface StudentCustomSubmitPayload {
  submissionNote: string;
  driveFileUrl?: string | null;
}

export interface NextDaySubjectPair {
  subjectCode: SubjectCode;
  subjectName: string;
  periodNote: string | null;
  reviewTask: StudyTaskSummaryResponse;
  previewTask: StudyTaskSummaryResponse;
}

export interface NextDayStudyPlanResponse {
  planId: number | null;
  targetDate: string;
  weekdayLabel: string;
  hasSchedule: boolean;
  subjects: NextDaySubjectPair[];
}

export interface StudyTaskDetailResponse {
  task: StudyTaskSummaryResponse;
  targetDate: string;
  reviewCompleted: boolean;
  submissions: StudySubmissionResponse[];
}

export interface SubmitStudyRequest {
  lessonTitle: string;
  content: string;
  idempotencyKey: string;
}

export interface StudyPlanHistoryItem {
  planId: number;
  targetDate: string;
  weekdayLabel: string;
  totalTasks: number;
  completedTasks: number;
  subjects: NextDaySubjectPair[];
}

export interface StudentStreakResponse {
  currentStreak: number;
  longestStreak: number;
  totalCompletedDays: number;
  lastActiveDate: string | null;
}
