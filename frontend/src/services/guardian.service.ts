import type {
  GuardianAcknowledgeRequest,
  GuardianAcknowledgeResponse,
  GuardianDashboardResponse,
  GuardianSkipReviewRequest,
  GuardianSkipTaskRequest,
  GuardianSkipTaskResponse,
  GuardianSubmissionDetailResponse,
  GuardianWeeklySummaryResponse,
  SubjectCode,
} from '@/types';

export interface GuardianDashboardParams {
  date?: string;
  subjectCode?: SubjectCode | 'ALL';
  studentId?: number;
}

export interface GuardianWeeklyParams {
  weekStart?: string;
  studentId?: number;
}

export interface GuardianExportParams {
  from?: string;
  to?: string;
  studentId?: number;
}

export interface GuardianService {
  getDashboard(
    params?: GuardianDashboardParams
  ): Promise<GuardianDashboardResponse>;
  getSubmissionDetail(
    submissionId: number
  ): Promise<GuardianSubmissionDetailResponse>;
  acknowledgeTask(
    taskId: number,
    request?: GuardianAcknowledgeRequest
  ): Promise<GuardianAcknowledgeResponse>;
  skipTask(
    taskId: number,
    request: GuardianSkipTaskRequest
  ): Promise<GuardianSkipTaskResponse>;
  reviewSkipRequest(
    taskId: number,
    request: GuardianSkipReviewRequest
  ): Promise<GuardianSkipTaskResponse>;
  getWeeklySummary(
    params?: GuardianWeeklyParams
  ): Promise<GuardianWeeklySummaryResponse>;
  exportReport(params?: GuardianExportParams): Promise<Blob>;
}
