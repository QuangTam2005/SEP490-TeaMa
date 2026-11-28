'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  ArrowRight,
  BookOpenCheck,
  CalendarDays,
  CheckCircle2,
  Compass,
  ExternalLink,
  Flame,
  FolderUp,
  History,
  Loader2,
  Lock,
  SkipForward,
  Sparkles,
} from 'lucide-react';
import { toast } from 'sonner';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { QueryStateBoundary } from '@/components/shared/query-state-boundary';
import { StatusBadge } from '@/components/shared/status-badge';
import { extractApiErrorMessage } from '@/components/shared/form-error-utils';
import { SkipRequestDialog } from '@/components/student/skip-request-dialog';
import { formatViDate } from '@/lib/study-utils';
import { cn } from '@/lib/utils';
import { useMockRuntime } from '@/mocks/mock-runtime';
import { quizService, studyService } from '@/services';
import type { StudyTaskSummaryResponse } from '@/types';

function TaskActionButton({
  task,
  stepLabel,
  stepNumber,
  onOpenSkipDialog,
  onCancelSkip,
  isCancellingSkip,
  onConfirmCustomTask,
  isConfirmingCustom,
}: {
  task: StudyTaskSummaryResponse;
  stepLabel: string;
  stepNumber: 1 | 2;
  onOpenSkipDialog: (task: StudyTaskSummaryResponse) => void;
  onCancelSkip: (taskId: number) => void;
  isCancellingSkip: boolean;
  onConfirmCustomTask: (taskId: number) => void;
  isConfirmingCustom: boolean;
}) {
  const isBlocked = task.status === 'BLOCKED';
  const isSkipped = task.status === 'SKIPPED';
  const isDone = task.status === 'EVALUATED';
  const isEvaluating = task.status === 'EVALUATING';
  const isCustomDrive = task.assignmentMode === 'CUSTOM_DRIVE';
  const isPendingSkip = task.skipRequestStatus === 'PENDING' && !isSkipped;
  const isRejectedSkip = task.skipRequestStatus === 'REJECTED' && !isSkipped;
  const canRequestSkip =
    !task.customSubmittedAt &&
    (task.status === 'NOT_STARTED' ||
      task.status === 'IN_PROGRESS' ||
      task.status === 'BLOCKED');

  return (
    <div
      className={cn(
        'flex flex-col justify-between gap-3 rounded-xl border p-4 transition-colors',
        isBlocked || isSkipped
          ? 'border-dashed bg-muted/40'
          : isDone
            ? 'border-emerald-200 bg-emerald-50/40 dark:border-emerald-900/70 dark:bg-emerald-950/20'
            : 'border-brand/25 bg-card'
      )}
    >
      <div className="space-y-2">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <span className="inline-flex items-center gap-1.5 text-xs font-bold uppercase tracking-wide text-brand-strong dark:text-sky-300">
            {stepNumber === 1 ? (
              <BookOpenCheck className="h-4 w-4" strokeWidth={1.75} />
            ) : (
              <Compass className="h-4 w-4" strokeWidth={1.75} />
            )}
            Bước {stepNumber}: {stepLabel}
          </span>
          <div className="flex flex-wrap items-center gap-1.5">
            <StatusBadge status={task.status} />
            {isCustomDrive && !isSkipped ? (
              <Badge variant="brand" className="whitespace-nowrap">
                Phụ huynh giao
              </Badge>
            ) : null}
            {isPendingSkip ? (
              <Badge variant="warning" className="whitespace-nowrap">
                Chờ PH duyệt bỏ qua
              </Badge>
            ) : null}
          </div>
        </div>

        <p className="break-words text-sm font-medium text-foreground [overflow-wrap:anywhere]">
          {isSkipped
            ? 'Phụ huynh đã đánh dấu bỏ qua bài này hôm nay'
            : isCustomDrive
              ? task.customTaskTitle ?? 'Nhiệm vụ phụ huynh giao thay thế'
              : task.currentLessonTitle ??
                (isBlocked
                  ? 'Mở khóa sau khi con nộp Bước 1 (Ôn bài cũ)'
                  : stepNumber === 1
                    ? 'Tóm tắt lại bài đã học trên lớp'
                    : 'Đọc trước và ghi ý chính bài học mới')}
        </p>

        {isCustomDrive &&
        !isSkipped &&
        (task.customTaskDescription ||
          task.customDriveFolderUrl ||
          task.customSubmissionNote) ? (
          <div className="space-y-1.5 rounded-lg border border-brand/25 bg-brand-soft/30 px-2.5 py-2 text-xs">
            {task.customTaskDescription ? (
              <p className="break-words text-muted-foreground [overflow-wrap:anywhere]">
                {task.customTaskDescription}
              </p>
            ) : null}
            {task.customDriveFolderUrl ? (
              <a
                href={task.customDriveFolderUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-1 font-semibold text-brand-strong hover:underline dark:text-sky-300"
              >
                <FolderUp className="h-3.5 w-3.5" />
                Mở thư mục Google Drive
                <ExternalLink className="h-3 w-3" />
              </a>
            ) : null}
            {task.customSubmissionNote ? (
              <p className="break-words text-sky-900 dark:text-sky-200 [overflow-wrap:anywhere]">
                Đã gửi: &ldquo;{task.customSubmissionNote}&rdquo;
              </p>
            ) : null}
          </div>
        ) : null}

        {isSkipped && task.skipRequestReason ? (
          <p className="break-words rounded-lg border bg-muted/60 px-2.5 py-1.5 text-xs text-muted-foreground [overflow-wrap:anywhere]">
            Lý do đã xin: &ldquo;{task.skipRequestReason}&rdquo;
          </p>
        ) : null}

        {isSkipped && task.guardianNote ? (
          <p className="break-words rounded-lg border bg-muted/60 px-2.5 py-1.5 text-xs text-muted-foreground [overflow-wrap:anywhere]">
            Ghi chú phụ huynh: &ldquo;{task.guardianNote}&rdquo;
          </p>
        ) : null}

        {isPendingSkip && task.skipRequestReason ? (
          <div className="space-y-1.5 rounded-lg border border-amber-200 bg-amber-50/70 px-2.5 py-2 text-xs text-amber-900 dark:border-amber-900/60 dark:bg-amber-950/30 dark:text-amber-200">
            <p className="font-semibold">Đang xin phụ huynh bỏ qua</p>
            <p className="break-words [overflow-wrap:anywhere]">
              Lý do: &ldquo;{task.skipRequestReason}&rdquo;
            </p>
            <Button
              type="button"
              size="sm"
              variant="outline"
              disabled={isCancellingSkip}
              onClick={() => onCancelSkip(task.id)}
              className="h-11 bg-background text-xs sm:h-8"
            >
              {isCancellingSkip ? 'Đang hủy...' : 'Hủy xin bỏ qua'}
            </Button>
          </div>
        ) : null}

        {isRejectedSkip ? (
          <div className="space-y-1 rounded-lg border border-rose-200 bg-rose-50/60 px-2.5 py-2 text-xs text-rose-800 dark:border-rose-900/60 dark:bg-rose-950/30 dark:text-rose-200">
            <p className="font-semibold">Phụ huynh chưa đồng ý bỏ qua</p>
            {task.guardianNote ? (
              <p className="break-words [overflow-wrap:anywhere]">
                Lời nhắn: &ldquo;{task.guardianNote}&rdquo;
              </p>
            ) : null}
          </div>
        ) : null}

        {task.revisionCount > 0 ? (
          <p className="text-xs text-muted-foreground">
            Phiên bản: {Math.min(task.revisionCount + 1, 3)}/3 (đã sửa{' '}
            {task.revisionCount}/{task.maxRevisions} lần)
          </p>
        ) : null}

        {!isSkipped && !isRejectedSkip && task.status === 'IN_PROGRESS' && task.guardianNote ? (
          <div className="rounded-lg border border-amber-200 bg-amber-50/70 px-2.5 py-1.5 text-xs text-amber-900 dark:border-amber-900/60 dark:bg-amber-950/30 dark:text-amber-200">
            <p className="font-semibold">Phụ huynh nhắc làm lại</p>
            <p className="mt-0.5 break-words [overflow-wrap:anywhere]">
              &ldquo;{task.guardianNote}&rdquo;
            </p>
          </div>
        ) : null}

        {!isSkipped && !isRejectedSkip && task.guardianAcknowledgedAt ? (
          <div className="rounded-lg border border-emerald-200 bg-emerald-50/70 px-2.5 py-1.5 text-xs text-emerald-800 dark:border-emerald-900/60 dark:bg-emerald-950/30 dark:text-emerald-200">
            <p className="font-semibold">Phụ huynh đã xem bài</p>
            {task.guardianNote ? (
              <p className="mt-0.5 text-emerald-700 dark:text-emerald-300">
                &ldquo;{task.guardianNote}&rdquo;
              </p>
            ) : null}
          </div>
        ) : null}
      </div>

      <div className="space-y-2">
        {isSkipped ? (
          <Button
            type="button"
            variant="outline"
            disabled
            className="h-11 w-full justify-center gap-2"
          >
            <SkipForward className="h-4 w-4" strokeWidth={1.75} />
            Không cần làm hôm nay
          </Button>
        ) : isBlocked ? (
          <Button
            type="button"
            variant="outline"
            disabled
            className="h-11 w-full justify-center gap-2"
          >
            <Lock className="h-4 w-4" strokeWidth={1.75} />
            Chờ hoàn thành Ôn bài cũ
          </Button>
        ) : isCustomDrive ? (
          task.status === 'SUBMITTED' || isDone ? (
            <Button
              type="button"
              variant="outline"
              disabled
              className="h-11 w-full justify-center gap-2"
            >
              <CheckCircle2 className="h-4 w-4" strokeWidth={1.75} />
              {isDone ? 'Đã hoàn thành' : 'Đã xác nhận · Chờ PH duyệt'}
            </Button>
          ) : (
            <Button
              type="button"
              disabled={isConfirmingCustom}
              onClick={() => onConfirmCustomTask(task.id)}
              className="h-11 w-full justify-center gap-2 text-sm font-semibold"
            >
              {isConfirmingCustom ? (
                <Loader2 className="h-4 w-4 animate-spin" strokeWidth={1.75} />
              ) : (
                <CheckCircle2 className="h-4 w-4" strokeWidth={1.75} />
              )}
              <span>
                {task.status === 'IN_PROGRESS'
                  ? 'Xác nhận đã làm lại'
                  : task.customDriveFolderUrl
                    ? 'Xác nhận đã nộp bài'
                    : 'Xác nhận đã hoàn thành'}
              </span>
            </Button>
          )
        ) : (
          <Button
            asChild
            variant={isDone ? 'outline' : 'default'}
            className="h-11 w-full justify-between px-4 text-sm font-semibold"
          >
            <Link href={`/student/tasks/${task.id}`}>
              <span>
                {isEvaluating
                  ? 'Xem trạng thái AI'
                  : isDone
                    ? 'Xem nhận xét / Sửa bài'
                    : task.status === 'EVALUATION_FAILED'
                      ? 'Mở bài & Thử đánh giá lại'
                      : 'Bắt đầu làm bài'}
              </span>
              <ArrowRight className="h-4 w-4" strokeWidth={1.75} />
            </Link>
          </Button>
        )}

        {canRequestSkip && !isPendingSkip ? (
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => onOpenSkipDialog(task)}
            className="h-11 w-full text-xs text-muted-foreground hover:text-foreground sm:h-9"
          >
            {isRejectedSkip ? 'Gửi lại lý do xin bỏ qua' : 'Xin PH bỏ qua phần này...'}
          </Button>
        ) : null}
      </div>
    </div>
  );
}

