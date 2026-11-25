import type { LucideIcon } from 'lucide-react';
import {
  CheckCircle2,
  Circle,
  Clock,
  Lock,
  Send,
  ShieldCheck,
  SkipForward,
  Sparkles,
  User,
  XCircle,
} from 'lucide-react';
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
  icon?: LucideIcon;
}

const STATUS_MAP: Record<StudyBadgeStatus, StatusStyleConfig> = {
  STUDENT: { label: 'Học sinh', variant: 'brand', icon: User },
  GUARDIAN: { label: 'Phụ huynh', variant: 'info', icon: ShieldCheck },

  NOT_STARTED: { label: 'Chưa làm', variant: 'secondary', icon: Circle },
  IN_PROGRESS: { label: 'Đang viết', variant: 'brand', icon: Clock },
  SUBMITTED: { label: 'Đã nộp', variant: 'info', icon: Send },
  EVALUATING: { label: 'AI đang xem', variant: 'warning', icon: Sparkles },
  EVALUATED: { label: 'Đã xem xong', variant: 'success', icon: CheckCircle2 },
  EVALUATION_FAILED: {
    label: 'Chưa xem được',
    variant: 'danger',
    icon: XCircle,
  },
  BLOCKED: { label: 'Chưa mở', variant: 'secondary', icon: Lock },
  SKIPPED: { label: 'Bỏ qua', variant: 'secondary', icon: SkipForward },

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
  const Icon = config.icon;
  return (
    <Badge variant={config.variant} className={cn('whitespace-nowrap', className)}>
      {Icon ? <Icon className="h-3.5 w-3.5 shrink-0" strokeWidth={1.75} /> : null}
      <span>{labelOverride ?? config.label}</span>
    </Badge>
  );
}
