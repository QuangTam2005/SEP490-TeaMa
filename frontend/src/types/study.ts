import type { AIEvaluationResponse } from './ai';
import type { SubjectCode } from './timetable';

export type StudyTaskType = 'REVIEW' | 'PREVIEW';

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
