'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  AlertCircle,
  CalendarDays,
  CheckCircle2,
  ClipboardList,
  Download,
  ExternalLink,
  Link2,
  Loader2,
  ShieldCheck,
  Sparkles,
  Users,
} from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { QueryStateBoundary } from '@/components/shared/query-state-boundary';
import { extractApiErrorMessage } from '@/components/shared/form-error-utils';
import { AssignCustomTaskDialog } from '@/components/guardian/assign-custom-task-dialog';
import {
  GuardianRejectSkipDialog,
  GuardianVerifyRedoDialog,
} from '@/components/guardian/guardian-note-dialogs';
import type { GuardianTaskActionHandlers } from '@/components/guardian/guardian-task-actions';
import { GuardianTaskTable } from '@/components/guardian/guardian-task-table';
import { GuardianWeeklyOverview } from '@/components/guardian/guardian-weekly-overview';
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
import type {
  GuardianAssignCustomTaskRequest,
  GuardianTaskItem,
  SubjectCode,
} from '@/types';

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
  const [rejectTarget, setRejectTarget] = useState<GuardianTaskItem | null>(
    null
  );
  const [rejectNote, setRejectNote] = useState('');
  const [assignTarget, setAssignTarget] = useState<GuardianTaskItem | null>(
    null
  );
  const [verifyTarget, setVerifyTarget] = useState<GuardianTaskItem | null>(
    null
  );
  const [verifyNote, setVerifyNote] = useState('');

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

  const invalidateAllPlanAndGuardianQueries = () => {
    void queryClient.invalidateQueries({ queryKey: ['guardian-dashboard'] });
    void queryClient.invalidateQueries({ queryKey: ['guardian-weekly'] });
    void queryClient.invalidateQueries({ queryKey: ['next-day-plan'] });
    void queryClient.invalidateQueries({ queryKey: ['student-task'] });
    void queryClient.invalidateQueries({ queryKey: ['student-history'] });
    void queryClient.invalidateQueries({ queryKey: ['student-streak'] });
  };

  const skipMutation = useMutation({
    mutationFn: ({ taskId, skipped }: { taskId: number; skipped: boolean }) =>
      guardianService.skipTask(taskId, { skipped }),
    onSuccess: (res) => {
      invalidateAllPlanAndGuardianQueries();
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

  const reviewSkipMutation = useMutation({
    mutationFn: ({
      taskId,
      approved,
      note,
    }: {
      taskId: number;
      approved: boolean;
      note?: string;
    }) => guardianService.reviewSkipRequest(taskId, { approved, note }),
    onSuccess: (res) => {
      invalidateAllPlanAndGuardianQueries();
      setRejectTarget(null);
      setRejectNote('');
      toast.success(
        res.status === 'SKIPPED'
          ? 'Đã đồng ý cho con bỏ qua bài học'
          : 'Đã từ chối yêu cầu xin bỏ qua của con'
      );
    },
    onError: (err) => {
      toast.error(extractApiErrorMessage(err));
    },
  });

  const assignCustomMutation = useMutation({
    mutationFn: ({
      taskId,
      payload,
    }: {
      taskId: number;
      payload: GuardianAssignCustomTaskRequest;
    }) => guardianService.assignCustomTask(taskId, payload),
    onSuccess: (res) => {
      invalidateAllPlanAndGuardianQueries();
      setAssignTarget(null);
      toast.success(
        res.customDriveFolderUrl
          ? 'Đã giao nhiệm vụ thay thế kèm thư mục Google Drive'
          : 'Đã giao nhiệm vụ thay thế cho con'
      );
    },
    onError: (err) => {
      toast.error(extractApiErrorMessage(err));
    },
  });

  const clearCustomMutation = useMutation({
    mutationFn: (taskId: number) => guardianService.clearCustomTask(taskId),
    onSuccess: () => {
      invalidateAllPlanAndGuardianQueries();
      toast.success('Đã chuyển nhiệm vụ về chế độ AI mặc định');
    },
    onError: (err) => {
      toast.error(extractApiErrorMessage(err));
    },
  });

  const verifyCustomMutation = useMutation({
    mutationFn: ({
      taskId,
      approved,
      note,
    }: {
      taskId: number;
      approved: boolean;
      note?: string;
    }) => guardianService.verifyCustomTask(taskId, { approved, note }),
    onSuccess: (res) => {
      invalidateAllPlanAndGuardianQueries();
      setVerifyTarget(null);
      setVerifyNote('');
      toast.success(
        res.status === 'EVALUATED'
          ? res.customDriveFolderUrl
            ? 'Đã xác nhận con hoàn thành bài nộp trên Drive'
            : 'Đã xác nhận con hoàn thành nhiệm vụ'
          : res.customDriveFolderUrl
            ? 'Đã gửi lời nhắc con bổ sung bài trên Drive'
            : 'Đã gửi lời nhắc con thực hiện lại nhiệm vụ'
      );
    },
    onError: (err) => {
      toast.error(extractApiErrorMessage(err));
    },
  });

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

  const actionHandlers: GuardianTaskActionHandlers = {
    onVerifyApprove: (taskId) =>
      verifyCustomMutation.mutate({ taskId, approved: true }),
    onOpenVerifyRedo: (row) => {
      setVerifyTarget(row);
      setVerifyNote('');
    },
    onApproveSkip: (taskId) =>
      reviewSkipMutation.mutate({ taskId, approved: true }),
    onOpenRejectSkip: (row) => {
      setRejectTarget(row);
      setRejectNote('');
    },
    onOpenAssignCustom: (row) => setAssignTarget(row),
    onClearCustom: (taskId) => clearCustomMutation.mutate(taskId),
    onToggleSkip: (taskId, skipped) =>
      skipMutation.mutate({ taskId, skipped }),
    verifyingTaskId: verifyCustomMutation.isPending
      ? (verifyCustomMutation.variables?.taskId ?? null)
      : null,
    reviewingSkipTaskId: reviewSkipMutation.isPending
      ? (reviewSkipMutation.variables?.taskId ?? null)
      : null,
    togglingSkipTaskId: skipMutation.isPending
      ? (skipMutation.variables?.taskId ?? null)
      : null,
    clearingCustomTaskId: clearCustomMutation.isPending
      ? (clearCustomMutation.variables ?? null)
      : null,
  };

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
            <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-muted-foreground">
              <span>
                Học sinh:{' '}
                <strong className="text-foreground">
                  {dashboardQuery.data.linkedStudentName}
                </strong>{' '}
                · Lớp {dashboardQuery.data.grade}
              </span>
              {dashboardQuery.data.defaultDriveFolderUrl ? (
                <a
                  href={dashboardQuery.data.defaultDriveFolderUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-1 text-xs font-semibold text-brand-strong hover:underline dark:text-sky-300"
                >
                  Thư mục Drive mặc định
                  <ExternalLink className="h-3 w-3" />
                </a>
              ) : null}
            </div>
          ) : null}
        </div>

        <div className="flex w-full flex-col gap-2.5 sm:w-auto sm:flex-row sm:flex-wrap sm:items-center">
          {childrenQuery.data && childrenQuery.data.length > 1 ? (
            <div className="flex w-full items-center gap-1.5 sm:w-auto">
              <Users
                className="h-4 w-4 shrink-0 text-muted-foreground"
                strokeWidth={1.75}
              />
              <select
                aria-label="Chọn học sinh đang theo dõi"
                value={selectedStudentId ?? ''}
                onChange={(e) => {
                  const id = Number(e.target.value);
                  setSelectedStudentId(Number.isFinite(id) ? id : null);
                  setSelectedSubject('ALL');
                }}
                className="h-11 w-full rounded-lg border bg-background px-3 text-base font-semibold text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring sm:h-10 sm:w-auto sm:text-sm"
              >
                {childrenQuery.data.map((child) => (
                  <option key={child.studentId} value={child.studentId}>
                    {child.displayName} (Lớp {child.grade})
                  </option>
                ))}
              </select>
            </div>
          ) : null}

          <div className="flex w-full items-center gap-2 sm:w-auto">
            <CalendarDays
              className="h-4 w-4 shrink-0 text-muted-foreground"
              strokeWidth={1.75}
            />
            <Input
              type="date"
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
              aria-label="Chọn ngày theo dõi"
              className="h-11 w-full sm:h-10 sm:w-44"
            />
          </div>

          <Button
            type="button"
            variant="outline"
            className="h-11 w-full gap-1.5 sm:h-10 sm:w-auto"
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

      {weeklyQuery.data ? (
        <GuardianWeeklyOverview
          weekly={weeklyQuery.data}
          selectedDate={selectedDate}
          onSelectDate={setSelectedDate}
          onShiftWeek={(delta) =>
            setWeekStart((prev) => shiftIsoDate(prev, delta))
          }
        />
      ) : null}

      <div className="-mx-1 flex flex-nowrap gap-2 overflow-x-auto px-1 pb-1 sm:mx-0 sm:flex-wrap sm:overflow-visible sm:px-0 sm:pb-0">
        {subjectFilters.map((filter) => (
          <button
            key={filter.code}
            type="button"
            onClick={() => setSelectedSubject(filter.code)}
            className={cn(
              'h-11 shrink-0 whitespace-nowrap rounded-lg border px-3.5 text-xs font-semibold transition-colors sm:h-10',
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
          {(data) => {
            const pendingSkipCount = data.items.filter(
              (item) => item.skipRequestStatus === 'PENDING'
            ).length;
            return (
              <div className="space-y-6">
                {pendingSkipCount > 0 ? (
                  <div className="flex items-center gap-3 rounded-xl border border-amber-300 bg-amber-50/80 px-4 py-3 text-sm text-amber-900 dark:border-amber-800 dark:bg-amber-950/35 dark:text-amber-200">
                    <AlertCircle className="h-4 w-4 shrink-0 text-amber-600 dark:text-amber-400" />
                    <p className="font-semibold">
                      Con đang xin bỏ qua {pendingSkipCount} bài học chờ duyệt.
                    </p>
                  </div>
                ) : null}

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

                <GuardianTaskTable items={data.items} actions={actionHandlers} />
              </div>
            );
          }}
        </QueryStateBoundary>
      )}

      <GuardianRejectSkipDialog
        target={rejectTarget}
        note={rejectNote}
        onNoteChange={setRejectNote}
        isSubmitting={reviewSkipMutation.isPending}
        onClose={() => {
          setRejectTarget(null);
          setRejectNote('');
        }}
        onConfirm={(taskId, note) =>
          reviewSkipMutation.mutate({ taskId, approved: false, note })
        }
      />

      <AssignCustomTaskDialog
        task={assignTarget}
        defaultDriveFolderUrl={dashboardQuery.data?.defaultDriveFolderUrl}
        open={Boolean(assignTarget)}
        onOpenChange={(open) => {
          if (!open) {
            setAssignTarget(null);
          }
        }}
        isSubmitting={assignCustomMutation.isPending}
        onSubmit={(taskId, payload) =>
          assignCustomMutation.mutate({ taskId, payload })
        }
      />

      <GuardianVerifyRedoDialog
        target={verifyTarget}
        note={verifyNote}
        onNoteChange={setVerifyNote}
        isSubmitting={verifyCustomMutation.isPending}
        onClose={() => {
          setVerifyTarget(null);
          setVerifyNote('');
        }}
        onConfirm={(taskId, note) =>
          verifyCustomMutation.mutate({ taskId, approved: false, note })
        }
      />
    </div>
  );
}
