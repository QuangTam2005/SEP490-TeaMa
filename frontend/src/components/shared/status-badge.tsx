import { Badge, type BadgeProps } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import type {
  AccuracyLevel,
  CompletenessLevel,
  ConfidenceLevel,
  OverallEvaluationStatus,
  StudyTaskStatus,
  UserRole,
} from '@/types';

export type StudyBadgeStatus =
  | UserRole
  | StudyTaskStatus
  | AccuracyLevel
  | CompletenessLevel
  | OverallEvaluationStatus
  | ConfidenceLevel;

interface StatusStyleConfig {
  label: string;
  variant: NonNullable<BadgeProps['variant']>;
}

const STATUS_MAP: Record<StudyBadgeStatus, StatusStyleConfig> = {
  STUDENT: { label: 'Học sinh', variant: 'brand' },
  GUARDIAN: { label: 'Phụ huynh', variant: 'info' },

  NOT_STARTED: { label: 'Chưa làm', variant: 'secondary' },
  IN_PROGRESS: { label: 'Đang viết', variant: 'brand' },
  SUBMITTED: { label: 'Đã nộp', variant: 'info' },
  EVALUATING: { label: 'AI đang xem', variant: 'warning' },
  EVALUATED: { label: 'Đã xem xong', variant: 'success' },
  EVALUATION_FAILED: {
    label: 'Chưa xem được',
    variant: 'danger',
  },
  BLOCKED: { label: 'Chưa mở', variant: 'secondary' },
  SKIPPED: { label: 'Bỏ qua', variant: 'secondary' },

  CORRECT: { label: 'Chính xác', variant: 'success' },
  PARTIALLY_CORRECT: { label: 'Đúng một phần', variant: 'warning' },
  INCORRECT: { label: 'Cần sửa thêm', variant: 'danger' },
  UNVERIFIABLE: { label: 'Chưa đủ chứng cứ', variant: 'secondary' },

  COMPLETE: { label: 'Đầy đủ ý', variant: 'success' },
  INCOMPLETE: { label: 'Còn thiếu ý', variant: 'warning' },
  UNDETERMINED: { label: 'Chưa xác định', variant: 'secondary' },

  GOOD: { label: 'Tốt', variant: 'success' },
  NEEDS_IMPROVEMENT: { label: 'Cần bổ sung', variant: 'warning' },
  INSUFFICIENT_EVIDENCE: { label: 'Thiếu nguồn đối chiếu', variant: 'secondary' },

  HIGH: { label: 'Tin cậy cao', variant: 'success' },
  MEDIUM: { label: 'Tin cậy vừa', variant: 'info' },
  LOW: { label: 'Tin cậy thấp', variant: 'warning' },
};

export interface StatusBadgeProps {
  status: StudyBadgeStatus;
  labelOverride?: string;
  className?: string;
}

export function StatusBadge({
  status,
  labelOverride,
  className,
}: StatusBadgeProps) {
  const config = STATUS_MAP[status] ?? {
    label: status,
    variant: 'secondary' as const,
  };
  return (
    <Badge variant={config.variant} className={cn('whitespace-nowrap', className)}>
      <span>{labelOverride ?? config.label}</span>
    </Badge>
  );
}
