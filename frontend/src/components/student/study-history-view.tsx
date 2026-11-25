'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import {
  ArrowRight,
  Award,
  BookOpenCheck,
  CalendarDays,
  CheckCircle2,
  Compass,
  Flame,
  History,
  RotateCcw,
} from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { QueryStateBoundary } from '@/components/shared/query-state-boundary';
import { StatusBadge } from '@/components/shared/status-badge';
import { formatViDate } from '@/lib/study-utils';
import { useMockRuntime } from '@/mocks/mock-runtime';
import { studyService } from '@/services';

export function StudyHistoryView() {
  const runtime = useMockRuntime();
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');

  const streakQuery = useQuery({
    queryKey: [
      'student-streak',
      runtime.resetCounter,
      runtime.scenario,
      runtime.simulatedRole,
    ],
    queryFn: () => studyService.getStreak(),
  });

  const historyQuery = useQuery({
    queryKey: [
      'student-history',
      fromDate,
      toDate,
      runtime.resetCounter,
      runtime.scenario,
      runtime.simulatedRole,
    ],
    queryFn: () =>
      studyService.getPlanHistory(
        fromDate || undefined,
        toDate || undefined
      ),
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 rounded-xl border bg-card p-5 sm:flex-row sm:items-center sm:justify-between">
        <div className="space-y-1">
          <div className="flex items-center gap-2.5">
            <History
              className="h-5 w-5 text-brand-strong dark:text-sky-400"
              strokeWidth={1.75}
            />
            <h1 className="text-xl font-bold tracking-tight text-foreground sm:text-2xl">
              Lịch sử học tập & Chuỗi ngày học
            </h1>
          </div>
          <p className="text-sm text-muted-foreground">
            Xem lại các bài Ôn bài cũ và Soạn bài mới bạn đã chuẩn bị theo từng ngày.
          </p>
        </div>

        <Button asChild variant="outline" className="h-10 gap-2">
          <Link href="/student/today">Về Bài học ngày mai</Link>
        </Button>
      </div>

      {/* Streak KPI Cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <Card className="border-amber-200/80 bg-amber-50/40 dark:border-amber-900/60 dark:bg-amber-950/20">
          <CardContent className="flex items-center justify-between p-5">
            <div>
              <p className="text-xs font-medium text-muted-foreground">
                Chuỗi học liên tiếp
              </p>
              <p className="mt-1 text-2xl font-bold text-foreground">
                {streakQuery.data?.currentStreak ?? 0} ngày
              </p>
            </div>
            <Flame className="h-7 w-7 text-amber-500" strokeWidth={1.75} />
          </CardContent>
        </Card>

        <Card>
          <CardContent className="flex items-center justify-between p-5">
            <div>
              <p className="text-xs font-medium text-muted-foreground">
                Kỷ lục dài nhất
              </p>
              <p className="mt-1 text-2xl font-bold text-foreground">
                {streakQuery.data?.longestStreak ?? 0} ngày
              </p>
            </div>
            <Award className="h-7 w-7 text-brand-strong" strokeWidth={1.75} />
          </CardContent>
        </Card>

        <Card>
          <CardContent className="flex items-center justify-between p-5">
            <div>
              <p className="text-xs font-medium text-muted-foreground">
                Tổng ngày hoàn thành
              </p>
              <p className="mt-1 text-2xl font-bold text-foreground">
                {streakQuery.data?.totalCompletedDays ?? 0} ngày
              </p>
            </div>
            <CheckCircle2
              className="h-7 w-7 text-emerald-600"
              strokeWidth={1.75}
            />
          </CardContent>
        </Card>
      </div>

      {/* Optional Date Range Filter */}
      <Card>
        <CardContent className="flex flex-col gap-3 p-4 sm:flex-row sm:items-end">
          <div className="flex-1 space-y-1">
            <label
              htmlFor="history-from"
              className="text-xs font-medium text-muted-foreground"
            >
              Từ ngày
            </label>
            <Input
              id="history-from"
              type="date"
              value={fromDate}
              onChange={(e) => setFromDate(e.target.value)}
              className="h-10"
            />
          </div>
          <div className="flex-1 space-y-1">
            <label
              htmlFor="history-to"
              className="text-xs font-medium text-muted-foreground"
            >
              Đến ngày
            </label>
            <Input
              id="history-to"
              type="date"
              value={toDate}
              onChange={(e) => setToDate(e.target.value)}
              className="h-10"
            />
          </div>
          {(fromDate || toDate) ? (
            <Button
              type="button"
              variant="outline"
              className="h-10 gap-1.5"
              onClick={() => {
                setFromDate('');
                setToDate('');
              }}
            >
              <RotateCcw className="h-4 w-4" />
              Đặt lại
            </Button>
          ) : null}
        </CardContent>
      </Card>

      <QueryStateBoundary
        data={historyQuery.data}
        isLoading={historyQuery.isLoading}
        isError={historyQuery.isError}
        error={historyQuery.error}
        onRetry={() => void historyQuery.refetch()}
        isEmpty={(items) => items.length === 0}
        emptyState={{
          title: 'Chưa có lịch sử bài học',
          description:
            'Khi con tạo kế hoạch ngày mai và làm bài, lịch sử học tập từng ngày sẽ hiện ở đây nhé.',
          icon: CalendarDays,
          action: (
            <Button asChild className="h-11">
              <Link href="/student/today">Làm bài hôm nay</Link>
            </Button>
          ),
        }}
      >
        {(plans) => (
          <div className="space-y-4">
            {plans.map((plan) => (
              <Card key={plan.planId}>
                <CardHeader className="flex flex-col gap-2 pb-3 sm:flex-row sm:items-center sm:justify-between">
                  <div className="flex flex-wrap items-center gap-2">
                    <CardTitle className="text-base">
                      {plan.weekdayLabel} · {formatViDate(plan.targetDate)}
                    </CardTitle>
                    <Badge variant="brand">
                      {plan.completedTasks}/{plan.totalTasks} nhiệm vụ
                    </Badge>
                  </div>
                </CardHeader>
                <CardContent className="space-y-3">
                  {plan.subjects.map((pair) => (
                    <div
                      key={pair.subjectCode}
                      className="rounded-lg border p-3.5"
                    >
                      <p className="mb-2.5 text-sm font-bold text-foreground">
                        {pair.subjectName}
                      </p>
                      <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-2">
                        {[
                          {
                            task: pair.reviewTask,
                            label: 'Ôn bài cũ',
                            Icon: BookOpenCheck,
                          },
                          {
                            task: pair.previewTask,
                            label: 'Soạn bài mới',
                            Icon: Compass,
                          },
                        ].map(({ task, label, Icon }) => (
                          <Link
                            key={task.id}
                            href={`/student/tasks/${task.id}`}
                            className="flex items-center justify-between gap-2 rounded-lg border bg-background px-3 py-2.5 text-sm transition-colors hover:bg-muted"
                          >
                            <div className="min-w-0 space-y-0.5">
                              <div className="flex items-center gap-1.5 text-xs font-semibold text-brand-strong dark:text-sky-300">
                                <Icon className="h-3.5 w-3.5" />
                                <span>{label}</span>
                              </div>
                              <p className="truncate text-xs text-muted-foreground">
                                {task.currentLessonTitle ?? 'Chưa nộp bài'}
                              </p>
                            </div>
                            <div className="flex shrink-0 items-center gap-2">
                              <StatusBadge status={task.status} />
                              <ArrowRight className="h-4 w-4 text-muted-foreground" />
                            </div>
                          </Link>
                        ))}
                      </div>
                    </div>
                  ))}
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </QueryStateBoundary>
    </div>
  );
}
