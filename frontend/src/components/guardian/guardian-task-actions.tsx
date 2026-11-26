'use client';

import Link from 'next/link';
import { ExternalLink, FolderUp } from 'lucide-react';
import { Button } from '@/components/ui/button';
import type { GuardianTaskItem } from '@/types';

export function canAssignCustomTask(row: GuardianTaskItem): boolean {
  return (
    !row.currentSubmissionId &&
    !row.customSubmittedAt &&
    row.status !== 'SUBMITTED' &&
    row.status !== 'EVALUATING' &&
    row.status !== 'EVALUATED' &&
    row.status !== 'EVALUATION_FAILED'
  );
}

export function canToggleSkip(row: GuardianTaskItem): boolean {
  return (
    !row.currentSubmissionId &&
    !row.customSubmittedAt &&
    (row.status === 'NOT_STARTED' ||
      row.status === 'IN_PROGRESS' ||
      row.status === 'BLOCKED' ||
      row.status === 'SKIPPED')
  );
}

export interface GuardianTaskActionHandlers {
  onVerifyApprove: (taskId: number) => void;
  onOpenVerifyRedo: (task: GuardianTaskItem) => void;
  onApproveSkip: (taskId: number) => void;
  onOpenRejectSkip: (task: GuardianTaskItem) => void;
  onOpenAssignCustom: (task: GuardianTaskItem) => void;
  onClearCustom: (taskId: number) => void;
  onToggleSkip: (taskId: number, skipped: boolean) => void;
  verifyingTaskId: number | null;
  reviewingSkipTaskId: number | null;
  togglingSkipTaskId: number | null;
  clearingCustomTaskId: number | null;
}

interface GuardianTaskActionsProps extends GuardianTaskActionHandlers {
  row: GuardianTaskItem;
  variant: 'table' | 'mobile';
}

