import {
  ALL_SUBJECTS,
  countNormalizedChars,
  getNextTargetDateIso,
  getSubjectName,
  getWeekdayFromIsoDate,
  getWeekdayLabel,
} from '@/lib/study-utils';
import type {
  AIEvaluationResponse,
  GuardianDashboardResponse,
  GuardianSubmissionDetailResponse,
  StudySubmissionResponse,
  StudyTaskDetailResponse,
  StudyTaskSummaryResponse,
  SubjectCode,
  TimetableEntryDto,
  TimetableResponse,
  Weekday,
} from '@/types';

/**
 * Ensures seeded text stays cleanly inside BR-12 (850-2,000 normalized chars).
 */
const MATH_REVIEW_V1_CONTENT = [
  'Hôm nay em ôn tập lại kiến thức về ước chung, ước chung lớn nhất (ƯCLN) và bội chung nhỏ nhất (BCNN) trong chương trình Toán lớp 6.',
  '1. Khái niệm ước và bội: Nếu số tự nhiên a chia hết cho số tự nhiên b (b khác 0) thì ta nói a là bội của b, còn b là ước của a. Tập hợp các ước của một số luôn hữu hạn, còn tập hợp các bội của một số khác 0 là vô hạn.',
  '2. Cách tìm ƯCLN bằng cách phân tích các số ra thừa số nguyên tố gồm ba bước chính: Bước một là phân tích mỗi số ra thừa số nguyên tố; bước hai là chọn ra các thừa số nguyên tố chung; bước ba là lập tích các thừa số đã chọn, mỗi thừa số lấy với số mũ nhỏ nhất của nó.',
  'Ví dụ: Phân tích 24 = 2^3 * 3 và 36 = 2^2 * 3^2. Thừa số chung là 2 và 3, số mũ nhỏ nhất lần lượt là 2 và 1 nên ƯCLN(24, 36) = 2^2 * 3 = 12. Khi biết ƯCLN(24, 36) = 12, tập hợp ước chung của 24 và 36 chính là các ước của 12 gồm {1, 2, 3, 4, 6, 12}.',
  '3. Cách tìm BCNN cũng phân tích mỗi số ra thừa số nguyên tố, nhưng bước hai chọn cả thừa số nguyên tố chung và riêng, bước ba lấy với số mũ lớn nhất. Với 24 và 36, BCNN(24, 36) = 2^3 * 3^2 = 72.',
  '4. Ứng dụng: Dùng ƯCLN để rút gọn phân số về phân số tối giản và giải bài toán chia đều thành nhiều phần bằng nhau nhất. Dùng BCNN để quy đồng mẫu số các phân số hoặc tìm thời điểm hai sự kiện lặp lại cùng lúc.',
].join('\n');

const MATH_REVIEW_V2_CONTENT = [
  MATH_REVIEW_V1_CONTENT,
  '5. Bổ sung trường hợp hai số nguyên tố cùng nhau: Hai số tự nhiên có ƯCLN bằng 1 được gọi là hai số nguyên tố cùng nhau (ví dụ 8 và 9). Khi rút gọn phân số mà tử và mẫu là hai số nguyên tố cùng nhau thì đó là phân số tối giản.',
].join('\n');

const SCI_PREVIEW_V1_CONTENT = [
  'Em đọc trước bài Các phép đo cơ bản (đo chiều dài, đo khối lượng và đo thời gian) trong sách Khoa học tự nhiên lớp 6.',
  '1. Đo chiều dài: Đơn vị đo chiều dài hợp pháp ở Việt Nam là mét (ký hiệu: m). Ngoài ra thường dùng milimét (mm), xentimét (cm), đềximét (dm) và kilômét (km). Khi chọn thước đo cần chú ý giới hạn đo (GHĐ) và độ chia nhỏ nhất (ĐCNN) phù hợp với vật cần đo. Khi đo phải đặt thước dọc theo chiều dài vật, vạch số 0 ngang bằng một đầu của vật, đặt mắt nhìn theo hướng vuông góc với cạnh thước ở đầu kia và đọc kết quả theo vạch chia gần nhất.',
  '2. Đo khối lượng: Đơn vị cơ bản là kilôgam (kg). Dụng cụ thường dùng là cân đồng hồ, cân điện tử, cân Rô-béc-van hoặc cân y tế. Trước khi cân cần ước lượng khối lượng vật để chọn cân có GHĐ và ĐCNN phù hợp, điều chỉnh kim cân về đúng vạch số 0.',
  '3. Đo thời gian: Đơn vị cơ bản là giây (s), dùng đồng hồ bấm giây trong phòng thí nghiệm hoặc đồng hồ đeo tay. Việc ước lượng đại lượng trước khi đo giúp tránh làm hỏng dụng cụ và giảm sai số trong quá trình thực hành trên lớp.',
].join('\n');

