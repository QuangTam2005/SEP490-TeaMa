import { ApiClientError } from '@/lib/api-client';
import {
  ALL_SUBJECTS,
  MAX_FEEDBACK_CHARS,
  MAX_POST_EVAL_REVISIONS,
  MAX_SUBMISSION_CHARS,
  MIN_SUBMISSION_CHARS,
  countNormalizedChars,
  getNextTargetDateIso,
  getSubjectName,
  getWeekdayFromIsoDate,
  getWeekdayLabel,
  normalizeStudyText,
} from '@/lib/study-utils';
import {
  type SeededTaskBucket,
  buildInitialStudyBuckets,
} from '@/mocks/fixtures/study-seed';
import {
  getMockRuntimeState,
  registerMockResetHook,
  runMockOperation,
} from '@/mocks/mock-runtime';
import { getCurrentMockTimetable } from '@/services/mock/timetable.mock-service';
import type { StudyService } from '@/services/study.service';
import type {
  AIEvaluationResponse,
  NextDayStudyPlanResponse,
  StudentSkipRequestPayload,
  StudentStreakResponse,
  StudyPlanHistoryItem,
  StudySubmissionResponse,
  StudyTaskDetailResponse,
  StudyTaskSummaryResponse,
  SubmitStudyRequest,
  SubjectCode,
} from '@/types';

let syncedTimetableVersion = 1;
let planState = buildInitialStudyBuckets(getCurrentMockTimetable().entries);
let nextSubmissionId = 2000;
let nextEvaluationId = 900;
let nextTaskId = 500;
const idempotencyCache = new Map<string, StudySubmissionResponse>();

registerMockResetHook(() => {
  syncedTimetableVersion = 1;
  planState = buildInitialStudyBuckets(getCurrentMockTimetable().entries);
  nextSubmissionId = 2000;
  nextEvaluationId = 900;
  nextTaskId = 500;
  idempotencyCache.clear();
});

function syncBucketsWithTimetable(): void {
  const timetable = getCurrentMockTimetable();
  if (timetable.version === syncedTimetableVersion) {
    return;
  }
  syncedTimetableVersion = timetable.version;
  const targetDate = getNextTargetDateIso();
  const weekday = getWeekdayFromIsoDate(targetDate);
  const weekdayLabel = getWeekdayLabel(weekday);

  const nextBuckets: SeededTaskBucket[] = [];
  for (const subject of ALL_SUBJECTS) {
    const matches = timetable.entries.filter(
      (e) => e.weekday === weekday && e.subjectCode === subject.code
    );
    if (matches.length === 0) {
      continue;
    }
    const notes = matches
      .map((m) => m.periodNote)
      .filter((n): n is string => Boolean(n && n.trim()));
    const periodNote = notes.length > 0 ? notes.join(', ') : null;

    const existing = planState.buckets.find(
      (b) => b.subjectCode === subject.code
    );
    if (existing) {
      nextBuckets.push({ ...existing, periodNote });
    } else {
      const subjectName = getSubjectName(subject.code);
      const reviewId = nextTaskId++;
      const previewId = nextTaskId++;
      nextBuckets.push({
        subjectCode: subject.code,
        periodNote,
        reviewTask: {
          task: {
            id: reviewId,
            subjectCode: subject.code,
            subjectName,
            taskType: 'REVIEW',
            status: 'NOT_STARTED',
            revisionCount: 0,
            maxRevisions: MAX_POST_EVAL_REVISIONS,
            currentLessonTitle: null,
            latestEvaluationStatus: null,
          },
          targetDate,
          reviewCompleted: false,
          submissions: [],
        },
        previewTask: {
          task: {
            id: previewId,
            subjectCode: subject.code,
            subjectName,
            taskType: 'PREVIEW',
            status: 'BLOCKED',
            revisionCount: 0,
            maxRevisions: MAX_POST_EVAL_REVISIONS,
            currentLessonTitle: null,
            latestEvaluationStatus: null,
          },
          targetDate,
          reviewCompleted: false,
          submissions: [],
        },
      });
    }
  }
  planState = { targetDate, weekdayLabel, buckets: nextBuckets };
}