export function GuardianTaskActions({
  row,
  variant,
  onVerifyApprove,
  onOpenVerifyRedo,
  onApproveSkip,
  onOpenRejectSkip,
  onOpenAssignCustom,
  onClearCustom,
  onToggleSkip,
  verifyingTaskId,
  reviewingSkipTaskId,
  togglingSkipTaskId,
  clearingCustomTaskId,
}: GuardianTaskActionsProps) {
  const isTable = variant === 'table';
  const stopIfTable = (e: React.MouseEvent) => {
    if (isTable) {
      e.stopPropagation();
    }
  };

  if (row.currentSubmissionId) {
    return (
      <Button
        asChild
        size={isTable ? 'sm' : 'default'}
        variant="outline"
        className={
          isTable
            ? 'h-8 whitespace-nowrap px-2.5 text-xs'
            : 'h-11 w-full justify-center'
        }
      >
        <Link href={`/guardian/submissions/${row.currentSubmissionId}`}>
          {isTable ? 'Xem bài gốc' : 'Xem bài gốc & nhận xét'}
        </Link>
      </Button>
    );
  }

  if (row.assignmentMode === 'CUSTOM_DRIVE' && row.status === 'SUBMITTED') {
    const isVerifying = verifyingTaskId === row.taskId;
    return (
      <div
        className={
          isTable
            ? 'flex flex-nowrap items-center justify-end gap-1.5'
            : 'grid grid-cols-2 gap-2'
        }
      >
        <Button
          type="button"
          size={isTable ? 'sm' : 'default'}
          disabled={isVerifying}
          className={
            isTable
              ? 'h-8 whitespace-nowrap px-2.5 text-xs'
              : 'h-11 w-full justify-center'
          }
          onClick={(e) => {
            stopIfTable(e);
            onVerifyApprove(row.taskId);
          }}
        >
          Xác nhận đạt
        </Button>
        <Button
          type="button"
          size={isTable ? 'sm' : 'default'}
          variant="outline"
          disabled={isVerifying}
          className={
            isTable
              ? 'h-8 whitespace-nowrap px-2.5 text-xs'
              : 'h-11 w-full justify-center'
          }
          onClick={(e) => {
            stopIfTable(e);
            onOpenVerifyRedo(row);
          }}
        >
          Nhắc làm lại
        </Button>
      </div>
    );
  }

  if (row.skipRequestStatus === 'PENDING') {
    const isReviewing = reviewingSkipTaskId === row.taskId;
    const assignBtn = canAssignCustomTask(row) ? (
      <Button
        type="button"
        size={isTable ? 'sm' : 'default'}
        variant="outline"
        className={
          isTable
            ? 'h-8 whitespace-nowrap px-2.5 text-xs'
            : 'h-11 w-full justify-center gap-1.5'
        }
        onClick={(e) => {
          stopIfTable(e);
          onOpenAssignCustom(row);
        }}
      >
        {!isTable ? <FolderUp className="h-4 w-4" /> : null}
        Giao bài thay thế
      </Button>
    ) : null;

    if (isTable) {
      return (
        <div className="flex flex-nowrap items-center justify-end gap-1.5">
          <Button
            type="button"
            size="sm"
            disabled={isReviewing}
            className="h-8 whitespace-nowrap px-2.5 text-xs"
            onClick={(e) => {
              e.stopPropagation();
              onApproveSkip(row.taskId);
            }}
          >
            Đồng ý
          </Button>
          <Button
            type="button"
            size="sm"
            variant="outline"
            disabled={isReviewing}
            className="h-8 whitespace-nowrap px-2.5 text-xs"
            onClick={(e) => {
              e.stopPropagation();
              onOpenRejectSkip(row);
            }}
          >
            Từ chối
          </Button>
          {assignBtn}
        </div>
      );
    }

    return (
      <div className="space-y-2">
        <div className="grid grid-cols-2 gap-2">
          <Button
            type="button"
            disabled={isReviewing}
            className="h-11 w-full justify-center"
            onClick={() => onApproveSkip(row.taskId)}
          >
            Đồng ý bỏ qua
          </Button>
          <Button
            type="button"
            variant="outline"
            disabled={isReviewing}
            className="h-11 w-full justify-center"
            onClick={() => onOpenRejectSkip(row)}
          >
            Từ chối
          </Button>
        </div>
        {assignBtn}
      </div>
    );
  }

  if (canToggleSkip(row) || canAssignCustomTask(row)) {
    const isSkipped = row.status === 'SKIPPED';
    const isCustom = row.assignmentMode === 'CUSTOM_DRIVE';
    const isBusy =
      togglingSkipTaskId === row.taskId || clearingCustomTaskId === row.taskId;

    return (
      <div
        className={
          isTable
            ? 'flex flex-nowrap items-center justify-end gap-1.5'
            : 'space-y-2'
        }
      >
        {canAssignCustomTask(row) ? (
          <Button
            type="button"
            size={isTable ? 'sm' : 'default'}
            variant="outline"
            disabled={isBusy}
            className={
              isTable
                ? 'h-8 whitespace-nowrap px-2.5 text-xs'
                : 'h-11 w-full justify-center gap-1.5'
            }
            onClick={(e) => {
              stopIfTable(e);
              onOpenAssignCustom(row);
            }}
          >
            {!isTable ? <FolderUp className="h-4 w-4" /> : null}
            {isCustom ? 'Sửa bài giao' : 'Giao bài thay thế'}
          </Button>
        ) : null}
        {isCustom && !row.customSubmittedAt ? (
          <Button
            type="button"
            size={isTable ? 'sm' : 'default'}
            variant="ghost"
            disabled={isBusy}
            className={
              isTable
                ? 'h-8 whitespace-nowrap px-2 text-xs'
                : 'h-11 w-full justify-center'
            }
            onClick={(e) => {
              stopIfTable(e);
              onClearCustom(row.taskId);
            }}
          >
            {isTable ? 'Gỡ thay thế' : 'Gỡ bài giao thay thế'}
          </Button>
        ) : null}
        {canToggleSkip(row) ? (
          <Button
            type="button"
            size={isTable ? 'sm' : 'default'}
            variant="outline"
            disabled={isBusy}
            className={
              isTable
                ? 'h-8 whitespace-nowrap px-2.5 text-xs'
                : 'h-11 w-full justify-center'
            }
            onClick={(e) => {
              stopIfTable(e);
              onToggleSkip(row.taskId, !isSkipped);
            }}
          >
            {isTable
              ? isBusy
                ? 'Đang xử lý...'
                : isSkipped
                  ? 'Khôi phục'
                  : 'Bỏ qua'
              : isSkipped
                ? 'Khôi phục bài học'
                : 'Đánh dấu bỏ qua hôm nay'}
          </Button>
        ) : null}
      </div>
    );
  }

  if (isTable && row.assignmentMode === 'CUSTOM_DRIVE' && row.customDriveFolderUrl) {
    return (
      <Button
        asChild
        size="sm"
        variant="outline"
        className="h-8 gap-1 whitespace-nowrap px-2.5 text-xs"
      >
        <a
          href={row.customDriveFolderUrl}
          target="_blank"
          rel="noopener noreferrer"
          onClick={(e) => e.stopPropagation()}
        >
          Mở Drive
          <ExternalLink className="h-3.5 w-3.5" />
        </a>
      </Button>
    );
  }

  return isTable ? <span className="text-xs text-muted-foreground">—</span> : null;
}