const BASE_WEEKLY_ENTRIES: TimetableEntryDto[] = [
  { weekday: 1, subjectCode: 'MATH', periodNote: 'Tiết 1–2' },
  { weekday: 1, subjectCode: 'LIT', periodNote: 'Tiết 3' },
  { weekday: 1, subjectCode: 'ENG', periodNote: 'Tiết 4' },
  { weekday: 2, subjectCode: 'SCI', periodNote: 'Tiết 1' },
  { weekday: 2, subjectCode: 'HISGEO', periodNote: 'Tiết 2' },
  { weekday: 2, subjectCode: 'MATH', periodNote: 'Tiết 3' },
  { weekday: 3, subjectCode: 'LIT', periodNote: 'Tiết 1–2' },
  { weekday: 3, subjectCode: 'ENG', periodNote: 'Tiết 3' },
  { weekday: 3, subjectCode: 'SCI', periodNote: 'Tiết 4' },
  { weekday: 4, subjectCode: 'MATH', periodNote: 'Tiết 1' },
  { weekday: 4, subjectCode: 'HISGEO', periodNote: 'Tiết 2' },
  { weekday: 4, subjectCode: 'ENG', periodNote: 'Tiết 3' },
  { weekday: 5, subjectCode: 'SCI', periodNote: 'Tiết 1–2' },
  { weekday: 5, subjectCode: 'LIT', periodNote: 'Tiết 3' },
  { weekday: 5, subjectCode: 'MATH', periodNote: 'Tiết 4' },
  { weekday: 6, subjectCode: 'ENG', periodNote: 'Tiết 1' },
  { weekday: 6, subjectCode: 'HISGEO', periodNote: 'Tiết 2' },
];

export function buildInitialTimetable(): TimetableResponse {
  const tomorrowIso = getNextTargetDateIso();
  const tomorrowWeekday: Weekday = getWeekdayFromIsoDate(tomorrowIso);

  const hasTomorrowMath = BASE_WEEKLY_ENTRIES.some(
    (e) => e.weekday === tomorrowWeekday && e.subjectCode === 'MATH'
  );
  const hasTomorrowLit = BASE_WEEKLY_ENTRIES.some(
    (e) => e.weekday === tomorrowWeekday && e.subjectCode === 'LIT'
  );
  const hasTomorrowSci = BASE_WEEKLY_ENTRIES.some(
    (e) => e.weekday === tomorrowWeekday && e.subjectCode === 'SCI'
  );

  const entries = [...BASE_WEEKLY_ENTRIES];
  if (!hasTomorrowMath) {
    entries.push({ weekday: tomorrowWeekday, subjectCode: 'MATH', periodNote: 'Tiết 1' });
  }
  if (!hasTomorrowLit) {
    entries.push({ weekday: tomorrowWeekday, subjectCode: 'LIT', periodNote: 'Tiết 2' });
  }
  if (!hasTomorrowSci) {
    entries.push({ weekday: tomorrowWeekday, subjectCode: 'SCI', periodNote: 'Tiết 3' });
  }

  return {
    id: 1,
    version: 1,
    effectiveDate: tomorrowIso,
    updatedAt: '2026-11-24T13:30:00Z',
    updatedByName: 'Nguyễn Minh Khôi',
    entries,
  };
}

export interface SeededTaskBucket {
  subjectCode: SubjectCode;
  periodNote: string | null;
  reviewTask: StudyTaskDetailResponse;
  previewTask: StudyTaskDetailResponse;
}