export function NextDayPlanView() {
  const runtime = useMockRuntime();
  const queryClient = useQueryClient();
  const [skipTarget, setSkipTarget] = useState<StudyTaskSummaryResponse | null>(
    null
  );

  const planQuery = useQuery({
    queryKey: ['next-day-plan', runtime.resetCounter, runtime.scenario, runtime.simulatedRole],
    queryFn: () => studyService.getNextDayPlan(),
    refetchInterval: (query) => {
      const hasEvaluating = query.state.data?.subjects.some(
        (pair) =>
          pair.reviewTask.status === 'EVALUATING' ||
          pair.previewTask.status === 'EVALUATING'
      );
      return hasEvaluating ? 1500 : false;
    },
  });

  const streakQuery = useQuery({
    queryKey: ['student-streak', runtime.resetCounter, runtime.scenario, runtime.simulatedRole],
    queryFn: () => studyService.getStreak(),
  });

  const dailyQuizQuery = useQuery({
    queryKey: ['student-daily-quiz'],
    queryFn: () => quizService.getTodayQuiz(),
  });

  const invalidateSkipRelatedQueries = () => {
    void queryClient.invalidateQueries({ queryKey: ['next-day-plan'] });
    void queryClient.invalidateQueries({ queryKey: ['student-task'] });
    void queryClient.invalidateQueries({ queryKey: ['student-history'] });
    void queryClient.invalidateQueries({ queryKey: ['guardian-dashboard'] });
    void queryClient.invalidateQueries({ queryKey: ['guardian-weekly'] });
    void queryClient.invalidateQueries({ queryKey: ['student-streak'] });
  };

  const confirmCustomMutation = useMutation({
    mutationFn: (taskId: number) => studyService.submitCustomTask(taskId, {}),
    onSuccess: (updated) => {
      invalidateSkipRelatedQueries();
      toast.success(
        updated.customDriveFolderUrl
          ? 'Đã xác nhận nộp bài lên Drive cho phụ huynh!'
          : 'Đã xác nhận hoàn thành nhiệm vụ cho phụ huynh!'
      );
    },
    onError: (err) => {
      toast.error(extractApiErrorMessage(err));
    },
  });

  const requestSkipMutation = useMutation({
    mutationFn: ({ taskId, reason }: { taskId: number; reason: string }) =>
      studyService.requestSkip(taskId, { reason }),
    onSuccess: () => {
      invalidateSkipRelatedQueries();
      setSkipTarget(null);
      toast.success('Đã gửi yêu cầu xin bỏ qua tới phụ huynh');
    },
    onError: (err) => {
      toast.error(extractApiErrorMessage(err));
    },
  });

  const cancelSkipMutation = useMutation({
    mutationFn: (taskId: number) => studyService.cancelSkipRequest(taskId),
    onSuccess: () => {
      invalidateSkipRelatedQueries();
      toast.success('Đã hủy yêu cầu xin bỏ qua');
    },
    onError: (err) => {
      toast.error(extractApiErrorMessage(err));
    },
  });

  return (
    <div className="space-y-6">
      <QueryStateBoundary
        data={planQuery.data}
        isLoading={planQuery.isLoading}
        isError={planQuery.isError}
        error={planQuery.error}
        onRetry={() => void planQuery.refetch()}
        isEmpty={(data) => !data.hasSchedule || data.subjects.length === 0}
        emptyState={{
          title: 'Không có môn học theo thời khóa biểu',
          description:
            'Ngày mai chưa có môn học nào trong lịch tuần. Con có thể cập nhật thời khóa biểu bất cứ lúc nào.',
          icon: CalendarDays,
          action: (
            <Button asChild className="h-11 gap-2 px-5">
              <Link href="/student/timetable">
                <CalendarDays className="h-4 w-4" strokeWidth={1.75} />
                Chỉnh Thời khóa biểu
              </Link>
            </Button>
          ),
        }}
      >
        {(plan) => {
          const allTasks = plan.subjects.flatMap((s) => [
            s.reviewTask,
            s.previewTask,
          ]);
          const activeTasks = allTasks.filter((t) => t.status !== 'SKIPPED');
          const completedCount = activeTasks.filter(
            (t) => t.status === 'EVALUATED'
          ).length;

          return (
            <>
              <div className="flex flex-col gap-4 rounded-xl border bg-card p-5 sm:flex-row sm:items-center sm:justify-between">
                <div className="space-y-1">
                  <div className="flex flex-wrap items-center gap-2.5">
                    <Sparkles
                      className="h-5 w-5 text-brand-strong dark:text-sky-400"
                      strokeWidth={1.75}
                    />
                    <h1 className="text-xl font-bold tracking-tight text-foreground sm:text-2xl">
                      Bài học ngày mai · {plan.weekdayLabel}
                    </h1>
                    <Badge variant="brand">
                      {formatViDate(plan.targetDate)}
                    </Badge>
                  </div>
                  <p className="text-sm text-muted-foreground">
                    Làm xong <strong className="text-foreground">Ôn bài cũ</strong> của từng môn để mở khóa{' '}
                    <strong className="text-foreground">Soạn bài mới</strong>.
                  </p>
                </div>

                <div className="flex flex-wrap items-center gap-2.5">
                  {dailyQuizQuery.data?.session?.status === 'COMPLETED' ? (
                    <div className="flex items-center gap-1.5 rounded-lg border border-emerald-200 bg-emerald-50/70 px-3.5 py-2 text-sm font-semibold text-emerald-800 dark:border-emerald-900/70 dark:bg-emerald-950/30 dark:text-emerald-200">
                      <Sparkles className="h-4 w-4 text-emerald-600 dark:text-emerald-400" strokeWidth={1.75} />
                      <span>
                        Quiz: {dailyQuizQuery.data.session.correctCount}/{dailyQuizQuery.data.session.totalQuestions} đúng
                      </span>
                    </div>
                  ) : null}

                  <div
                    className="flex items-center gap-1.5 rounded-lg border border-amber-200 bg-amber-50/70 px-3.5 py-2 text-sm font-semibold text-amber-800 dark:border-amber-900/70 dark:bg-amber-950/30 dark:text-amber-200"
                    title={`Kỷ lục dài nhất: ${streakQuery.data?.longestStreak ?? 0} ngày`}
                  >
                    <Flame className="h-4 w-4 text-amber-500" strokeWidth={2} />
                    <span>Chuỗi {streakQuery.data?.currentStreak ?? 0} ngày</span>
                  </div>

                  <div className="flex items-center gap-2 rounded-lg border border-brand/25 bg-brand-soft/60 px-4 py-2 text-sm font-semibold text-brand-strong dark:text-sky-200">
                    <CheckCircle2 className="h-4 w-4" strokeWidth={1.75} />
                    <span>
                      Hoàn thành: {completedCount}/{activeTasks.length} bài
                    </span>
                  </div>

                  <Button asChild variant="outline" className="h-11 gap-1.5 sm:h-10">
                    <Link href="/student/history">
                      <History className="h-4 w-4" strokeWidth={1.75} />
                      <span>Lịch sử</span>
                    </Link>
                  </Button>
                </div>
              </div>

              <div
                className={cn(
                  'grid grid-cols-1 gap-5',
                  plan.subjects.length > 1 && '2xl:grid-cols-2'
                )}
              >
                {plan.subjects.map((pair) => (
                  <Card key={pair.subjectCode} className="overflow-hidden">
                    <CardHeader className="border-b bg-brand-soft/35 pb-3.5 dark:bg-sky-950/30">
                      <div className="flex flex-wrap items-center justify-between gap-2">
                        <CardTitle className="text-lg font-bold text-foreground">
                          Môn {pair.subjectName}
                        </CardTitle>
                      </div>
                    </CardHeader>
                    <CardContent className="grid grid-cols-1 gap-4 p-4 md:grid-cols-2">
                      <TaskActionButton
                        task={pair.reviewTask}
                        stepLabel="Ôn bài cũ"
                        stepNumber={1}
                        onOpenSkipDialog={setSkipTarget}
                        onCancelSkip={(id) => cancelSkipMutation.mutate(id)}
                        isCancellingSkip={
                          cancelSkipMutation.isPending &&
                          cancelSkipMutation.variables === pair.reviewTask.id
                        }
                        onConfirmCustomTask={(id) => confirmCustomMutation.mutate(id)}
                        isConfirmingCustom={
                          confirmCustomMutation.isPending &&
                          confirmCustomMutation.variables === pair.reviewTask.id
                        }
                      />
                      <TaskActionButton
                        task={pair.previewTask}
                        stepLabel="Soạn bài mới"
                        stepNumber={2}
                        onOpenSkipDialog={setSkipTarget}
                        onCancelSkip={(id) => cancelSkipMutation.mutate(id)}
                        isCancellingSkip={
                          cancelSkipMutation.isPending &&
                          cancelSkipMutation.variables === pair.previewTask.id
                        }
                        onConfirmCustomTask={(id) => confirmCustomMutation.mutate(id)}
                        isConfirmingCustom={
                          confirmCustomMutation.isPending &&
                          confirmCustomMutation.variables === pair.previewTask.id
                        }
                      />
                    </CardContent>
                  </Card>
                ))}
              </div>
            </>
          );
        }}
      </QueryStateBoundary>

      <SkipRequestDialog
        task={skipTarget}
        open={Boolean(skipTarget)}
        onOpenChange={(open) => {
          if (!open) {
            setSkipTarget(null);
          }
        }}
        isSubmitting={requestSkipMutation.isPending}
        onSubmit={(taskId, reason) =>
          requestSkipMutation.mutate({ taskId, reason })
        }
      />
    </div>
  );
}
