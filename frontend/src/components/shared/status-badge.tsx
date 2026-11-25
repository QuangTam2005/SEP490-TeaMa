import type { LucideIcon } from 'lucide-react';
import {
  AlertCircle,
  AlertTriangle,
  CheckCircle2,
  Circle,
  Clock,
  HelpCircle,
  Lock,
  Send,
  ShieldCheck,
  Sparkles,
  User,
  XCircle,
} from 'lucide-react';
import { Badge, type BadgeProps } from '@/components/ui/badge';
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
  icon: LucideIcon;
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
  BLOCKED: { label: 'Làm Ôn bài trước', variant: 'secondary', icon: Lock },

  CORRECT: { label: 'Chính xác', variant: 'success', icon: CheckCircle2 },
  PARTIALLY_CORRECT: {
    label: 'Đúng một phần',
    variant: 'warning',
    icon: AlertTriangle,
  },
  INCORRECT: { label: 'Cần sửa thêm', variant: 'danger', icon: AlertCircle },
  UNVERIFIABLE: {
    label: 'Chưa đủ chứng cứ',
    variant: 'secondary',
    icon: HelpCircle,
  },

  COMPLETE: { label: 'Đầy đủ ý', variant: 'success', icon: CheckCircle2 },
  INCOMPLETE: {
    label: 'Còn thiếu ý',
    variant: 'warning',
    icon: AlertTriangle,
  },
  UNDETERMINED: {
    label: 'Chưa xác định',
    variant: 'secondary',
    icon: HelpCircle,
  },

  GOOD: { label: 'Tốt', variant: 'success', icon: CheckCircle2 },
  NEEDS_IMPROVEMENT: {
    label: 'Cần bổ sung',
    variant: 'warning',
    icon: AlertTriangle,
  },
  INSUFFICIENT_EVIDENCE: {
    label: 'Thiếu nguồn đối chiếu',
    variant: 'secondary',
    icon: HelpCircle,
  },

  HIGH: { label: 'Tin cậy cao', variant: 'success', icon: CheckCircle2 },
  MEDIUM: { label: 'Tin cậy vừa', variant: 'info', icon: AlertCircle },
  LOW: { label: 'Tin cậy thấp', variant: 'warning', icon: HelpCircle },
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
    icon: AlertCircle,
  };
  const Icon = config.icon;
  return (
    <Badge variant={config.variant} className={className}>
      <Icon className="h-3.5 w-3.5 shrink-0" strokeWidth={1.75} />
      <span>{labelOverride ?? config.label}</span>
    </Badge>
  );
}