export function buildInitialStudyBuckets(
  entries: TimetableEntryDto[]
): {
  targetDate: string;
  weekdayLabel: string;
  buckets: SeededTaskBucket[];
} {
  const targetDate = getNextTargetDateIso();
  const weekday = getWeekdayFromIsoDate(targetDate);
  const weekdayLabel = getWeekdayLabel(weekday);

  const uniqueSubjects: { code: SubjectCode; periodNote: string | null }[] = [];
  for (const subject of ALL_SUBJECTS) {
    const matches = entries.filter(
      (e) => e.weekday === weekday && e.subjectCode === subject.code
    );
    if (matches.length > 0) {
      const notes = matches
        .map((m) => m.periodNote)
        .filter((n): n is string => Boolean(n && n.trim()));
      uniqueSubjects.push({
        code: subject.code,
        periodNote: notes.length > 0 ? notes.join(', ') : null,
      });
    }
  }

  let taskIdSeq = 101;
  const buckets: SeededTaskBucket[] = uniqueSubjects.map((item) => {
    const subjectName = getSubjectName(item.code);
    const reviewId = taskIdSeq++;
    const previewId = taskIdSeq++;

    if (item.code === 'MATH') {
      const evalV1: AIEvaluationResponse = {
        id: 501,
        submissionId: 1001,
        accuracy: 'PARTIALLY_CORRECT',
        completeness: 'INCOMPLETE',
        overallStatus: 'NEEDS_IMPROVEMENT',
        feedback:
          'Con đã nêu đúng 3 bước tìm ƯCLN và BCNN kèm ví dụ 24 và 36 rất rõ ràng. Tuy nhiên con cần bổ sung khái niệm hai số nguyên tố cùng nhau để hoàn thiện phần rút gọn phân số tối giản.',
        incorrectPoints: [],
        missingPoints: ['Chưa nêu ví dụ hoặc định nghĩa hai số nguyên tố cùng nhau (ƯCLN = 1).'],
        confidence: 'HIGH',
        sources: [
          {
            id: 701,
            title: 'Toán 6 Kết nối tri thức – Bài 11: Ước chung và Ước chung lớn nhất',
            url: 'https://loigiaihay.com/toan-6-ket-noi-tri-thuc-bai-11',
            snippet: 'Hai số có ƯCLN bằng 1 gọi là hai số nguyên tố cùng nhau.',
            retrievedAt: '2026-11-24T14:02:00Z',
          },
        ],
        evaluatedAt: '2026-11-24T14:02:10Z',
      };

      const evalV2: AIEvaluationResponse = {
        id: 502,
        submissionId: 1002,
        accuracy: 'CORRECT',
        completeness: 'COMPLETE',
        overallStatus: 'GOOD',
        feedback:
          'Bài ôn tập rất tốt! Con đã nắm chắc cách phân tích thừa số nguyên tố tìm ƯCLN, BCNN và bổ sung chính xác định nghĩa hai số nguyên tố cùng nhau kèm ví dụ.',
        incorrectPoints: [],
        missingPoints: [],
        confidence: 'HIGH',
        sources: [
          {
            id: 702,
            title: 'Toán 6 Kết nối tri thức – Bài 11: Ước chung và Ước chung lớn nhất',
            url: 'https://loigiaihay.com/toan-6-ket-noi-tri-thuc-bai-11',
            snippet: 'Phân số tối giản khi tử và mẫu là hai số nguyên tố cùng nhau.',
            retrievedAt: '2026-11-24T14:15:00Z',
          },
          {
            id: 703,
            title: 'Wikipedia – Bội chung nhỏ nhất & Ước chung lớn nhất',
            url: 'https://vi.wikipedia.org/wiki/%C6%AF%E1%BB%9Bc_chung_l%E1%BB%9Bn_nh%E1%BA%A5t',
            snippet: 'Hai số nguyên tố cùng nhau khi ước chung lớn nhất bằng 1.',
            retrievedAt: '2026-11-24T14:15:02Z',
          },
        ],
        evaluatedAt: '2026-11-24T14:15:12Z',
      };

      const subV1: StudySubmissionResponse = {
        id: 1001,
        taskId: reviewId,
        version: 1,
        isCurrent: false,
        lessonTitle: 'Ôn tập ƯCLN và BCNN',
        content: MATH_REVIEW_V1_CONTENT,
        charCount: countNormalizedChars(MATH_REVIEW_V1_CONTENT),
        submittedAt: '2026-11-24T14:01:40Z',
        evaluation: evalV1,
        lastError: null,
      };

      const subV2: StudySubmissionResponse = {
        id: 1002,
        taskId: reviewId,
        version: 2,
        isCurrent: true,
        lessonTitle: 'Ôn tập ƯCLN, BCNN và số nguyên tố cùng nhau',
        content: MATH_REVIEW_V2_CONTENT,
        charCount: countNormalizedChars(MATH_REVIEW_V2_CONTENT),
        submittedAt: '2026-11-24T14:14:50Z',
        evaluation: evalV2,
        lastError: null,
      };

      const reviewSummary: StudyTaskSummaryResponse = {
        id: reviewId,
        subjectCode: 'MATH',
        subjectName,
        taskType: 'REVIEW',
        status: 'EVALUATED',
        revisionCount: 1,
        maxRevisions: 2,
        currentLessonTitle: subV2.lessonTitle,
        latestEvaluationStatus: 'GOOD',
      };

      const previewSummary: StudyTaskSummaryResponse = {
        id: previewId,
        subjectCode: 'MATH',
        subjectName,
        taskType: 'PREVIEW',
        status: 'NOT_STARTED',
        revisionCount: 0,
        maxRevisions: 2,
        currentLessonTitle: null,
        latestEvaluationStatus: null,
      };

      return {
        subjectCode: item.code,
        periodNote: item.periodNote,
        reviewTask: {
          task: reviewSummary,
          targetDate,
          reviewCompleted: true,
          submissions: [subV2, subV1],
        },
        previewTask: {
          task: previewSummary,
          targetDate,
          reviewCompleted: true,
          submissions: [],
        },
      };
    }

    if (item.code === 'SCI') {
      const evalSci: AIEvaluationResponse = {
        id: 503,
        submissionId: 1003,
        accuracy: 'CORRECT',
        completeness: 'INCOMPLETE',
        overallStatus: 'NEEDS_IMPROVEMENT',
        feedback:
          'Con trình bày chính xác đơn vị đo (m, kg, s) và thao tác đặt thước đo chiều dài. Hãy bổ sung thêm cách đọc vạch chia khi dùng cân đồng hồ và cách quy đổi phút sang giây.',
        incorrectPoints: [],
        missingPoints: ['Chưa nêu ví dụ quy đổi 1 phút = 60 giây, 1 giờ = 3600 giây.'],
        confidence: 'HIGH',
        sources: [
          {
            id: 704,
            title: 'KHTN Kết nối tri thức – Các phép đo cơ bản',
            url: 'https://loigiaihay.com/khtn-6-ket-noi-tri-thuc-cac-phep-do',
            snippet: 'Đơn vị đo thời gian là giây (s), 1 phút = 60 giây.',
            retrievedAt: '2026-11-24T14:32:00Z',
          },
          {
            id: 705,
            title: 'Wikipedia – Hệ đo lường quốc tế SI',
            url: 'https://vi.wikipedia.org/wiki/H%E1%BB%87_%C4%91o_l%C6%B0%E1%BB%9Dng_qu%E1%BB%91c_t%E1%BA%BF',
            snippet: 'Mét (m), kilôgam (kg), giây (s) là các đơn vị cơ bản.',
            retrievedAt: '2026-11-24T14:32:01Z',
          },
        ],
        evaluatedAt: '2026-11-24T14:32:10Z',
      };

      const evalSciFallback: AIEvaluationResponse = {
        id: 504,
        submissionId: 1004,
        accuracy: 'UNVERIFIABLE',
        completeness: 'UNDETERMINED',
        overallStatus: 'INSUFFICIENT_EVIDENCE',
        feedback:
          'Hệ thống AI đang gặp lỗi phản hồi sau 3 lần thử tự động. Phụ huynh vui lòng tự xem và kiểm tra trực tiếp bài làm của con bên dưới nhé.',
        incorrectPoints: [],
        missingPoints: [],
        confidence: 'LOW',
        sources: [],
        evaluatedAt: '2026-11-24T14:27:00Z',
      };

      const subSci: StudySubmissionResponse = {
        id: 1003,
        taskId: previewId,
        version: 1,
        isCurrent: true,
        lessonTitle: 'Soạn bài Các phép đo cơ bản',
        content: SCI_PREVIEW_V1_CONTENT,
        charCount: countNormalizedChars(SCI_PREVIEW_V1_CONTENT),
        submittedAt: '2026-11-24T14:31:30Z',
        evaluation: evalSci,
        lastError: null,
      };

      return {
        subjectCode: item.code,
        periodNote: item.periodNote,
        reviewTask: {
          task: {
            id: reviewId,
            subjectCode: 'SCI',
            subjectName,
            taskType: 'REVIEW',
            status: 'EVALUATED',
            revisionCount: 0,
            maxRevisions: 2,
            currentLessonTitle: 'Ôn bài Dụng cụ đo phòng thí nghiệm',
            latestEvaluationStatus: 'INSUFFICIENT_EVIDENCE',
          },
          targetDate,
          reviewCompleted: true,
          submissions: [
            {
              id: 1004,
              taskId: reviewId,
              version: 1,
              isCurrent: true,
              lessonTitle: 'Ôn bài Dụng cụ đo phòng thí nghiệm',
              content: SCI_PREVIEW_V1_CONTENT,
              charCount: countNormalizedChars(SCI_PREVIEW_V1_CONTENT),
              submittedAt: '2026-11-24T14:25:00Z',
              evaluation: evalSciFallback,
              lastError: null,
            },
          ],
        },
        previewTask: {
          task: {
            id: previewId,
            subjectCode: 'SCI',
            subjectName,
            taskType: 'PREVIEW',
            status: 'EVALUATED',
            revisionCount: 0,
            maxRevisions: 2,
            currentLessonTitle: subSci.lessonTitle,
            latestEvaluationStatus: 'NEEDS_IMPROVEMENT',
          },
          targetDate,
          reviewCompleted: true,
          submissions: [subSci],
        },
      };
    }

    return {
      subjectCode: item.code,
      periodNote: item.periodNote,
      reviewTask: {
        task: {
          id: reviewId,
          subjectCode: item.code,
          subjectName,
          taskType: 'REVIEW',
          status: 'NOT_STARTED',
          revisionCount: 0,
          maxRevisions: 2,
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
          subjectCode: item.code,
          subjectName,
          taskType: 'PREVIEW',
          status: 'BLOCKED',
          revisionCount: 0,
          maxRevisions: 2,
          currentLessonTitle: null,
          latestEvaluationStatus: null,
        },
        targetDate,
        reviewCompleted: false,
        submissions: [],
      },
    };
  });

  return { targetDate, weekdayLabel, buckets };
}

