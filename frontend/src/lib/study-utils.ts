import type { SubjectCode, Weekday } from '@/types';

export const MIN_SUBMISSION_CHARS = 400;
export const MAX_SUBMISSION_CHARS = 3000;
export const MAX_FEEDBACK_CHARS = 1000;
/**
 * Maximum immutable versions per task is 3 (v1 initial + up to 2 revisions: v2, v3).
 */
export const MAX_SUBMISSION_VERSIONS = 3;
export const MAX_POST_EVAL_REVISIONS = 2;

export const GRADE_OPTIONS = Array.from({ length: 12 }, (_, index) => ({
  value: index + 1,
  label: `Lớp ${index + 1}`,
}));

export interface SubjectCatalogItem {
  code: SubjectCode;
  name: string;
  shortLabel: string;
  minGrade: number;
  maxGrade: number;
}

export const ALL_SUBJECTS: SubjectCatalogItem[] = [
  { code: 'MATH', name: 'Toán', shortLabel: 'Toán', minGrade: 1, maxGrade: 12 },
  { code: 'LIT', name: 'Ngữ văn', shortLabel: 'Văn', minGrade: 1, maxGrade: 12 },
  { code: 'ENG', name: 'Tiếng Anh', shortLabel: 'Anh', minGrade: 1, maxGrade: 12 },
  { code: 'SCI', name: 'Khoa học tự nhiên', shortLabel: 'KHTN', minGrade: 1, maxGrade: 9 },
  { code: 'HISGEO', name: 'Lịch sử – Địa lý', shortLabel: 'Sử - Địa', minGrade: 1, maxGrade: 9 },
  { code: 'PHY', name: 'Vật lý', shortLabel: 'Lý', minGrade: 10, maxGrade: 12 },
  { code: 'CHEM', name: 'Hóa học', shortLabel: 'Hóa', minGrade: 10, maxGrade: 12 },
  { code: 'BIO', name: 'Sinh học', shortLabel: 'Sinh', minGrade: 10, maxGrade: 12 },
  { code: 'HIST', name: 'Lịch sử', shortLabel: 'Sử', minGrade: 10, maxGrade: 12 },
  { code: 'GEO', name: 'Địa lý', shortLabel: 'Địa', minGrade: 10, maxGrade: 12 },
  { code: 'INFO', name: 'Tin học', shortLabel: 'Tin', minGrade: 3, maxGrade: 12 },
  { code: 'TECH', name: 'Công nghệ', shortLabel: 'Công nghệ', minGrade: 3, maxGrade: 12 },
  { code: 'CIVICS', name: 'Giáo dục công dân', shortLabel: 'GDCD', minGrade: 6, maxGrade: 12 },
];

export const GRADE_6_SUBJECTS: {
  code: SubjectCode;
  name: string;
  shortLabel: string;
}[] = ALL_SUBJECTS.filter((s) => 6 >= s.minGrade && 6 <= s.maxGrade);

export function getSubjectsForGrade(grade: number | null | undefined): SubjectCatalogItem[] {
  const safeGrade = grade && grade >= 1 && grade <= 12 ? grade : 6;
  return ALL_SUBJECTS.filter(
    (s) => safeGrade >= s.minGrade && safeGrade <= s.maxGrade
  );
}

export const WEEKDAYS: { value: Weekday; label: string; shortLabel: string }[] = [
  { value: 1, label: 'Thứ Hai', shortLabel: 'T2' },
  { value: 2, label: 'Thứ Ba', shortLabel: 'T3' },
  { value: 3, label: 'Thứ Tư', shortLabel: 'T4' },
  { value: 4, label: 'Thứ Năm', shortLabel: 'T5' },
  { value: 5, label: 'Thứ Sáu', shortLabel: 'T6' },
  { value: 6, label: 'Thứ Bảy', shortLabel: 'T7' },
  { value: 7, label: 'Chủ Nhật', shortLabel: 'CN' },
];

/**
 * Normalizes line endings (\r\n -> \n) and trims leading/trailing whitespace
 * per SRS BR-12 so Frontend and Backend character counts always agree.
 */
export function normalizeStudyText(raw: string): string {
  return raw.replace(/\r\n/g, '\n').trim();
}

export function countNormalizedChars(raw: string): number {
  return normalizeStudyText(raw).length;
}

export function isValidStudyLength(raw: string): boolean {
  const count = countNormalizedChars(raw);
  return count >= MIN_SUBMISSION_CHARS && count <= MAX_SUBMISSION_CHARS;
}

export function getSubjectName(code: SubjectCode): string {
  return ALL_SUBJECTS.find((s) => s.code === code)?.name ?? code;
}

/**
 * Returns tomorrow's ISO date string (YYYY-MM-DD) in Asia/Ho_Chi_Minh timezone.
 */
export function getNextTargetDateIso(): string {
  const now = new Date();
  const vnNow = new Date(
    now.toLocaleString('en-US', { timeZone: 'Asia/Ho_Chi_Minh' })
  );
  vnNow.setDate(vnNow.getDate() + 1);
  const year = vnNow.getFullYear();
  const month = String(vnNow.getMonth() + 1).padStart(2, '0');
  const day = String(vnNow.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function getWeekdayFromIsoDate(isoDate: string): Weekday {
  const [y, m, d] = isoDate.split('-').map(Number);
  const utcDay = new Date(Date.UTC(y, (m || 1) - 1, d || 1)).getUTCDay();
  return (utcDay === 0 ? 7 : utcDay) as Weekday;
}

export function getWeekdayLabel(weekday: Weekday): string {
  return WEEKDAYS.find((w) => w.value === weekday)?.label ?? '';
}

export function formatViDate(isoDate: string): string {
  const parts = isoDate.split('-');
  if (parts.length !== 3) {
    return isoDate;
  }
  return `${parts[2]}/${parts[1]}/${parts[0]}`;
}
