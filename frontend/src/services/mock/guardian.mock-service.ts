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
  GuardianAssignCustomTaskRequest,
  GuardianDashboardResponse,
  GuardianSkipReviewRequest,
  GuardianSkipTaskRequest,
  GuardianSkipTaskResponse,
  GuardianSubmissionDetailResponse,
  GuardianTaskItem,
  GuardianVerifyCustomTaskRequest,
  GuardianWeeklyDaySummary,
  GuardianWeeklySummaryResponse,
  Weekday,
} from '@/types';

let mockDefaultDriveUrl: string | null = null;

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
          defaultDriveFolderUrl: mockDefaultDriveUrl,
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

  async skipTask(
    taskId: number,
    request: GuardianSkipTaskRequest
  ): Promise<GuardianSkipTaskResponse> {
    return runMockOperation(
      () => {
        const state = getMockStudyState();
        const shouldSkip = request.skipped;
        const normalizedNote = request.note?.trim() || null;

        for (const bucket of state.buckets) {
          for (const detail of [bucket.reviewTask, bucket.previewTask]) {
            if (detail.task.id !== taskId) {
              continue;
            }
            if (
              detail.submissions.length > 0 ||
              detail.task.status === 'EVALUATING' ||
              detail.task.status === 'EVALUATED' ||
              detail.task.status === 'EVALUATION_FAILED' ||
              detail.task.status === 'SUBMITTED'
            ) {
              throw new ApiClientError({
                status: 400,
                errorCode: 'VAL_001',
                message: shouldSkip
                  ? 'Không thể bỏ qua bài học đã được học sinh nộp.'
                  : 'Không thể khôi phục bài học đã có bài nộp.',
              });
            }

            if (shouldSkip) {
              detail.task.status = 'SKIPPED';
              if (detail.task.skipRequestStatus === 'PENDING') {
                detail.task.skipRequestStatus = 'APPROVED';
                detail.task.skipReviewedAt = new Date().toISOString();
              }
              if (normalizedNote) {
                detail.task.guardianNote = normalizedNote;
              }
              if (detail.task.taskType === 'REVIEW') {
                bucket.previewTask.reviewCompleted = true;
                if (bucket.previewTask.task.status === 'BLOCKED') {
                  bucket.previewTask.task.status = 'NOT_STARTED';
                }
              }
            } else {
              detail.task.guardianNote = null;
              detail.task.skipRequestStatus = null;
              detail.task.skipRequestReason = null;
              detail.task.skipRequestedAt = null;
              detail.task.skipReviewedAt = null;
              if (detail.task.taskType === 'REVIEW') {
                detail.task.status = 'NOT_STARTED';
                if (
                  bucket.previewTask.task.status === 'NOT_STARTED' &&
                  bucket.previewTask.submissions.length === 0
                ) {
                  bucket.previewTask.reviewCompleted = false;
                  bucket.previewTask.task.status = 'BLOCKED';
                }
              } else {
                const reviewCleared =
                  bucket.reviewTask.task.status === 'SKIPPED' ||
                  bucket.reviewTask.submissions.length > 0;
                detail.task.status = reviewCleared ? 'NOT_STARTED' : 'BLOCKED';
              }
            }
            return {
              taskId,
              status: detail.task.status,
              guardianNote: detail.task.guardianNote ?? null,
              skipRequestStatus: detail.task.skipRequestStatus ?? null,
              skipRequestReason: detail.task.skipRequestReason ?? null,
              skipRequestedAt: detail.task.skipRequestedAt ?? null,
              skipReviewedAt: detail.task.skipReviewedAt ?? null,
            };
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

  async reviewSkipRequest(
    taskId: number,
    request: GuardianSkipReviewRequest
  ): Promise<GuardianSkipTaskResponse> {
    return runMockOperation(
      () => {
        const state = getMockStudyState();
        const normalizedNote = request.note?.trim() || null;

        for (const bucket of state.buckets) {
          for (const detail of [bucket.reviewTask, bucket.previewTask]) {
            if (detail.task.id !== taskId) {
              continue;
            }
            if (detail.task.skipRequestStatus !== 'PENDING') {
              throw new ApiClientError({
                status: 400,
                errorCode: 'VAL_001',
                message: 'Yêu cầu xin bỏ qua này không còn ở trạng thái chờ duyệt.',
              });
            }
            const reviewedAt = new Date().toISOString();
            detail.task.skipReviewedAt = reviewedAt;
            if (request.approved) {
              detail.task.skipRequestStatus = 'APPROVED';
              detail.task.status = 'SKIPPED';
              if (normalizedNote) {
                detail.task.guardianNote = normalizedNote;
              }
              if (detail.task.taskType === 'REVIEW') {
                bucket.previewTask.reviewCompleted = true;
                if (bucket.previewTask.task.status === 'BLOCKED') {
                  bucket.previewTask.task.status = 'NOT_STARTED';
                }
              }
            } else {
              detail.task.skipRequestStatus = 'REJECTED';
              detail.task.guardianNote = normalizedNote;
            }
            return {
              taskId,
              status: detail.task.status,
              guardianNote: detail.task.guardianNote ?? null,
              skipRequestStatus: detail.task.skipRequestStatus ?? null,
              skipRequestReason: detail.task.skipRequestReason ?? null,
              skipRequestedAt: detail.task.skipRequestedAt ?? null,
              skipReviewedAt: detail.task.skipReviewedAt ?? null,
            };
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

  async assignCustomTask(
    taskId: number,
    request: GuardianAssignCustomTaskRequest
  ): Promise<GuardianTaskItem> {
    return runMockOperation(
      () => {
        const state = getMockStudyState();
        const title = request.title.trim();
        const driveUrl = request.driveFolderUrl.trim();
        if (title.length < 5 || !driveUrl.startsWith('https://')) {
          throw new ApiClientError({
            status: 400,
            errorCode: 'VAL_001',
            message: 'Vui lòng nhập tiêu đề (>= 5 ký tự) và link Drive https:// hợp lệ.',
          });
        }
        for (const bucket of state.buckets) {
          for (const detail of [bucket.reviewTask, bucket.previewTask]) {
            if (detail.task.id !== taskId) continue;
            detail.task.assignmentMode = 'CUSTOM_DRIVE';
            detail.task.customTaskTitle = title;
            detail.task.customTaskDescription = request.description?.trim() || null;
            detail.task.customDriveFolderUrl = driveUrl;
            detail.task.customAssignedAt = new Date().toISOString();
            if (detail.task.status === 'SKIPPED') {
              detail.task.status = 'NOT_STARTED';
            }
            detail.task.skipRequestStatus = null;
            detail.task.skipRequestReason = null;
            if (request.saveAsDefaultDriveFolder) {
              mockDefaultDriveUrl = driveUrl;
            }
            return {
              taskId: detail.task.id,
              currentSubmissionId: null,
              targetDate: detail.targetDate,
              subjectCode: detail.task.subjectCode,
              subjectName: detail.task.subjectName,
              taskType: detail.task.taskType,
              status: detail.task.status,
              lessonTitle: null,
              revisionCount: detail.task.revisionCount,
              evaluation: null,
              guardianAcknowledgedAt: detail.task.guardianAcknowledgedAt ?? null,
              guardianNote: detail.task.guardianNote ?? null,
              skipRequestStatus: null,
              skipRequestReason: null,
              skipRequestedAt: null,
              skipReviewedAt: null,
              assignmentMode: 'CUSTOM_DRIVE',
              customTaskTitle: detail.task.customTaskTitle,
              customTaskDescription: detail.task.customTaskDescription,
              customDriveFolderUrl: detail.task.customDriveFolderUrl,
              customAssignedAt: detail.task.customAssignedAt,
              customSubmissionNote: detail.task.customSubmissionNote ?? null,
              customSubmissionUrl: detail.task.customSubmissionUrl ?? null,
              customSubmittedAt: detail.task.customSubmittedAt ?? null,
            };
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

  async clearCustomTask(taskId: number): Promise<GuardianTaskItem> {
    return runMockOperation(
      () => {
        const state = getMockStudyState();
        for (const bucket of state.buckets) {
          for (const detail of [bucket.reviewTask, bucket.previewTask]) {
            if (detail.task.id !== taskId) continue;
            detail.task.assignmentMode = 'STANDARD_AI';
            detail.task.customTaskTitle = null;
            detail.task.customTaskDescription = null;
            detail.task.customDriveFolderUrl = null;
            detail.task.customAssignedAt = null;
            detail.task.customSubmissionNote = null;
            detail.task.customSubmissionUrl = null;
            detail.task.customSubmittedAt = null;
            return {
              taskId: detail.task.id,
              currentSubmissionId: null,
              targetDate: detail.targetDate,
              subjectCode: detail.task.subjectCode,
              subjectName: detail.task.subjectName,
              taskType: detail.task.taskType,
              status: detail.task.status,
              lessonTitle: null,
              revisionCount: detail.task.revisionCount,
              evaluation: null,
              assignmentMode: 'STANDARD_AI',
            };
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

  async verifyCustomTask(
    taskId: number,
    request: GuardianVerifyCustomTaskRequest
  ): Promise<GuardianTaskItem> {
    return runMockOperation(
      () => {
        const state = getMockStudyState();
        const note = request.note?.trim() || null;
        for (const bucket of state.buckets) {
          for (const detail of [bucket.reviewTask, bucket.previewTask]) {
            if (detail.task.id !== taskId) continue;
            if (request.approved) {
              detail.task.status = 'EVALUATED';
              detail.task.guardianAcknowledgedAt = new Date().toISOString();
              if (note) detail.task.guardianNote = note;
            } else {
              detail.task.status = 'IN_PROGRESS';
              detail.task.guardianNote = note;
            }
            return {
              taskId: detail.task.id,
              currentSubmissionId: null,
              targetDate: detail.targetDate,
              subjectCode: detail.task.subjectCode,
              subjectName: detail.task.subjectName,
              taskType: detail.task.taskType,
              status: detail.task.status,
              lessonTitle: null,
              revisionCount: detail.task.revisionCount,
              evaluation: null,
              guardianAcknowledgedAt: detail.task.guardianAcknowledgedAt ?? null,
              guardianNote: detail.task.guardianNote ?? null,
              assignmentMode: detail.task.assignmentMode ?? 'CUSTOM_DRIVE',
              customTaskTitle: detail.task.customTaskTitle ?? null,
              customTaskDescription: detail.task.customTaskDescription ?? null,
              customDriveFolderUrl: detail.task.customDriveFolderUrl ?? null,
              customAssignedAt: detail.task.customAssignedAt ?? null,
              customSubmissionNote: detail.task.customSubmissionNote ?? null,
              customSubmissionUrl: detail.task.customSubmissionUrl ?? null,
              customSubmittedAt: detail.task.customSubmittedAt ?? null,
            };
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
            const activeTasks = allTasks.filter((t) => t.status !== 'SKIPPED');
            const dayTotal = matchesTarget ? activeTasks.length : 0;
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

        const activeTasks = allTasks.filter((t) => t.status !== 'SKIPPED');
        return {
          studentId: meta.studentId,
          studentName: meta.name,
          grade: meta.grade,
          weekStart: days[0].date,
          weekEnd: days[6].date,
          totalTasks: activeTasks.length,
          completedTasks: allTasks.filter((t) => t.status === 'EVALUATED').length,
          acknowledgedTasks: allTasks.filter((t) => Boolean(t.guardianAcknowledgedAt))
            .length,
          days,
          subjects: state.buckets.map((b) => {
            const pair = [b.reviewTask.task, b.previewTask.task];
            return {
              subjectCode: b.subjectCode,
              subjectName: b.reviewTask.task.subjectName,
              totalTasks: pair.filter((t) => t.status !== 'SKIPPED').length,
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
