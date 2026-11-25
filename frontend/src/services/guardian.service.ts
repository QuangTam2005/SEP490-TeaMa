import type {
  GuardianDashboardResponse,
  GuardianSubmissionDetailResponse,
  SubjectCode,
} from '@/types';

export interface GuardianDashboardParams {
  date?: string;
  subjectCode?: SubjectCode | 'ALL';
}

export interface GuardianService {
  getDashboard(
    params?: GuardianDashboardParams
  ): Promise<GuardianDashboardResponse>;
  getSubmissionDetail(
    submissionId: number
  ): Promise<GuardianSubmissionDetailResponse>;
}
