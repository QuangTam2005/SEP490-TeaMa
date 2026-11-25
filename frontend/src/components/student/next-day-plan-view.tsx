'use client';

import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import {
  ArrowRight,
  BookOpenCheck,
  CalendarDays,
  CheckCircle2,
  Compass,
  Lock,
  Sparkles,
} from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { QueryStateBoundary } from '@/components/shared/query-state-boundary';
import { StatusBadge } from '@/components/shared/status-badge';
import { formatViDate } from '@/lib/study-utils';
import { cn } from '@/lib/utils';
import { useMockRuntime } from '@/mocks/mock-runtime';
import { studyService } from '@/services';
import type { StudyTaskSummaryResponse } from '@/types';

function TaskActionButton({
  task,
  stepLabel,
  stepNumber,
}: {
  task: StudyTaskSummaryResponse;
  stepLabel: string;
  stepNumber: 1 | 2;
}) {
  const isBlocked = task.status === 'BLOCKED';
  const isDone = task.status === 'EVALUATED';
  const isEvaluating = task.status === 'EVALUATING';

  return (
    <div
      className={cn(
        'flex flex-col justify-between gap-3 rounded-xl border p-4 transition-colors',
        isBlocked
          ? 'border-dashed bg-muted/40'
          : isDone
            ? 'border-emerald-200 bg-emerald-50/40 dark:border-emerald-900/70 dark:bg-emerald-950/20'
            : 'border-brand/25 bg-card'
      )}
    >
      <div className="space-y-2">
        <div className="flex items-center justify-between gap-2">
          <span className="inline-flex items-center gap-1.5 text-xs font-bold uppercase tracking-wide text-brand-strong dark:text-sky-300">
            {stepNumber === 1 ? (
              <BookOpenCheck className="h-4 w-4" strokeWidth={1.75} />
            ) : (
              <Compass className="h-4 w-4" strokeWidth={1.75} />
            )}
            Bước {stepNumber}: {stepLabel}
          </span>
          <StatusBadge status={task.status} />
        </div>

        <p className="text-sm font-medium text-foreground">
          {task.currentLessonTitle ??
            (isBlocked
              ? 'Mở khóa sau khi con nộp Bước 1 (Ôn bài cũ)'
              : stepNumber === 1
                ? 'Tóm tắt lại bài đã học trên lớp'
                : 'Đọc trước và ghi ý chính bài học mới')}
        </p>

        {task.revisionCount > 0 ? (
          <p className="text-xs text-muted-foreground">
            Phiên bản: {Math.min(task.revisionCount + 1, 3)}/3 (đã sửa{' '}
            {task.revisionCount}/{task.maxRevisions} lần)
          </p>
        ) : null}
      </div>

      {isBlocked ? (
        <Button
          type="button"
          variant="outline"
          disabled
          className="h-11 w-full justify-center gap-2"
        >
          <Lock className="h-4 w-4" strokeWidth={1.75} />
          Chờ hoàn thành Ôn bài cũ
        </Button>
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
    </div>
  );
}

export function NextDayPlanView() {
  const runtime = useMockRuntime();

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
          const completedCount = allTasks.filter(
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

                <div className="flex items-center gap-2 rounded-lg border border-brand/25 bg-brand-soft/60 px-4 py-2.5 text-sm font-semibold text-brand-strong dark:text-sky-200">
                  <CheckCircle2 className="h-4 w-4" strokeWidth={1.75} />
                  <span>
                    Hoàn thành: {completedCount}/{allTasks.length} bài
                  </span>
                </div>
              </div>

              <div className="grid grid-cols-1 gap-5">
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
                      />
                      <TaskActionButton
                        task={pair.previewTask}
                        stepLabel="Soạn bài mới"
                        stepNumber={2}
                      />
                    </CardContent>
                  </Card>
                ))}
              </div>
            </>
          );
        }}
      </QueryStateBoundary>
    </div>
  );
}