function findTaskContainer(taskId: number): {
  bucket: SeededTaskBucket;
  detail: StudyTaskDetailResponse;
} | null {
  syncBucketsWithTimetable();
  for (const bucket of planState.buckets) {
    if (bucket.reviewTask.task.id === taskId) {
      return { bucket, detail: bucket.reviewTask };
    }
    if (bucket.previewTask.task.id === taskId) {
      return { bucket, detail: bucket.previewTask };
    }
  }
  return null;
}

function buildMockAiEvaluation(
  submissionId: number,
  subjectCode: SubjectCode,
  subjectName: string,
  lessonTitle: string,
  forceInsufficientEvidence: boolean
): AIEvaluationResponse {
  if (forceInsufficientEvidence) {
    return {
      id: nextEvaluationId++,
      submissionId,
      accuracy: 'UNVERIFIABLE',
      completeness: 'UNDETERMINED',
      overallStatus: 'INSUFFICIENT_EVIDENCE',
      feedback:
        'Hệ thống chưa tìm đủ tài liệu SGK phù hợp để đối chiếu chi tiết cho tên bài học này. Con hãy ghi rõ tên bài và chương trong SGK để AI hỗ trợ chính xác hơn nhé.'.slice(
          0,
          MAX_FEEDBACK_CHARS
        ),
      incorrectPoints: [],
      missingPoints: [],
      confidence: 'LOW',
      sources: [],
      evaluatedAt: new Date().toISOString(),
    };
  }

  const rawFeedback = `Con đã trình bày mạch lạc nội dung bài "${lessonTitle}" môn ${subjectName}. Phần ý chính khá đầy đủ; con có thể bổ sung thêm 1 ví dụ minh họa ngắn gọn để nhớ lâu hơn khi lên lớp.`;

  return {
    id: nextEvaluationId++,
    submissionId,
    accuracy: 'CORRECT',
    completeness: 'COMPLETE',
    overallStatus: 'GOOD',
    feedback: rawFeedback.slice(0, MAX_FEEDBACK_CHARS),
    incorrectPoints: [],
    missingPoints: [
      `Có thể ghi thêm 1 ví dụ thực tế ngắn cho bài "${lessonTitle}" (${subjectCode}).`,
    ],
    confidence: 'HIGH',
    sources: [
      {
        id: nextEvaluationId * 10 + 1,
        title: `${subjectName} Kết nối tri thức với cuộc sống – ${lessonTitle}`,
        url: 'https://loigiaihay.com/ket-noi-tri-thuc',
        snippet: `Tài liệu SGK Kết nối tri thức với cuộc sống môn ${subjectName} (${lessonTitle}).`,
        retrievedAt: new Date().toISOString(),
      },
      {
        id: nextEvaluationId * 10 + 2,
        title: `Wikipedia – ${lessonTitle}`,
        url: 'https://vi.wikipedia.org',
        snippet: `Kiến thức chuẩn Wikipedia liên quan đến bài "${lessonTitle}".`,
        retrievedAt: new Date().toISOString(),
      },
    ],
    evaluatedAt: new Date().toISOString(),
  };
}

function buildFallbackSystemErrorEvaluation(
  submissionId: number
): AIEvaluationResponse {
  return {
    id: nextEvaluationId++,
    submissionId,
    accuracy: 'UNVERIFIABLE',
    completeness: 'UNDETERMINED',
    overallStatus: 'INSUFFICIENT_EVIDENCE',
    feedback:
      'Hệ thống AI đang gặp lỗi sau 3 lần thử đối chiếu tự động. Phụ huynh vui lòng tự kiểm tra trực tiếp bài làm của con nhé.'.slice(
        0,
        MAX_FEEDBACK_CHARS
      ),
    incorrectPoints: [],
    missingPoints: [],
    confidence: 'LOW',
    sources: [],
    evaluatedAt: new Date().toISOString(),
  };
}