export function buildGuardianDashboardFromBuckets(
  buckets: SeededTaskBucket[],
  targetDate: string,
  selectedSubject: SubjectCode | 'ALL'
): GuardianDashboardResponse {
  const allItems = buckets.flatMap((bucket) => [bucket.reviewTask, bucket.previewTask]);
  const filtered = allItems.filter((detail) =>
    selectedSubject === 'ALL' ? true : detail.task.subjectCode === selectedSubject
  );

  const items = filtered.map((detail) => {
    const currentSub =
      detail.submissions.find((s) => s.isCurrent) ?? detail.submissions[0] ?? null;
    const evaluation = currentSub?.evaluation ?? null;

    return {
      taskId: detail.task.id,
      currentSubmissionId: currentSub?.id ?? null,
      targetDate: detail.targetDate,
      subjectCode: detail.task.subjectCode,
      subjectName: detail.task.subjectName,
      taskType: detail.task.taskType,
      status: detail.task.status,
      lessonTitle: detail.task.currentLessonTitle,
      revisionCount: detail.task.revisionCount,
      guardianAcknowledgedAt: detail.task.guardianAcknowledgedAt ?? null,
      guardianNote: detail.task.guardianNote ?? null,
      evaluation: evaluation
        ? {
            accuracy: evaluation.accuracy,
            completeness: evaluation.completeness,
            overallStatus: evaluation.overallStatus,
            feedback: evaluation.feedback,
            evaluatedAt: evaluation.evaluatedAt,
          }
        : null,
    };
  });

  return {
    linkedStudentName: 'Nguyễn Minh Khôi',
    grade: 6,
    selectedDate: targetDate,
    selectedSubject,
    totalTasks: items.length,
    completedTasks: items.filter((i) => i.status === 'EVALUATED').length,
    evaluatingTasks: items.filter((i) => i.status === 'EVALUATING').length,
    items,
  };
}

export function findGuardianSubmissionDetail(
  buckets: SeededTaskBucket[],
  submissionId: number
): GuardianSubmissionDetailResponse | null {
  for (const bucket of buckets) {
    for (const detail of [bucket.reviewTask, bucket.previewTask]) {
      if (detail.submissions.some((s) => s.id === submissionId)) {
        return {
          submissionId,
          taskId: detail.task.id,
          studentName: 'Nguyễn Minh Khôi',
          targetDate: detail.targetDate,
          subjectCode: detail.task.subjectCode,
          subjectName: detail.task.subjectName,
          taskType: detail.task.taskType,
          status: detail.task.status,
          revisionCount: detail.task.revisionCount,
          guardianAcknowledgedAt: detail.task.guardianAcknowledgedAt ?? null,
          guardianNote: detail.task.guardianNote ?? null,
          versions: detail.submissions,
        };
      }
    }
  }
  return null;
}
