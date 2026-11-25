import { ApiClientError } from '@/lib/api-client';
import { getNextTargetDateIso } from '@/lib/study-utils';
import {
  buildGuardianDashboardFromBuckets,
  findGuardianSubmissionDetail,
} from '@/mocks/fixtures/study-seed';
import { runMockOperation } from '@/mocks/mock-runtime';
import type {
  GuardianDashboardParams,
  GuardianService,
} from '@/services/guardian.service';
import { getMockStudyState } from '@/services/mock/study.mock-service';
import type {
  GuardianDashboardResponse,
  GuardianSubmissionDetailResponse,
} from '@/types';

export const guardianMockService: GuardianService = {
  async getDashboard(
    params?: GuardianDashboardParams
  ): Promise<GuardianDashboardResponse> {
    const selectedSubject = params?.subjectCode ?? 'ALL';
    const selectedDate = params?.date ?? getNextTargetDateIso();

    return runMockOperation<GuardianDashboardResponse>(
      () => {
        const state = getMockStudyState();
        return buildGuardianDashboardFromBuckets(
          state.buckets,
          selectedDate,
          selectedSubject
        );
      },
      {
        requireRole: 'GUARDIAN',
        emptyFallback: {
          linkedStudentName: 'Nguyễn Minh Khôi',
          grade: 6,
          selectedDate,
          selectedSubject,
          totalTasks: 0,
          completedTasks: 0,
          evaluatingTasks: 0,
          items: [],
        },
      }
    );
  },

  async getSubmissionDetail(
    submissionId: number
  ): Promise<GuardianSubmissionDetailResponse> {
    return runMockOperation<GuardianSubmissionDetailResponse>(
      () => {
        const state = getMockStudyState();
        const found = findGuardianSubmissionDetail(state.buckets, submissionId);
        if (!found) {
          throw new ApiClientError({
            status: 404,
            errorCode: 'STUDY_001',
            message: 'Không tìm thấy bài nộp của học sinh.',
          });
        }
        return structuredClone(found);
      },
      { requireRole: 'GUARDIAN' }
    );
  },
};
