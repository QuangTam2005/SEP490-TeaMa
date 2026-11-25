import { ApiClientError } from '@/lib/api-client';
import {
  getNextTargetDateIso,
  getWeekdayFromIsoDate,
  getWeekdayLabel,
} from '@/lib/study-utils';
import {
  buildGuardianDashboardFromBuckets,
  findGuardianSubmissionDetail,
} from '@/mocks/fixtures/study-seed';
import { runMockOperation } from '@/mocks/mock-runtime';
import type {
  GuardianDashboardParams,
  GuardianService,
  GuardianWeeklyParams,
} from '@/services/guardian.service';
import { getMockStudyState } from '@/services/mock/study.mock-service';
import type {
  GuardianAcknowledgeRequest,
  GuardianAcknowledgeResponse,
  GuardianDashboardResponse,
  GuardianSubmissionDetailResponse,
  GuardianWeeklyDaySummary,
  GuardianWeeklySummaryResponse,
  Weekday,
} from '@/types';

function resolveMockStudentMeta(studentId?: number): {
  studentId: number;
  name: string;
  grade: number;
} {
  if (studentId === 11) {
    return { studentId: 11, name: 'Nguyễn Minh An', grade: 10 };
  }
  return { studentId: 10, name: 'Nguyễn Minh Khôi', grade: 6 };
}

export const guardianMockService: GuardianService = {
  async getDashboard(
    params?: GuardianDashboardParams
  ): Promise<GuardianDashboardResponse> {
    const selectedSubject = params?.subjectCode ?? 'ALL';
    const selectedDate = params?.date ?? getNextTargetDateIso();
    const meta = resolveMockStudentMeta(params?.studentId);

    return runMockOperation<GuardianDashboardResponse>(
      () => {
        const state = getMockStudyState();
        const result = buildGuardianDashboardFromBuckets(
          state.buckets,
          selectedDate,
          selectedSubject
        );
        return {
          ...result,
          linkedStudentName: meta.name,
          grade: meta.grade,
        };
      },
      {
        requireRole: 'GUARDIAN',
        emptyFallback: {
          linkedStudentName: meta.name,
          grade: meta.grade,
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

  async acknowledgeTask(
    taskId: number,
    request?: GuardianAcknowledgeRequest
  ): Promise<GuardianAcknowledgeResponse> {
    return runMockOperation(
      () => {
        const state = getMockStudyState();
        const now = new Date().toISOString();
        const normalizedNote = request?.note?.trim() || null;

        for (const bucket of state.buckets) {
          for (const detail of [bucket.reviewTask, bucket.previewTask]) {
            if (detail.task.id === taskId) {
              detail.task.guardianAcknowledgedAt = now;
              detail.task.guardianNote = normalizedNote;
              return { taskId, acknowledgedAt: now, note: normalizedNote };
            }
          }
        }
        throw new ApiClientError({
          status: 404,
          errorCode: 'STUDY_001',
          message: 'Không tìm thấy nhiệm vụ học tập.',
        });
      },
      { requireRole: 'GUARDIAN' }
    );
  },

  async getWeeklySummary(
    params?: GuardianWeeklyParams
  ): Promise<GuardianWeeklySummaryResponse> {
    const meta = resolveMockStudentMeta(params?.studentId);
    return runMockOperation<GuardianWeeklySummaryResponse>(
      () => {
        const state = getMockStudyState();
        const allTasks = state.buckets.flatMap((b) => [
          b.reviewTask.task,
          b.previewTask.task,
        ]);
        const startIso = params?.weekStart ?? state.targetDate;
        const [y, m, d] = startIso.split('-').map(Number);
        const startDt = new Date(Date.UTC(y, (m || 1) - 1, d || 1));

        const days: GuardianWeeklyDaySummary[] = Array.from(
          { length: 7 },
          (_, idx) => {
            const dt = new Date(startDt);
            dt.setUTCDate(startDt.getUTCDate() + idx);
            const dateIso = dt.toISOString().slice(0, 10);
            const weekday: Weekday = getWeekdayFromIsoDate(dateIso);
            const matchesTarget = dateIso === state.targetDate;
            const dayTotal = matchesTarget ? allTasks.length : 0;
            const dayCompleted = matchesTarget
              ? allTasks.filter((t) => t.status === 'EVALUATED').length
              : 0;
            const dayAcknowledged = matchesTarget
              ? allTasks.filter((t) => Boolean(t.guardianAcknowledgedAt)).length
              : 0;
            return {
              date: dateIso,
              weekdayLabel: getWeekdayLabel(weekday),
              totalTasks: dayTotal,
              completedTasks: dayCompleted,
              acknowledgedTasks: dayAcknowledged,
            };
          }
        );

        return {
          studentId: meta.studentId,
          studentName: meta.name,
          grade: meta.grade,
          weekStart: days[0].date,
          weekEnd: days[6].date,
          totalTasks: allTasks.length,
          completedTasks: allTasks.filter((t) => t.status === 'EVALUATED').length,
          acknowledgedTasks: allTasks.filter((t) => Boolean(t.guardianAcknowledgedAt))
            .length,
          days,
          subjects: state.buckets.map((b) => {
            const pair = [b.reviewTask.task, b.previewTask.task];
            return {
              subjectCode: b.subjectCode,
              subjectName: b.reviewTask.task.subjectName,
              totalTasks: pair.length,
              completedTasks: pair.filter((t) => t.status === 'EVALUATED').length,
            };
          }),
        };
      },
      { requireRole: 'GUARDIAN' }
    );
  },

  async exportReport(): Promise<Blob> {
    return runMockOperation(
      () =>
        new Blob(['Smart Otter Mock Study Report'], {
          type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        }),
      { requireRole: 'GUARDIAN' }
    );
  },
};
