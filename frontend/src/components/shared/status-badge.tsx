import type { LucideIcon } from 'lucide-react';
import {
  AlertCircle,
  Ban,
  CheckCircle2,
  Circle,
  Clock,
  Crown,
  GitPullRequest,
  PlayCircle,
  Send,
  ShieldCheck,
  User,
  XCircle,
} from 'lucide-react';
import { Badge, type BadgeProps } from '@/components/ui/badge';
import type {
  ComplexityLevel,
  DeliverableStatus,
  InvitationStatus,
  MilestoneStatus,
  ProjectRole,
  TaskStatus,
  TestStatus,
  TrackingStatus,
} from '@/types';

export type DomainStatusValue =
  | MilestoneStatus
  | DeliverableStatus
  | InvitationStatus
  | TaskStatus
  | TestStatus
  | TrackingStatus
  | ComplexityLevel
  | ProjectRole;

interface StatusStyleConfig {
  label: string;
  variant: NonNullable<BadgeProps['variant']>;
  icon: LucideIcon;
}

const STATUS_MAP: Record<DomainStatusValue, StatusStyleConfig> = {
  APPROVED: { label: 'Đã duyệt', variant: 'success', icon: CheckCircle2 },
  SUBMITTED: { label: 'Đã nộp', variant: 'info', icon: Send },
  NEEDS_REVISION: { label: 'Cần sửa', variant: 'warning', icon: AlertCircle },
  PENDING: { label: 'Chờ nộp', variant: 'secondary', icon: Clock },
  ACCEPTED: { label: 'Accepted', variant: 'success', icon: CheckCircle2 },
  REVOKED: { label: 'Revoked', variant: 'danger', icon: Ban },
  EXPIRED: { label: 'Expired', variant: 'secondary', icon: AlertCircle },
  TODO: { label: 'To Do', variant: 'secondary', icon: Circle },
  IN_PROGRESS: { label: 'In Progress', variant: 'info', icon: PlayCircle },
  IN_REVIEW: { label: 'In Review', variant: 'warning', icon: GitPullRequest },
  DONE: { label: 'Done', variant: 'success', icon: CheckCircle2 },
  UNTESTED: { label: 'Untested', variant: 'secondary', icon: Circle },
  PASS: { label: 'Pass', variant: 'success', icon: CheckCircle2 },
  FAIL: { label: 'Fail', variant: 'danger', icon: XCircle },
  OPEN: { label: 'Open', variant: 'danger', icon: AlertCircle },
  RESOLVED: { label: 'Resolved', variant: 'success', icon: ShieldCheck },
  CLOSED: { label: 'Closed', variant: 'secondary', icon: CheckCircle2 },
  SIMPLE: { label: 'Simple', variant: 'success', icon: Circle },
  MEDIUM: { label: 'Medium', variant: 'warning', icon: AlertCircle },
  COMPLEX: { label: 'Complex', variant: 'danger', icon: AlertCircle },
  LEADER: { label: 'Leader', variant: 'brand', icon: Crown },
  MEMBER: { label: 'Member', variant: 'secondary', icon: User },
};

export interface StatusBadgeProps {
  status: DomainStatusValue;
  labelOverride?: string;
  className?: string;
}

export function StatusBadge({
  status,
  labelOverride,
  className,
}: StatusBadgeProps) {
  const config = STATUS_MAP[status];
  const Icon = config.icon;
  return (
    <Badge variant={config.variant} className={className}>
      <Icon className="h-3.5 w-3.5 shrink-0" strokeWidth={1.75} />
      <span>{labelOverride ?? config.label}</span>
    </Badge>
  );
}
