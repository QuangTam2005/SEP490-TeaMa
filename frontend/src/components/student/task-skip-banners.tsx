'use client';

import { Button } from '@/components/ui/button';
import type { StudyTaskSummaryResponse } from '@/types';

interface TaskSkipBannersProps {
  task: StudyTaskSummaryResponse;
  isCancellingSkip: boolean;
  onCancelSkip: (taskId: number) => void;
}

export function TaskSkipBanners({
  task,
  isCancellingSkip,
  onCancelSkip,
}: TaskSkipBannersProps) {
  return (
    <>
      {task.skipRequestStatus === 'PENDING' && task.skipRequestReason ? (
        <div className="flex flex-col gap-2 rounded-xl border border-amber-200 bg-amber-50/70 px-4 py-3 text-sm text-amber-900 dark:border-amber-900/60 dark:bg-amber-950/30 dark:text-amber-200 sm:flex-row sm:items-center sm:justify-between">
          <div className="min-w-0 space-y-0.5">
            <p className="font-semibold">Đang chờ phụ huynh duyệt yêu cầu bỏ qua</p>
            <p className="break-words text-xs [overflow-wrap:anywhere]">
              Lý do: &ldquo;{task.skipRequestReason}&rdquo;
            </p>
          </div>
          <Button
            type="button"
            size="sm"
            variant="outline"
            disabled={isCancellingSkip}
            onClick={() => onCancelSkip(task.id)}
            className="h-11 shrink-0 bg-background sm:h-9"
          >
            {isCancellingSkip ? 'Đang hủy...' : 'Hủy xin bỏ qua'}
          </Button>
        </div>
      ) : null}

      {task.skipRequestStatus === 'REJECTED' ? (
        <div className="rounded-xl border border-rose-200 bg-rose-50/60 px-4 py-3 text-sm text-rose-900 dark:border-rose-900/60 dark:bg-rose-950/25 dark:text-rose-200">
          <p className="font-semibold">Phụ huynh chưa đồng ý bỏ qua bài học này</p>
          {task.guardianNote ? (
            <p className="mt-0.5 break-words text-xs [overflow-wrap:anywhere]">
              Lời nhắn của phụ huynh: &ldquo;{task.guardianNote}&rdquo;
            </p>
          ) : null}
        </div>
      ) : null}
    </>
  );
}
