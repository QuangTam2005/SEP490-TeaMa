import { apiClient, apiGet, apiPost } from '@/lib/api-client';
import type {
  GuardianDashboardParams,
  GuardianExportParams,
  GuardianService,
  GuardianWeeklyParams,
} from '@/services/guardian.service';
import type {
  GuardianAcknowledgeRequest,
  GuardianAcknowledgeResponse,
  GuardianDashboardResponse,
  GuardianSubmissionDetailResponse,
  GuardianWeeklySummaryResponse,
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
    if (params?.studentId) {
      query.set('studentId', String(params.studentId));
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

  acknowledgeTask: (taskId: number, request?: GuardianAcknowledgeRequest) =>
    apiPost<GuardianAcknowledgeResponse>(
      `/api/guardian/tasks/${taskId}/acknowledge`,
      request ?? {}
    ),

  getWeeklySummary: (params?: GuardianWeeklyParams) => {
    const query = new URLSearchParams();
    if (params?.weekStart) {
      query.set('weekStart', params.weekStart);
    }
    if (params?.studentId) {
      query.set('studentId', String(params.studentId));
    }
    const qs = query.toString();
    return apiGet<GuardianWeeklySummaryResponse>(
      `/api/guardian/weekly${qs ? `?${qs}` : ''}`
    );
  },

  exportReport: async (params?: GuardianExportParams) => {
    const response = await apiClient.get<Blob>('/api/guardian/export', {
      params: {
        ...(params?.from ? { from: params.from } : {}),
        ...(params?.to ? { to: params.to } : {}),
        ...(params?.studentId ? { studentId: params.studentId } : {}),
      },
      responseType: 'blob',
    });
    return response.data;
  },
};
