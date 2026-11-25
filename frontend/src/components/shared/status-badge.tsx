import type { LucideIcon } from 'lucide-react';
import {
  AlertCircle,
  CheckCircle2,
  Circle,
  Clock,
  Lock,
  Send,
  ShieldCheck,
  User,
  XCircle,
} from 'lucide-react';
import { Badge, type BadgeProps } from '@/components/ui/badge';
import type { UserRole } from '@/types';

export type StudyBadgeStatus =
  | UserRole
  | 'NOT_STARTED'
  | 'IN_PROGRESS'
  | 'SUBMITTED'
  | 'EVALUATING'
  | 'EVALUATED'
  | 'EVALUATION_FAILED'
  | 'BLOCKED';

interface StatusStyleConfig {
  label: string;
  variant: NonNullable<BadgeProps['variant']>;
  icon: LucideIcon;
}

const STATUS_MAP: Record<StudyBadgeStatus, StatusStyleConfig> = {
  STUDENT: { label: 'Học sinh', variant: 'brand', icon: User },
  GUARDIAN: { label: 'Phụ huynh', variant: 'info', icon: ShieldCheck },
  NOT_STARTED: { label: 'Chưa bắt đầu', variant: 'secondary', icon: Circle },
  IN_PROGRESS: { label: 'Đang nhập', variant: 'info', icon: Clock },
  SUBMITTED: { label: 'Đã gửi', variant: 'info', icon: Send },
  EVALUATING: { label: 'Đang AI đánh giá', variant: 'warning', icon: Clock },
  EVALUATED: { label: 'Đã có kết quả', variant: 'success', icon: CheckCircle2 },
  EVALUATION_FAILED: {
    label: 'Chưa thể đánh giá',
    variant: 'danger',
    icon: XCircle,
  },
  BLOCKED: { label: 'Cần hoàn thành bài cũ', variant: 'secondary', icon: Lock },
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
