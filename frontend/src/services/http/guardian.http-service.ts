import { apiGet } from '@/lib/api-client';
import type {
  GuardianDashboardParams,
  GuardianService,
} from '@/services/guardian.service';
import type {
  GuardianDashboardResponse,
  GuardianSubmissionDetailResponse,
} from '@/types';

export const guardianHttpService: GuardianService = {
  getDashboard: (params?: GuardianDashboardParams) => {
    const query = new URLSearchParams();
    if (params?.date) {
      query.set('date', params.date);
    }
    if (params?.subjectCode && params.subjectCode !== 'ALL') {
      query.set('subjectCode', params.subjectCode);
    }
    const qs = query.toString();
    return apiGet<GuardianDashboardResponse>(
      `/api/guardian/dashboard${qs ? `?${qs}` : ''}`
    );
  },

  getSubmissionDetail: (submissionId: number) =>
    apiGet<GuardianSubmissionDetailResponse>(
      `/api/guardian/submissions/${submissionId}`
    ),
};