function scheduleAsyncEvaluation(
  detail: StudyTaskDetailResponse,
  submissionId: number
): void {
  const { scenario } = getMockRuntimeState();
  if (scenario === 'evaluating') {
    return;
  }

  setTimeout(() => {
    const targetSub = detail.submissions.find((s) => s.id === submissionId);
    if (!targetSub) {
      return;
    }

    const latestScenario = getMockRuntimeState().scenario;
    if (latestScenario === 'evaluating') {
      return;
    }

    if (latestScenario === 'evaluation_failed') {
      // After 3 automatic retries fail, stop retrying and forward to Guardian as reviewed with error notice
      const fallbackEval = buildFallbackSystemErrorEvaluation(submissionId);
      targetSub.evaluation = fallbackEval;
      targetSub.lastError = null;
      detail.task.status = 'EVALUATED';
      detail.task.latestEvaluationStatus = fallbackEval.overallStatus;
      return;
    }

    const evaluation = buildMockAiEvaluation(
      submissionId,
      detail.task.subjectCode,
      detail.task.subjectName,
      targetSub.lessonTitle,
      latestScenario === 'insufficient_evidence'
    );
    targetSub.evaluation = evaluation;
    targetSub.lastError = null;
    detail.task.status = 'EVALUATED';
    detail.task.latestEvaluationStatus = evaluation.overallStatus;
  }, 1400);
}

export function getMockStudyState() {
  syncBucketsWithTimetable();
  return planState;
}

