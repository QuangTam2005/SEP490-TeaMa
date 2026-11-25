import type { AIEvaluationResponse } from './ai';
import type {
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
  studentName: string;
  targetDate: string;
  subjectCode: SubjectCode;
  subjectName: string;
  taskType: StudyTaskType;
  status: StudyTaskStatus;
  revisionCount: number;
  versions: StudySubmissionResponse[];
}
