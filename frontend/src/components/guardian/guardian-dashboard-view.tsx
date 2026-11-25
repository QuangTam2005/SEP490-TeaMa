'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  ArrowRight,
  BookOpenCheck,
  CalendarDays,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  ClipboardList,
  Compass,
  Download,
  Eye,
  Link2,
  Loader2,
  RotateCcw,
  ShieldCheck,
  SkipForward,
  Sparkles,
  Users,
} from 'lucide-react';
import { toast } from 'sonner';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import {
  DataTable,
  type DataTableColumn,
} from '@/components/shared/data-table';
import { QueryStateBoundary } from '@/components/shared/query-state-boundary';
import { StatusBadge } from '@/components/shared/status-badge';
import { extractApiErrorMessage } from '@/components/shared/form-error-utils';
import {
  formatViDate,
  getNextTargetDateIso,
  getSubjectsForGrade,
} from '@/lib/study-utils';
import { cn } from '@/lib/utils';
import { useMockRuntime } from '@/mocks/mock-runtime';
import { IS_MOCK_MODE, guardianService, linkService } from '@/services';
import { useAuthStore } from '@/store/auth-store';
import { useGuardianStore } from '@/store/guardian-store';
import type { GuardianTaskItem, StudyTaskStatus, SubjectCode } from '@/types';

function shiftIsoDate(isoDate: string, days: number): string {
  const [y, m, d] = isoDate.split('-').map(Number);
  const dt = new Date(Date.UTC(y, (m || 1) - 1, d || 1));
  dt.setUTCDate(dt.getUTCDate() + days);
  return dt.toISOString().slice(0, 10);
}

function getMondayIso(isoDate: string): string {
  const [y, m, d] = isoDate.split('-').map(Number);
  const dt = new Date(Date.UTC(y, (m || 1) - 1, d || 1));
  const day = dt.getUTCDay();
  const diff = day === 0 ? -6 : 1 - day;
  dt.setUTCDate(dt.getUTCDate() + diff);
  return dt.toISOString().slice(0, 10);
}

