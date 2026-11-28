'use client';

import { CheckCircle2, ExternalLink, FolderUp, Loader2 } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { StatusBadge } from '@/components/shared/status-badge';
import { TaskSkipBanners } from '@/components/student/task-skip-banners';
import { formatViDate } from '@/lib/study-utils';
import type { StudyTaskSummaryResponse } from '@/types';

interface CustomTaskWorkspaceProps {
  task: StudyTaskSummaryResponse;
  targetDate: string;
  canRequestSkipHere: boolean;
  isCancellingSkip: boolean;
  isSubmitting: boolean;
  onCancelSkip: (taskId: number) => void;
  onOpenSkipDialog: (task: StudyTaskSummaryResponse) => void;
  onConfirmComplete: () => void;
}

export function CustomTaskWorkspace({
  task,
  targetDate,
  canRequestSkipHere,
  isCancellingSkip,
  isSubmitting,
  onCancelSkip,
  onOpenSkipDialog,
  onConfirmComplete,
}: CustomTaskWorkspaceProps) {
  const hasDriveFolder = Boolean(task.customDriveFolderUrl);
  const isSubmittedOrDone =
    task.status === 'SUBMITTED' || task.status === 'EVALUATED';

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 rounded-xl border bg-card p-5 sm:flex-row sm:items-center sm:justify-between">
        <div className="space-y-1">
          <div className="flex flex-wrap items-center gap-2">
            <Badge variant="brand">Môn {task.subjectName}</Badge>
            <Badge variant="outline">
              {task.taskType === 'REVIEW' ? 'Ôn bài cũ' : 'Soạn bài mới'}
            </Badge>
            <Badge variant="brand">Phụ huynh giao thay thế</Badge>
            <StatusBadge status={task.status} />
            {task.skipRequestStatus === 'PENDING' ? (
              <Badge variant="warning" className="whitespace-nowrap">
                Chờ PH duyệt bỏ qua
              </Badge>
            ) : null}
          </div>
          <h1 className="break-words text-xl font-bold tracking-tight text-foreground [overflow-wrap:anywhere] sm:text-2xl">
            {task.customTaskTitle ?? `Bài tập ${task.subjectName}`}
          </h1>
        </div>
        <div className="flex flex-wrap items-center gap-2.5 text-xs font-medium text-muted-foreground">
          <span>Ngày học trên lớp: {formatViDate(targetDate)}</span>
          {canRequestSkipHere && task.skipRequestStatus !== 'PENDING' ? (
            <Button
              type="button"
              size="sm"
              variant="outline"
              className="h-11 sm:h-9"
              onClick={() => onOpenSkipDialog(task)}
            >
              {task.skipRequestStatus === 'REJECTED'
                ? 'Gửi lại xin bỏ qua'
                : 'Xin phụ huynh bỏ qua'}
            </Button>
          ) : null}
        </div>
      </div>

      <TaskSkipBanners
        task={task}
        isCancellingSkip={isCancellingSkip}
        onCancelSkip={onCancelSkip}
      />

      {task.status === 'IN_PROGRESS' && task.guardianNote ? (
        <div className="rounded-xl border border-amber-200 bg-amber-50/70 px-4 py-3 text-sm text-amber-900 dark:border-amber-900/60 dark:bg-amber-950/30 dark:text-amber-200">
          <p className="font-semibold">
            {hasDriveFolder
              ? 'Phụ huynh nhắc con bổ sung lại bài trên Drive'
              : 'Phụ huynh nhắc con thực hiện hoặc bổ sung lại'}
          </p>
          <p className="mt-0.5 break-words text-xs [overflow-wrap:anywhere]">
            Lời nhắn: &ldquo;{task.guardianNote}&rdquo;
          </p>
        </div>
      ) : null}

      {task.guardianAcknowledgedAt ? (
        <div className="rounded-xl border border-emerald-200 bg-emerald-50/60 px-4 py-3 text-sm text-emerald-900 dark:border-emerald-900/60 dark:bg-emerald-950/25 dark:text-emerald-200">
          <p className="font-semibold">
            Phụ huynh đã xác nhận hoàn thành (
            {new Date(task.guardianAcknowledgedAt).toLocaleString('vi-VN')})
          </p>
          {task.guardianNote ? (
            <p className="mt-0.5 break-words text-xs [overflow-wrap:anywhere]">
              Lời nhắn từ bố mẹ: &ldquo;{task.guardianNote}&rdquo;
            </p>
          ) : null}
        </div>
      ) : null}

      <div className="max-w-3xl space-y-4">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-lg">
              <FolderUp className="h-5 w-5 text-brand-strong dark:text-sky-400" />
              {hasDriveFolder
                ? 'Nhiệm vụ từ Phụ huynh & Nộp bài Google Drive'
                : 'Nhiệm vụ từ Phụ huynh'}
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2 rounded-xl border border-brand/25 bg-brand-soft/30 p-4">
              <p className="break-words text-sm font-semibold text-foreground [overflow-wrap:anywhere]">
                {task.customTaskTitle}
              </p>
              {task.customTaskDescription ? (
                <p className="whitespace-pre-wrap break-words text-sm text-muted-foreground [overflow-wrap:anywhere]">
                  {task.customTaskDescription}
                </p>
              ) : null}
              {task.customDriveFolderUrl ? (
                <div className="pt-1">
                  <Button asChild className="h-11 w-full gap-2 sm:w-auto">
                    <a
                      href={task.customDriveFolderUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      <FolderUp className="h-4 w-4" />
                      Mở thư mục Google Drive
                      <ExternalLink className="h-4 w-4" />
                    </a>
                  </Button>
                </div>
              ) : null}
            </div>

            {isSubmittedOrDone ? (
              <div className="rounded-xl border bg-muted/40 p-4 text-sm">
                <p className="font-semibold text-foreground">
                  {task.status === 'EVALUATED'
                    ? 'Nhiệm vụ đã được phụ huynh xác nhận đạt'
                    : hasDriveFolder
                      ? 'Đã xác nhận nộp bài lên Drive (chờ phụ huynh kiểm tra)'
                      : 'Đã xác nhận hoàn thành (chờ phụ huynh kiểm tra)'}
                </p>
              </div>
            ) : (
              <Button
                type="button"
                disabled={isSubmitting}
                onClick={onConfirmComplete}
                className="h-11 w-full gap-2 sm:w-auto"
              >
                {isSubmitting ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <CheckCircle2 className="h-4 w-4" />
                )}
                {task.status === 'IN_PROGRESS'
                  ? 'Xác nhận đã làm lại'
                  : hasDriveFolder
                    ? 'Xác nhận đã nộp bài'
                    : 'Xác nhận đã hoàn thành'}
              </Button>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
