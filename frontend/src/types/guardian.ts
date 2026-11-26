import type { AIEvaluationResponse } from './ai';
import type {
  SkipRequestStatus,
  StudySubmissionResponse,
  StudyTaskStatus,
  StudyTaskType,
} from './study';
import type { SubjectCode } from './timetable';

export interface LinkCodeResponse {
  linkCode: string;
  expiresAt: string;
  status: 'PENDING' | 'ACTIVE' | 'REVOKED';
  linkedUserName: string | null;
}

export interface LinkedChildResponse {
  studentId: number;
  studentUserId: number;
  displayName: string;
  grade: number;
  linkedAt: string;
}

export interface GuardianTaskItem {
  taskId: number;
  currentSubmissionId: number | null;
  targetDate: string;
  subjectCode: SubjectCode;
  subjectName: string;
  taskType: StudyTaskType;
  status: StudyTaskStatus;
  lessonTitle: string | null;
  revisionCount: number;
  evaluation: Pick<
    AIEvaluationResponse,
    'accuracy' | 'completeness' | 'overallStatus' | 'feedback' | 'evaluatedAt'
  > | null;
  guardianAcknowledgedAt?: string | null;
  guardianNote?: string | null;
  skipRequestStatus?: SkipRequestStatus | null;
  skipRequestReason?: string | null;
  skipRequestedAt?: string | null;
  skipReviewedAt?: string | null;
}

export interface GuardianDashboardResponse {
  linkedStudentName: string | null;
  grade: number;
  selectedDate: string;
  selectedSubject: SubjectCode | 'ALL';
  totalTasks: number;
  completedTasks: number;
  evaluatingTasks: number;
  items: GuardianTaskItem[];
}

export interface GuardianSubmissionDetailResponse {
  submissionId: number;
  taskId?: number | null;
  studentName: string;
  targetDate: string;
  subjectCode: SubjectCode;
  subjectName: string;
  taskType: StudyTaskType;
  status: StudyTaskStatus;
  revisionCount: number;
  guardianAcknowledgedAt?: string | null;
  guardianNote?: string | null;
  versions: StudySubmissionResponse[];
}

export interface GuardianAcknowledgeRequest {
  note?: string | null;
}

export interface GuardianAcknowledgeResponse {
  taskId: number;
  acknowledgedAt: string;
  note: string | null;
}

export interface GuardianSkipTaskRequest {
  skipped: boolean;
  note?: string | null;
}

export interface GuardianSkipReviewRequest {
  approved: boolean;
  note?: string | null;
}

export interface GuardianSkipTaskResponse {
  taskId: number;
  status: StudyTaskStatus;
  guardianNote: string | null;
  skipRequestStatus?: SkipRequestStatus | null;
  skipRequestReason?: string | null;
  skipRequestedAt?: string | null;
  skipReviewedAt?: string | null;
}

export interface GuardianWeeklyDaySummary {
  date: string;
  weekdayLabel: string;
  totalTasks: number;
  completedTasks: number;
  acknowledgedTasks: number;
}

export interface GuardianWeeklySubjectStat {
  subjectCode: SubjectCode;
  subjectName: string;
  totalTasks: number;
  completedTasks: number;
}

export interface GuardianWeeklySummaryResponse {
  studentId: number;
  studentName: string;
  grade: number;
  weekStart: string;
  weekEnd: string;
  totalTasks: number;
  completedTasks: number;
  acknowledgedTasks: number;
  days: GuardianWeeklyDaySummary[];
  subjects: GuardianWeeklySubjectStat[];
}