export function GuardianDashboardView() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const runtime = useMockRuntime();
  const user = useAuthStore((state) => state.user);
  const isHydrated = useAuthStore((state) => state.isHydrated);
  const selectedStudentId = useGuardianStore((state) => state.selectedStudentId);
  const setSelectedStudentId = useGuardianStore(
    (state) => state.setSelectedStudentId
  );

  const isGuardianReady = IS_MOCK_MODE || (isHydrated && user?.role === 'GUARDIAN');

  const [selectedDate, setSelectedDate] = useState<string>(() =>
    getNextTargetDateIso()
  );
  const [selectedSubject, setSelectedSubject] = useState<SubjectCode | 'ALL'>(
    'ALL'
  );
  const [weekStart, setWeekStart] = useState<string>(() =>
    getMondayIso(getNextTargetDateIso())
  );
  const [isExporting, setIsExporting] = useState(false);

  const childrenQuery = useQuery({
    queryKey: [
      'linked-children',
      runtime.resetCounter,
      runtime.scenario,
      runtime.simulatedRole,
    ],
    queryFn: () => linkService.getLinkedChildren(),
    enabled: isGuardianReady,
  });

  const hasLinkedChildren =
    IS_MOCK_MODE || (childrenQuery.data?.length ?? 0) > 0;
  const hasNoLinkedChild =
    isGuardianReady &&
    !IS_MOCK_MODE &&
    childrenQuery.isSuccess &&
    (childrenQuery.data?.length ?? 0) === 0;

  useEffect(() => {
    const children = childrenQuery.data;
    if (!children || children.length === 0) {
      return;
    }
    const stillValid = children.some((c) => c.studentId === selectedStudentId);
    if (!selectedStudentId || !stillValid) {
      setSelectedStudentId(children[0].studentId);
    }
  }, [childrenQuery.data, selectedStudentId, setSelectedStudentId]);

  const activeChild = childrenQuery.data?.find(
    (c) => c.studentId === selectedStudentId
  );

  const dashboardQuery = useQuery({
    queryKey: [
      'guardian-dashboard',
      selectedDate,
      selectedSubject,
      selectedStudentId,
      runtime.resetCounter,
      runtime.scenario,
      runtime.simulatedRole,
    ],
    queryFn: () =>
      guardianService.getDashboard({
        date: selectedDate,
        subjectCode: selectedSubject,
        studentId: selectedStudentId ?? undefined,
      }),
    enabled:
      isGuardianReady &&
      hasLinkedChildren &&
      (IS_MOCK_MODE || Boolean(selectedStudentId)),
  });

  const weeklyQuery = useQuery({
    queryKey: [
      'guardian-weekly',
      weekStart,
      selectedStudentId,
      runtime.resetCounter,
      runtime.scenario,
      runtime.simulatedRole,
    ],
    queryFn: () =>
      guardianService.getWeeklySummary({
        weekStart,
        studentId: selectedStudentId ?? undefined,
      }),
    enabled:
      isGuardianReady &&
      hasLinkedChildren &&
      (IS_MOCK_MODE || Boolean(selectedStudentId)),
  });

  const skipMutation = useMutation({
    mutationFn: ({ taskId, skipped }: { taskId: number; skipped: boolean }) =>
      guardianService.skipTask(taskId, { skipped }),
    onSuccess: (res) => {
      void queryClient.invalidateQueries({ queryKey: ['guardian-dashboard'] });
      void queryClient.invalidateQueries({ queryKey: ['guardian-weekly'] });
      void queryClient.invalidateQueries({ queryKey: ['next-day-plan'] });
      void queryClient.invalidateQueries({ queryKey: ['student-task'] });
      void queryClient.invalidateQueries({ queryKey: ['student-streak'] });
      toast.success(
        res.status === 'SKIPPED'
          ? 'Đã đánh dấu bỏ qua bài học cho con'
          : 'Đã khôi phục bài học cho con'
      );
    },
    onError: (err) => {
      toast.error(extractApiErrorMessage(err));
    },
  });

  const canToggleSkip = (status: StudyTaskStatus) =>
    status === 'NOT_STARTED' ||
    status === 'IN_PROGRESS' ||
    status === 'BLOCKED' ||
    status === 'SKIPPED';

  const handleExportExcel = async () => {
    setIsExporting(true);
    try {
      const blob = await guardianService.exportReport({
        studentId: selectedStudentId ?? undefined,
      });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `bao-cao-hoc-tap-${selectedDate}.xlsx`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
      toast.success('Đã tải báo cáo Excel thành công');
    } catch (err) {
      toast.error(extractApiErrorMessage(err));
    } finally {
      setIsExporting(false);
    }
  };

  const activeGrade = dashboardQuery.data?.grade ?? activeChild?.grade ?? 6;
  const subjectFilters: { code: SubjectCode | 'ALL'; label: string }[] = [
    { code: 'ALL', label: 'Tất cả môn' },
    ...getSubjectsForGrade(activeGrade).map((s) => ({
      code: s.code,
      label: s.name,
    })),
  ];

  const columns: DataTableColumn<GuardianTaskItem>[] = [
    {
      key: 'subject',
      header: 'Môn học',
      render: (row) => (
        <div className="space-y-0.5">
          <p className="font-semibold text-foreground">{row.subjectName}</p>
          <span className="inline-flex items-center gap-1 text-xs text-muted-foreground">
            {row.taskType === 'REVIEW' ? (
              <>
                <BookOpenCheck className="h-3.5 w-3.5" />
                Ôn bài cũ
              </>
            ) : (
              <>
                <Compass className="h-3.5 w-3.5" />
                Soạn bài mới
              </>
            )}
          </span>
        </div>
      ),
    },
    {
      key: 'lessonTitle',
      header: 'Bài học',
      render: (row) => (
        <span className="block max-w-xs break-words text-sm text-foreground [overflow-wrap:anywhere]">
          {row.lessonTitle ?? 'Chưa nộp bài'}
        </span>
      ),
    },
    {
      key: 'status',
      header: 'Trạng thái',
      render: (row) => <StatusBadge status={row.status} />,
    },
    {
      key: 'evaluation',
      header: 'Kết quả AI',
      render: (row) =>
        row.evaluation ? (
          <div className="flex flex-wrap gap-1.5">
            <StatusBadge status={row.evaluation.overallStatus} />
            <StatusBadge status={row.evaluation.accuracy} />
          </div>
        ) : (
          <span className="text-xs text-muted-foreground">Chưa có</span>
        ),
    },
    {
      key: 'revisions',
      header: 'Xác nhận PH',
      align: 'center',
      render: (row) =>
        row.guardianAcknowledgedAt ? (
          <Badge variant="success" className="whitespace-nowrap">
            Đã xem
          </Badge>
        ) : (
          <span className="text-xs text-muted-foreground">Chưa xem</span>
        ),
    },
    {
      key: 'action',
      header: 'Thao tác',
      align: 'right',
      render: (row) => {
        if (row.currentSubmissionId) {
          return (
            <Button asChild size="sm" variant="outline" className="h-9 gap-1.5">
              <Link href={`/guardian/submissions/${row.currentSubmissionId}`}>
                <Eye className="h-3.5 w-3.5" strokeWidth={1.75} />
                Xem bài gốc
              </Link>
            </Button>
          );
        }
        if (canToggleSkip(row.status)) {
          const isSkipped = row.status === 'SKIPPED';
          const isBusy =
            skipMutation.isPending &&
            skipMutation.variables?.taskId === row.taskId;
          return (
            <Button
              type="button"
              size="sm"
              variant="outline"
              disabled={isBusy}
              className="h-9 gap-1.5"
              onClick={(e) => {
                e.stopPropagation();
                skipMutation.mutate({
                  taskId: row.taskId,
                  skipped: !isSkipped,
                });
              }}
            >
              {isBusy ? (
                <Loader2 className="h-3.5 w-3.5 animate-spin" />
              ) : isSkipped ? (
                <RotateCcw className="h-3.5 w-3.5" strokeWidth={1.75} />
              ) : (
                <SkipForward className="h-3.5 w-3.5" strokeWidth={1.75} />
              )}
              <span>{isSkipped ? 'Khôi phục' : 'Bỏ qua'}</span>
            </Button>
          );
        }
        return <span className="text-xs text-muted-foreground">—</span>;
      },
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 rounded-xl border bg-card p-5 sm:flex-row sm:items-center sm:justify-between">
        <div className="space-y-1">
          <div className="flex flex-wrap items-center gap-2.5">
            <ShieldCheck
              className="h-5 w-5 text-brand-strong dark:text-sky-400"
              strokeWidth={1.75}
            />
            <h1 className="text-xl font-bold tracking-tight text-foreground sm:text-2xl">
              Theo dõi học tập của con
            </h1>
          </div>
          {dashboardQuery.data?.linkedStudentName ? (
            <p className="text-sm text-muted-foreground">
              Học sinh:{' '}
              <strong className="text-foreground">
                {dashboardQuery.data.linkedStudentName}
              </strong>{' '}
              · Lớp {dashboardQuery.data.grade}
            </p>
          ) : null}
        </div>

        <div className="flex flex-wrap items-center gap-2.5">
          {childrenQuery.data && childrenQuery.data.length > 1 ? (
            <div className="flex items-center gap-1.5">
              <Users className="h-4 w-4 text-muted-foreground" strokeWidth={1.75} />
              <select
                aria-label="Chọn học sinh đang theo dõi"
                value={selectedStudentId ?? ''}
                onChange={(e) => {
                  const id = Number(e.target.value);
                  setSelectedStudentId(Number.isFinite(id) ? id : null);
                  setSelectedSubject('ALL');
                }}
                className="h-10 rounded-lg border bg-background px-3 text-sm font-semibold text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              >
                {childrenQuery.data.map((child) => (
                  <option key={child.studentId} value={child.studentId}>
                    {child.displayName} (Lớp {child.grade})
                  </option>
                ))}
              </select>
            </div>
          ) : null}

          <div className="flex items-center gap-2">
            <CalendarDays
              className="h-4 w-4 text-muted-foreground"
              strokeWidth={1.75}
            />
            <Input
              type="date"
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
              aria-label="Chọn ngày theo dõi"
              className="h-10 w-44"
            />
          </div>

          <Button
            type="button"
            variant="outline"
            className="h-10 gap-1.5"
            disabled={isExporting || !dashboardQuery.data?.linkedStudentName}
            onClick={() => void handleExportExcel()}
          >
            {isExporting ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Download className="h-4 w-4" strokeWidth={1.75} />
            )}
            <span>Xuất Excel</span>
          </Button>
        </div>
      </div>

      {/* Weekly Progress Card */}
      {weeklyQuery.data ? (
        <Card>
          <CardHeader className="flex flex-col gap-2 pb-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <CardTitle className="text-base">
                Tổng quan tuần ({formatViDate(weeklyQuery.data.weekStart)} –{' '}
                {formatViDate(weeklyQuery.data.weekEnd)})
              </CardTitle>
              <p className="text-xs text-muted-foreground">
                Hoàn thành {weeklyQuery.data.completedTasks}/
                {weeklyQuery.data.totalTasks} nhiệm vụ · Phụ huynh đã xem{' '}
                {weeklyQuery.data.acknowledgedTasks} bài
              </p>
            </div>
            <div className="flex items-center gap-1.5">
              <Button
                type="button"
                variant="outline"
                size="icon"
                className="h-9 w-9"
                aria-label="Tuần trước"
                onClick={() => setWeekStart((prev) => shiftIsoDate(prev, -7))}
              >
                <ChevronLeft className="h-4 w-4" />
              </Button>
              <Button
                type="button"
                variant="outline"
                size="icon"
                className="h-9 w-9"
                aria-label="Tuần sau"
                onClick={() => setWeekStart((prev) => shiftIsoDate(prev, 7))}
              >
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          </CardHeader>
          {weeklyQuery.data.days.length > 0 ? (
            <CardContent className="grid grid-cols-2 gap-2 sm:grid-cols-7">
              {weeklyQuery.data.days.map((day) => (
                <button
                  key={day.date}
                  type="button"
                  onClick={() => setSelectedDate(day.date)}
                  className={cn(
                    'flex flex-col items-center rounded-lg border p-2.5 text-center transition-colors',
                    selectedDate === day.date
                      ? 'border-brand-strong bg-brand-soft/60 text-brand-strong dark:text-sky-200'
                      : 'bg-background hover:bg-muted'
                  )}
                >
                  <span className="text-xs font-semibold">{day.weekdayLabel}</span>
                  <span className="text-[11px] text-muted-foreground">
                    {day.date.slice(5).split('-').reverse().join('/')}
                  </span>
                  <span className="mt-1 font-mono text-xs font-bold">
                    {day.completedTasks}/{day.totalTasks}
                  </span>
                </button>
              ))}
            </CardContent>
          ) : null}
        </Card>
      ) : null}

      {/* Subject filter pills */}
      <div className="flex flex-wrap gap-2">
        {subjectFilters.map((filter) => (
          <button
            key={filter.code}
            type="button"
            onClick={() => setSelectedSubject(filter.code)}
            className={cn(
              'h-10 rounded-lg border px-3.5 text-xs font-semibold transition-colors',
              selectedSubject === filter.code
                ? 'border-brand-strong bg-brand-soft text-brand-strong dark:text-sky-200'
                : 'bg-card text-muted-foreground hover:bg-muted hover:text-foreground'
            )}
          >
            {filter.label}
          </button>
        ))}
      </div>

      {hasNoLinkedChild ? (
        <Card className="mx-auto max-w-xl">
          <CardContent className="flex flex-col items-center gap-4 py-10 text-center">
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-brand-soft text-brand-strong">
              <Link2 className="h-6 w-6" strokeWidth={1.75} />
            </div>
            <div className="space-y-1">
              <h2 className="text-lg font-bold text-foreground">
                Chưa liên kết với tài khoản học sinh
              </h2>
              <p className="text-sm text-muted-foreground">
                Phụ huynh cần nhập mã liên kết 8 ký tự từ tài khoản học sinh để xem tiến độ.
              </p>
            </div>
            <Button asChild className="h-11 gap-2">
              <Link href="/link-student">Nhập mã liên kết 8 ký tự</Link>
            </Button>
          </CardContent>
        </Card>
      ) : (
      <QueryStateBoundary
        data={dashboardQuery.data}
        isLoading={
          !isGuardianReady ||
          (!IS_MOCK_MODE && childrenQuery.isLoading) ||
          dashboardQuery.isLoading
        }
        isError={dashboardQuery.isError}
        error={dashboardQuery.error}
        onRetry={() => void dashboardQuery.refetch()}
        isEmpty={(data) => !data.linkedStudentName || data.items.length === 0}
        emptyState={{
          title: dashboardQuery.data?.linkedStudentName
            ? 'Chưa có bài học nào trong bộ lọc này'
            : 'Chưa liên kết với tài khoản học sinh',
          description: dashboardQuery.data?.linkedStudentName
            ? `Ngày ${formatViDate(selectedDate)} hiện chưa ghi nhận bài tập phù hợp.`
            : 'Phụ huynh cần nhập mã liên kết 8 ký tự từ tài khoản học sinh để xem tiến độ.',
          action: !dashboardQuery.data?.linkedStudentName ? (
            <Button asChild className="h-11 gap-2">
              <Link href="/link-student">Nhập mã liên kết 8 ký tự</Link>
            </Button>
          ) : undefined,
        }}
      >
        {(data) => (
          <div className="space-y-6">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <Card>
                <CardHeader className="pb-2">
                  <CardTitle className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                    <ClipboardList className="h-4 w-4 text-brand-strong" />
                    Tổng nhiệm vụ
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <p className="text-2xl font-bold text-foreground">
                    {data.totalTasks}
                  </p>
                </CardContent>
              </Card>

              <Card>
                <CardHeader className="pb-2">
                  <CardTitle className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                    <CheckCircle2 className="h-4 w-4 text-emerald-600" />
                    Đã có nhận xét AI
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <p className="text-2xl font-bold text-emerald-600 dark:text-emerald-400">
                    {data.completedTasks}
                  </p>
                </CardContent>
              </Card>

              <Card>
                <CardHeader className="pb-2">
                  <CardTitle className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                    <Sparkles className="h-4 w-4 text-amber-600" />
                    Đang xử lý
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <p className="text-2xl font-bold text-foreground">
                    {data.evaluatingTasks}
                  </p>
                </CardContent>
              </Card>
            </div>

            {/* Desktop table */}
            <div className="hidden md:block">
              <DataTable
                columns={columns}
                data={data.items}
                rowKey={(row) => row.taskId}
                onRowClick={(row) => {
                  if (row.currentSubmissionId) {
                    router.push(
                      `/guardian/submissions/${row.currentSubmissionId}`
                    );
                  }
                }}
              />
            </div>

            {/* Mobile stacked cards */}
            <div className="grid grid-cols-1 gap-3 md:hidden">
              {data.items.map((item) => (
                <Card key={item.taskId}>
                  <CardContent className="space-y-3 p-4">
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <p className="font-semibold text-foreground">
                          {item.subjectName} ·{' '}
                          {item.taskType === 'REVIEW'
                            ? 'Ôn bài cũ'
                            : 'Soạn bài mới'}
                        </p>
                        <p className="break-words text-sm text-muted-foreground [overflow-wrap:anywhere]">
                          {item.lessonTitle ?? 'Chưa nộp bài'}
                        </p>
                      </div>
                      <StatusBadge status={item.status} />
                    </div>

                    {(item.evaluation || item.guardianAcknowledgedAt) ? (
                      <div className="flex flex-wrap gap-1.5">
                        {item.evaluation ? (
                          <>
                            <StatusBadge status={item.evaluation.overallStatus} />
                            <StatusBadge status={item.evaluation.accuracy} />
                          </>
                        ) : null}
                        {item.guardianAcknowledgedAt ? (
                          <Badge variant="success" className="whitespace-nowrap">
                            Đã xem
                          </Badge>
                        ) : null}
                      </div>
                    ) : null}

                    {item.guardianNote ? (
                      <p className="break-words rounded-md bg-muted/50 px-2.5 py-1.5 text-xs text-muted-foreground [overflow-wrap:anywhere]">
                        Lời nhắn của phụ huynh: &ldquo;{item.guardianNote}&rdquo;
                      </p>
                    ) : null}

                    {item.currentSubmissionId ? (
                      <Button
                        asChild
                        variant="outline"
                        className="h-11 w-full justify-between"
                      >
                        <Link
                          href={`/guardian/submissions/${item.currentSubmissionId}`}
                        >
                          <span>Xem bài gốc &amp; nhận xét</span>
                          <ArrowRight className="h-4 w-4" strokeWidth={1.75} />
                        </Link>
                      </Button>
                    ) : canToggleSkip(item.status) ? (
                      <Button
                        type="button"
                        variant="outline"
                        disabled={
                          skipMutation.isPending &&
                          skipMutation.variables?.taskId === item.taskId
                        }
                        className="h-11 w-full justify-center gap-2"
                        onClick={() =>
                          skipMutation.mutate({
                            taskId: item.taskId,
                            skipped: item.status !== 'SKIPPED',
                          })
                        }
                      >
                        {item.status === 'SKIPPED' ? (
                          <>
                            <RotateCcw className="h-4 w-4" strokeWidth={1.75} />
                            <span>Khôi phục bài học</span>
                          </>
                        ) : (
                          <>
                            <SkipForward className="h-4 w-4" strokeWidth={1.75} />
                            <span>Đánh dấu bỏ qua hôm nay</span>
                          </>
                        )}
                      </Button>
                    ) : null}
                  </CardContent>
                </Card>
              ))}
            </div>
          </div>
        )}
      </QueryStateBoundary>
      )}
    </div>
  );
}