export const studyMockService: StudyService = {
  async getNextDayPlan(): Promise<NextDayStudyPlanResponse> {
    return runMockOperation<NextDayStudyPlanResponse>(
      () => {
        syncBucketsWithTimetable();
        const { scenario } = getMockRuntimeState();

        return {
          planId: 1,
          targetDate: planState.targetDate,
          weekdayLabel: planState.weekdayLabel,
          hasSchedule: planState.buckets.length > 0,
          subjects: planState.buckets.map((bucket) => ({
            subjectCode: bucket.subjectCode,
            subjectName: getSubjectName(bucket.subjectCode),
            periodNote: bucket.periodNote,
            reviewTask: {
              ...bucket.reviewTask.task,
              status:
                scenario === 'evaluating' &&
                bucket.reviewTask.submissions.length > 0
                  ? 'EVALUATING'
                  : bucket.reviewTask.task.status,
            },
            previewTask: {
              ...bucket.previewTask.task,
            },
          })),
        };
      },
      {
        requireRole: 'STUDENT',
        emptyFallback: {
          planId: null,
          targetDate: getNextTargetDateIso(),
          weekdayLabel: getWeekdayLabel(getWeekdayFromIsoDate(getNextTargetDateIso())),
          hasSchedule: false,
          subjects: [],
        },
      }
    );
  },

  async getTaskDetail(taskId: number): Promise<StudyTaskDetailResponse> {
    return runMockOperation(
      () => {
        const found = findTaskContainer(taskId);
        if (!found) {
          throw new ApiClientError({
            status: 404,
            errorCode: 'STUDY_001',
            message: 'Không tìm thấy nhiệm vụ học tập.',
          });
        }
        const cloned = structuredClone(found.detail);
        const { scenario } = getMockRuntimeState();
        if (scenario === 'evaluating' && cloned.submissions.length > 0) {
          cloned.task.status = 'EVALUATING';
          cloned.submissions[0].evaluation = null;
        }
        return cloned;
      },
      { requireRole: 'STUDENT' }
    );
  },

  async submitTask(
    taskId: number,
    request: SubmitStudyRequest
  ): Promise<StudySubmissionResponse> {
    return runMockOperation(
      () => {
        const cached = idempotencyCache.get(request.idempotencyKey);
        if (cached) {
          return structuredClone(cached);
        }

        const found = findTaskContainer(taskId);
        if (!found) {
          throw new ApiClientError({
            status: 404,
            errorCode: 'STUDY_001',
            message: 'Không tìm thấy nhiệm vụ học tập.',
          });
        }

        const { bucket, detail } = found;
        if (detail.task.status === 'SKIPPED') {
          throw new ApiClientError({
            status: 400,
            errorCode: 'VAL_001',
            message: 'Nhiệm vụ này đã được phụ huynh đánh dấu bỏ qua.',
          });
        }

        if (
          detail.task.taskType === 'PREVIEW' &&
          bucket.reviewTask.task.status !== 'SKIPPED' &&
          bucket.reviewTask.submissions.length === 0
        ) {
          throw new ApiClientError({
            status: 400,
            errorCode: 'STUDY_003',
            message: 'Con cần nộp bài Ôn bài cũ trước khi làm Soạn bài mới nhé.',
          });
        }

        if (detail.task.status === 'EVALUATING') {
          throw new ApiClientError({
            status: 409,
            errorCode: 'STUDY_006',
            message: 'AI đang đánh giá bài vừa nộp, con đợi vài giây nhé.',
          });
        }

        const isRevision = detail.submissions.length > 0;
        if (
          isRevision &&
          (detail.submissions.length >= 3 ||
            detail.task.revisionCount >= MAX_POST_EVAL_REVISIONS)
        ) {
          throw new ApiClientError({
            status: 409,
            errorCode: 'STUDY_005',
            message: 'Con đã nộp đủ tối đa 3 phiên bản (v1–v3) cho nhiệm vụ này rồi.',
          });
        }

        const trimmedTitle = request.lessonTitle.trim();
        const normalizedContent = normalizeStudyText(request.content);
        const charCount = countNormalizedChars(request.content);

        if (!trimmedTitle) {
          throw new ApiClientError({
            status: 400,
            errorCode: 'VAL_001',
            message: 'Con hãy nhập tên bài học nhé.',
            fieldErrors: { lessonTitle: 'Tên bài học không được để trống' },
          });
        }

        if (
          charCount < MIN_SUBMISSION_CHARS ||
          charCount > MAX_SUBMISSION_CHARS
        ) {
          throw new ApiClientError({
            status: 400,
            errorCode: 'STUDY_004',
            message: `Bài viết cần từ ${MIN_SUBMISSION_CHARS} đến ${MAX_SUBMISSION_CHARS} ký tự (hiện tại: ${charCount}).`,
            fieldErrors: {
              content: `Độ dài hợp lệ: ${MIN_SUBMISSION_CHARS}–${MAX_SUBMISSION_CHARS} ký tự (đang có ${charCount}).`,
            },
          });
        }

        for (const oldSub of detail.submissions) {
          oldSub.isCurrent = false;
        }

        const newVersion = detail.submissions.length + 1;
        const created: StudySubmissionResponse = {
          id: nextSubmissionId++,
          taskId,
          version: newVersion,
          isCurrent: true,
          lessonTitle: trimmedTitle,
          content: normalizedContent,
          charCount,
          submittedAt: new Date().toISOString(),
          evaluation: null,
          lastError: null,
        };

        detail.submissions.unshift(created);
        detail.task.status = 'EVALUATING';
        detail.task.currentLessonTitle = trimmedTitle;
        detail.task.revisionCount = Math.max(0, newVersion - 1);
        detail.task.latestEvaluationStatus = null;
        if (detail.task.skipRequestStatus === 'PENDING') {
          detail.task.skipRequestStatus = null;
          detail.task.skipRequestReason = null;
          detail.task.skipRequestedAt = null;
          detail.task.skipReviewedAt = null;
        }

        if (detail.task.taskType === 'REVIEW') {
          bucket.reviewTask.reviewCompleted = true;
          bucket.previewTask.reviewCompleted = true;
          if (bucket.previewTask.task.status === 'BLOCKED') {
            bucket.previewTask.task.status = 'NOT_STARTED';
          }
        }

        idempotencyCache.set(request.idempotencyKey, structuredClone(created));
        scheduleAsyncEvaluation(detail, created.id);
        return structuredClone(created);
      },
      { requireRole: 'STUDENT' }
    );
  },

  async retryEvaluation(submissionId: number): Promise<StudySubmissionResponse> {
    return runMockOperation(
      () => {
        syncBucketsWithTimetable();
        for (const bucket of planState.buckets) {
          for (const detail of [bucket.reviewTask, bucket.previewTask]) {
            const sub = detail.submissions.find((s) => s.id === submissionId);
            if (sub) {
              sub.lastError = null;
              detail.task.status = 'EVALUATING';
              scheduleAsyncEvaluation(detail, sub.id);
              return structuredClone(sub);
            }
          }
        }
        throw new ApiClientError({
          status: 404,
          errorCode: 'STUDY_001',
          message: 'Không tìm thấy bài nộp cần đánh giá lại.',
        });
      },
      { requireRole: 'STUDENT' }
    );
  },

  async getPlanHistory(from?: string, to?: string): Promise<StudyPlanHistoryItem[]> {
    return runMockOperation<StudyPlanHistoryItem[]>(
      () => {
        syncBucketsWithTimetable();
        const subjects = planState.buckets.map((bucket) => ({
          subjectCode: bucket.subjectCode,
          subjectName: getSubjectName(bucket.subjectCode),
          periodNote: bucket.periodNote,
          reviewTask: { ...bucket.reviewTask.task },
          previewTask: { ...bucket.previewTask.task },
        }));
        const allTasks = subjects.flatMap((s) => [s.reviewTask, s.previewTask]);
        const items: StudyPlanHistoryItem[] = [
          {
            planId: 1,
            targetDate: planState.targetDate,
            weekdayLabel: planState.weekdayLabel,
            totalTasks: allTasks.filter((t) => t.status !== 'SKIPPED').length,
            completedTasks: allTasks.filter((t) => t.status === 'EVALUATED').length,
            subjects,
          },
        ];
        return items.filter((item) => {
          if (from && item.targetDate < from) return false;
          if (to && item.targetDate > to) return false;
          return true;
        });
      },
      { requireRole: 'STUDENT', emptyFallback: [] }
    );
  },

  async getStreak(): Promise<StudentStreakResponse> {
    return runMockOperation<StudentStreakResponse>(
      () => ({
        currentStreak: 3,
        longestStreak: 5,
        totalCompletedDays: 8,
        lastActiveDate: planState.targetDate,
      }),
      {
        requireRole: 'STUDENT',
        emptyFallback: {
          currentStreak: 0,
          longestStreak: 0,
          totalCompletedDays: 0,
          lastActiveDate: null,
        },
      }
    );
  },

  async requestSkip(
    taskId: number,
    request: StudentSkipRequestPayload
  ): Promise<StudyTaskSummaryResponse> {
    return runMockOperation(
      () => {
        const container = findTaskContainer(taskId);
        if (!container) {
          throw new ApiClientError({
            status: 404,
            errorCode: 'STUDY_001',
            message: 'Không tìm thấy nhiệm vụ học tập.',
          });
        }
        const { detail } = container;
        if (detail.task.status === 'SKIPPED') {
          throw new ApiClientError({
            status: 400,
            errorCode: 'VAL_001',
            message: 'Bài học này đã được phụ huynh đánh dấu bỏ qua.',
          });
        }
        if (
          detail.submissions.length > 0 ||
          detail.task.status === 'SUBMITTED' ||
          detail.task.status === 'EVALUATING' ||
          detail.task.status === 'EVALUATED' ||
          detail.task.status === 'EVALUATION_FAILED'
        ) {
          throw new ApiClientError({
            status: 400,
            errorCode: 'VAL_001',
            message: 'Không thể xin bỏ qua bài học đã nộp bài.',
          });
        }
        const trimmed = request.reason.trim();
        if (trimmed.length < 5 || trimmed.length > 300) {
          throw new ApiClientError({
            status: 400,
            errorCode: 'VAL_001',
            message: 'Lý do xin bỏ qua phải từ 5 đến 300 ký tự.',
          });
        }
        detail.task.skipRequestStatus = 'PENDING';
        detail.task.skipRequestReason = trimmed;
        detail.task.skipRequestedAt = new Date().toISOString();
        detail.task.skipReviewedAt = null;
        return structuredClone(detail.task);
      },
      { requireRole: 'STUDENT' }
    );
  },

  async cancelSkipRequest(taskId: number): Promise<StudyTaskSummaryResponse> {
    return runMockOperation(
      () => {
        const container = findTaskContainer(taskId);
        if (!container) {
          throw new ApiClientError({
            status: 404,
            errorCode: 'STUDY_001',
            message: 'Không tìm thấy nhiệm vụ học tập.',
          });
        }
        const { detail } = container;
        if (detail.task.skipRequestStatus !== 'PENDING') {
          throw new ApiClientError({
            status: 400,
            errorCode: 'VAL_001',
            message: 'Chỉ có thể hủy yêu cầu xin bỏ qua đang chờ duyệt.',
          });
        }
        detail.task.skipRequestStatus = null;
        detail.task.skipRequestReason = null;
        detail.task.skipRequestedAt = null;
        detail.task.skipReviewedAt = null;
        return structuredClone(detail.task);
      },
      { requireRole: 'STUDENT' }
    );
  },
};
