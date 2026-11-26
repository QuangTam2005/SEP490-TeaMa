'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  AlertCircle,
  CalendarDays,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  ClipboardList,
  Download,
  ExternalLink,
  FolderUp,
  Link2,
  Loader2,
  ShieldCheck,
  Sparkles,
  Users,
} from 'lucide-react';
import { toast } from 'sonner';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  DataTable,
  type DataTableColumn,
} from '@/components/shared/data-table';
import { QueryStateBoundary } from '@/components/shared/query-state-boundary';
import { StatusBadge } from '@/components/shared/status-badge';
import { extractApiErrorMessage } from '@/components/shared/form-error-utils';
import { AssignCustomTaskDialog } from '@/components/guardian/assign-custom-task-dialog';
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
  StudyTaskStatus,
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
    onSuccess: () => {
      invalidateAllPlanAndGuardianQueries();
      setAssignTarget(null);
      toast.success('Đã giao nhiệm vụ thay thế kèm thư mục Google Drive');
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
          ? 'Đã xác nhận con hoàn thành bài nộp trên Drive'
          : 'Đã gửi lời nhắc con bổ sung bài trên Drive'
      );
    },
    onError: (err) => {
      toast.error(extractApiErrorMessage(err));
    },
  });

  const canAssignCustomTask = (row: GuardianTaskItem) =>
    !row.currentSubmissionId &&
    !row.customSubmittedAt &&
    row.status !== 'SUBMITTED' &&
    row.status !== 'EVALUATING' &&
    row.status !== 'EVALUATED' &&
    row.status !== 'EVALUATION_FAILED';

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
          <span className="block text-xs text-muted-foreground">
            {row.taskType === 'REVIEW' ? 'Ôn bài cũ' : 'Soạn bài mới'}
          </span>
        </div>
      ),
    },
    {
      key: 'lessonTitle',
      header: 'Bài học',
      render: (row) => {
        const isCustom = row.assignmentMode === 'CUSTOM_DRIVE';
        return (
          <div className="max-w-xs space-y-1">
            {isCustom ? (
              <div className="space-y-1">
                <div className="flex flex-wrap items-center gap-1.5">
                  <Badge variant="brand" className="whitespace-nowrap">
                    PH giao (Drive)
                  </Badge>
                  {row.customDriveFolderUrl ? (
                    <a
                      href={row.customDriveFolderUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      onClick={(e) => e.stopPropagation()}
                      className="inline-flex items-center gap-1 text-xs font-semibold text-brand-strong hover:underline dark:text-sky-300"
                    >
                      Mở Drive
                      <ExternalLink className="h-3 w-3" />
                    </a>
                  ) : null}
                </div>
                <p className="break-words text-sm font-medium text-foreground [overflow-wrap:anywhere]">
                  {row.customTaskTitle}
                </p>
                {row.customSubmissionNote ? (
                  <p className="break-words rounded border border-sky-200 bg-sky-50/70 px-2 py-1 text-xs text-sky-900 dark:border-sky-900/60 dark:bg-sky-950/30 dark:text-sky-200 [overflow-wrap:anywhere]">
                    Con nộp: &ldquo;{row.customSubmissionNote}&rdquo;
                    {row.customSubmissionUrl ? (
                      <>
                        {' · '}
                        <a
                          href={row.customSubmissionUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          onClick={(e) => e.stopPropagation()}
                          className="font-semibold underline"
                        >
                          Link bài
                        </a>
                      </>
                    ) : null}
                  </p>
                ) : null}
              </div>
            ) : (
              <span className="block break-words text-sm text-foreground [overflow-wrap:anywhere]">
                {row.lessonTitle ?? 'Chưa nộp bài'}
              </span>
            )}
            {row.skipRequestReason &&
            (row.skipRequestStatus === 'PENDING' ||
              row.skipRequestStatus === 'APPROVED') ? (
              <p className="break-words rounded border border-amber-200 bg-amber-50/70 px-2 py-1 text-xs text-amber-900 dark:border-amber-900/60 dark:bg-amber-950/30 dark:text-amber-200 [overflow-wrap:anywhere]">
                Lý do con xin: &ldquo;{row.skipRequestReason}&rdquo;
              </p>
            ) : null}
          </div>
        );
      },
    },
    {
      key: 'status',
      header: 'Trạng thái',
      render: (row) => (
        <div className="flex flex-wrap items-center gap-1.5">
          <StatusBadge status={row.status} />
          {row.skipRequestStatus === 'PENDING' ? (
            <Badge variant="warning" className="whitespace-nowrap">
              Chờ duyệt bỏ qua
            </Badge>
          ) : null}
        </div>
      ),
    },
    {
      key: 'evaluation',
      header: 'AI Đánh giá',
      cellClassName: 'whitespace-nowrap',
      render: (row) =>
        row.assignmentMode === 'CUSTOM_DRIVE' ? (
          <Badge variant="secondary" className="whitespace-nowrap">
            Manual
          </Badge>
        ) : row.evaluation ? (
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
      cellClassName: 'whitespace-nowrap',
      render: (row) => {
        if (row.currentSubmissionId) {
          return (
            <Button asChild size="sm" variant="outline" className="h-8 px-2.5 text-xs whitespace-nowrap">
              <Link href={`/guardian/submissions/${row.currentSubmissionId}`}>
                Xem bài gốc
              </Link>
            </Button>
          );
        }
        if (row.assignmentMode === 'CUSTOM_DRIVE' && row.status === 'SUBMITTED') {
          const isVerifying =
            verifyCustomMutation.isPending &&
            verifyCustomMutation.variables?.taskId === row.taskId;
          return (
            <div className="flex flex-nowrap items-center justify-end gap-1.5">
              <Button
                type="button"
                size="sm"
                disabled={isVerifying}
                className="h-8 px-2.5 text-xs whitespace-nowrap"
                onClick={(e) => {
                  e.stopPropagation();
                  verifyCustomMutation.mutate({
                    taskId: row.taskId,
                    approved: true,
                  });
                }}
              >
                Xác nhận đạt
              </Button>
              <Button
                type="button"
                size="sm"
                variant="outline"
                disabled={isVerifying}
                className="h-8 px-2.5 text-xs whitespace-nowrap"
                onClick={(e) => {
                  e.stopPropagation();
                  setVerifyTarget(row);
                  setVerifyNote('');
                }}
              >
                Nhắc làm lại
              </Button>
            </div>
          );
        }
        if (row.skipRequestStatus === 'PENDING') {
          const isReviewing =
            reviewSkipMutation.isPending &&
            reviewSkipMutation.variables?.taskId === row.taskId;
          return (
            <div className="flex flex-nowrap items-center justify-end gap-1.5">
              <Button
                type="button"
                size="sm"
                disabled={isReviewing}
                className="h-8 px-2.5 text-xs whitespace-nowrap"
                onClick={(e) => {
                  e.stopPropagation();
                  reviewSkipMutation.mutate({
                    taskId: row.taskId,
                    approved: true,
                  });
                }}
              >
                Đồng ý
              </Button>
              <Button
                type="button"
                size="sm"
                variant="outline"
                disabled={isReviewing}
                className="h-8 px-2.5 text-xs whitespace-nowrap"
                onClick={(e) => {
                  e.stopPropagation();
                  setRejectTarget(row);
                  setRejectNote('');
                }}
              >
                Từ chối
              </Button>
              {canAssignCustomTask(row) ? (
                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  className="h-8 px-2.5 text-xs whitespace-nowrap"
                  onClick={(e) => {
                    e.stopPropagation();
                    setAssignTarget(row);
                  }}
                >
                  Giao bài thay thế
                </Button>
              ) : null}
            </div>
          );
        }
        if (canToggleSkip(row.status) || canAssignCustomTask(row)) {
          const isSkipped = row.status === 'SKIPPED';
          const isCustom = row.assignmentMode === 'CUSTOM_DRIVE';
          const isBusy =
            (skipMutation.isPending && skipMutation.variables?.taskId === row.taskId) ||
            (clearCustomMutation.isPending && clearCustomMutation.variables === row.taskId);
          return (
            <div className="flex flex-nowrap items-center justify-end gap-1.5">
              {canAssignCustomTask(row) ? (
                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  disabled={isBusy}
                  className="h-8 px-2.5 text-xs whitespace-nowrap"
                  onClick={(e) => {
                    e.stopPropagation();
                    setAssignTarget(row);
                  }}
                >
                  {isCustom ? 'Sửa bài giao' : 'Giao bài thay thế'}
                </Button>
              ) : null}
              {isCustom && !row.customSubmittedAt ? (
                <Button
                  type="button"
                  size="sm"
                  variant="ghost"
                  disabled={isBusy}
                  className="h-8 px-2 text-xs whitespace-nowrap"
                  onClick={(e) => {
                    e.stopPropagation();
                    clearCustomMutation.mutate(row.taskId);
                  }}
                >
                  Gỡ thay thế
                </Button>
              ) : null}
              {canToggleSkip(row.status) ? (
                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  disabled={isBusy}
                  className="h-8 px-2.5 text-xs whitespace-nowrap"
                  onClick={(e) => {
                    e.stopPropagation();
                    skipMutation.mutate({
                      taskId: row.taskId,
                      skipped: !isSkipped,
                    });
                  }}
                >
                  {isBusy ? 'Đang xử lý...' : isSkipped ? 'Khôi phục' : 'Bỏ qua'}
                </Button>
              ) : null}
            </div>
          );
        }
        if (row.assignmentMode === 'CUSTOM_DRIVE' && row.customDriveFolderUrl) {
          return (
            <Button asChild size="sm" variant="outline" className="h-8 gap-1 px-2.5 text-xs whitespace-nowrap">
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
              <Users className="h-4 w-4 shrink-0 text-muted-foreground" strokeWidth={1.75} />
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
                className="h-11 w-11 sm:h-9 sm:w-9"
                aria-label="Tuần trước"
                onClick={() => setWeekStart((prev) => shiftIsoDate(prev, -7))}
              >
                <ChevronLeft className="h-4 w-4" />
              </Button>
              <Button
                type="button"
                variant="outline"
                size="icon"
                className="h-11 w-11 sm:h-9 sm:w-9"
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

      {/* Subject filter pills (horizontally scrollable single row on mobile) */}
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
            (i) => i.skipRequestStatus === 'PENDING'
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
                      <div className="flex flex-wrap items-center justify-end gap-1.5">
                        <StatusBadge status={item.status} />
                        {item.skipRequestStatus === 'PENDING' ? (
                          <Badge variant="warning" className="whitespace-nowrap">
                            Chờ duyệt bỏ qua
                          </Badge>
                        ) : null}
                      </div>
                    </div>

                    {item.assignmentMode === 'CUSTOM_DRIVE' ? (
                      <div className="space-y-1.5 rounded-md border border-brand/25 bg-brand-soft/30 px-2.5 py-2 text-xs">
                        <div className="flex flex-wrap items-center justify-between gap-2">
                          <Badge variant="brand" className="whitespace-nowrap">
                            PH giao (Drive)
                          </Badge>
                          {item.customDriveFolderUrl ? (
                            <a
                              href={item.customDriveFolderUrl}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="inline-flex items-center gap-1 font-semibold text-brand-strong hover:underline dark:text-sky-300"
                            >
                              Mở Drive xem bài
                              <ExternalLink className="h-3 w-3" />
                            </a>
                          ) : null}
                        </div>
                        <p className="break-words font-medium text-foreground [overflow-wrap:anywhere]">
                          {item.customTaskTitle}
                        </p>
                        {item.customSubmissionNote ? (
                          <p className="break-words text-sky-900 dark:text-sky-200 [overflow-wrap:anywhere]">
                            Con nộp: &ldquo;{item.customSubmissionNote}&rdquo;
                          </p>
                        ) : null}
                      </div>
                    ) : null}

                    {item.skipRequestReason &&
                    (item.skipRequestStatus === 'PENDING' ||
                      item.skipRequestStatus === 'APPROVED') ? (
                      <p className="break-words rounded-md border border-amber-200 bg-amber-50/70 px-2.5 py-1.5 text-xs text-amber-900 dark:border-amber-900/60 dark:bg-amber-950/30 dark:text-amber-200 [overflow-wrap:anywhere]">
                        Lý do con xin bỏ qua: &ldquo;{item.skipRequestReason}&rdquo;
                      </p>
                    ) : null}

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
                        className="h-11 w-full justify-center"
                      >
                        <Link
                          href={`/guardian/submissions/${item.currentSubmissionId}`}
                        >
                          Xem bài gốc &amp; nhận xét
                        </Link>
                      </Button>
                    ) : item.assignmentMode === 'CUSTOM_DRIVE' &&
                      item.status === 'SUBMITTED' ? (
                      <div className="grid grid-cols-2 gap-2">
                        <Button
                          type="button"
                          disabled={
                            verifyCustomMutation.isPending &&
                            verifyCustomMutation.variables?.taskId === item.taskId
                          }
                          className="h-11 w-full justify-center"
                          onClick={() =>
                            verifyCustomMutation.mutate({
                              taskId: item.taskId,
                              approved: true,
                            })
                          }
                        >
                          Xác nhận đạt
                        </Button>
                        <Button
                          type="button"
                          variant="outline"
                          disabled={
                            verifyCustomMutation.isPending &&
                            verifyCustomMutation.variables?.taskId === item.taskId
                          }
                          className="h-11 w-full justify-center"
                          onClick={() => {
                            setVerifyTarget(item);
                            setVerifyNote('');
                          }}
                        >
                          Nhắc làm lại
                        </Button>
                      </div>
                    ) : item.skipRequestStatus === 'PENDING' ? (
                      <div className="space-y-2">
                        <div className="grid grid-cols-2 gap-2">
                          <Button
                            type="button"
                            disabled={
                              reviewSkipMutation.isPending &&
                              reviewSkipMutation.variables?.taskId === item.taskId
                            }
                            className="h-11 w-full justify-center"
                            onClick={() =>
                              reviewSkipMutation.mutate({
                                taskId: item.taskId,
                                approved: true,
                              })
                            }
                          >
                            Đồng ý bỏ qua
                          </Button>
                          <Button
                            type="button"
                            variant="outline"
                            disabled={
                              reviewSkipMutation.isPending &&
                              reviewSkipMutation.variables?.taskId === item.taskId
                            }
                            className="h-11 w-full justify-center"
                            onClick={() => {
                              setRejectTarget(item);
                              setRejectNote('');
                            }}
                          >
                            Từ chối
                          </Button>
                        </div>
                        {canAssignCustomTask(item) ? (
                          <Button
                            type="button"
                            variant="outline"
                            className="h-11 w-full justify-center gap-1.5"
                            onClick={() => setAssignTarget(item)}
                          >
                            <FolderUp className="h-4 w-4" />
                            Giao bài thay thế (Drive)
                          </Button>
                        ) : null}
                      </div>
                    ) : canToggleSkip(item.status) || canAssignCustomTask(item) ? (
                      <div className="space-y-2">
                        {canAssignCustomTask(item) ? (
                          <Button
                            type="button"
                            variant="outline"
                            className="h-11 w-full justify-center gap-1.5"
                            onClick={() => setAssignTarget(item)}
                          >
                            <FolderUp className="h-4 w-4" />
                            {item.assignmentMode === 'CUSTOM_DRIVE'
                              ? 'Sửa bài giao (Drive)'
                              : 'Giao bài thay thế (Drive)'}
                          </Button>
                        ) : null}
                        {item.assignmentMode === 'CUSTOM_DRIVE' &&
                        !item.customSubmittedAt ? (
                          <Button
                            type="button"
                            variant="ghost"
                            disabled={
                              clearCustomMutation.isPending &&
                              clearCustomMutation.variables === item.taskId
                            }
                            className="h-11 w-full justify-center"
                            onClick={() => clearCustomMutation.mutate(item.taskId)}
                          >
                            Gỡ bài giao thay thế
                          </Button>
                        ) : null}
                        {canToggleSkip(item.status) ? (
                          <Button
                            type="button"
                            variant="outline"
                            disabled={
                              skipMutation.isPending &&
                              skipMutation.variables?.taskId === item.taskId
                            }
                            className="h-11 w-full justify-center"
                            onClick={() =>
                              skipMutation.mutate({
                                taskId: item.taskId,
                                skipped: item.status !== 'SKIPPED',
                              })
                            }
                          >
                            {item.status === 'SKIPPED'
                              ? 'Khôi phục bài học'
                              : 'Đánh dấu bỏ qua hôm nay'}
                          </Button>
                        ) : null}
                      </div>
                    ) : null}
                  </CardContent>
                </Card>
              ))}
            </div>
          </div>
          );
        }}
      </QueryStateBoundary>
      )}

      <Dialog
        open={Boolean(rejectTarget)}
        onOpenChange={(open) => {
          if (!open) {
            setRejectTarget(null);
            setRejectNote('');
          }
        }}
      >
        <DialogContent className="w-[calc(100%-1.5rem)] sm:w-full">
          <DialogHeader>
            <DialogTitle>
              Từ chối yêu cầu bỏ qua · {rejectTarget?.subjectName}
            </DialogTitle>
            <DialogDescription>
              Giữ nguyên bài học để con tự hoàn thành.
            </DialogDescription>
          </DialogHeader>
          {rejectTarget?.skipRequestReason ? (
            <p className="break-words rounded-lg border bg-muted/50 px-3 py-2 text-xs text-muted-foreground [overflow-wrap:anywhere]">
              Lý do con gửi: &ldquo;{rejectTarget.skipRequestReason}&rdquo;
            </p>
          ) : null}
          <div className="space-y-1.5">
            <Label htmlFor="guardian-reject-note">Lời nhắn cho con (tùy chọn)</Label>
            <textarea
              id="guardian-reject-note"
              rows={3}
              maxLength={300}
              value={rejectNote}
              onChange={(e) => setRejectNote(e.target.value)}
              placeholder="Lời nhắn cho con..."
              className="w-full rounded-lg border bg-background px-3 py-2 text-base text-foreground placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring md:text-sm"
            />
          </div>
          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              className="h-11 sm:h-10"
              disabled={reviewSkipMutation.isPending}
              onClick={() => {
                setRejectTarget(null);
                setRejectNote('');
              }}
            >
              Hủy
            </Button>
            <Button
              type="button"
              className="h-11 gap-1.5 sm:h-10"
              disabled={reviewSkipMutation.isPending || !rejectTarget}
              onClick={() => {
                if (!rejectTarget) return;
                reviewSkipMutation.mutate({
                  taskId: rejectTarget.taskId,
                  approved: false,
                  note: rejectNote.trim() || undefined,
                });
              }}
            >
              {reviewSkipMutation.isPending ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : null}
              Xác nhận từ chối
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

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

      <Dialog
        open={Boolean(verifyTarget)}
        onOpenChange={(open) => {
          if (!open) {
            setVerifyTarget(null);
            setVerifyNote('');
          }
        }}
      >
        <DialogContent className="w-[calc(100%-1.5rem)] sm:w-full">
          <DialogHeader>
            <DialogTitle>
              Nhắc con làm lại · {verifyTarget?.subjectName}
            </DialogTitle>
            <DialogDescription>
              Ghi chú phần con cần sửa hoặc bổ sung trên Drive.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-1.5">
            <Label htmlFor="guardian-verify-note">Lời nhắn cho con</Label>
            <textarea
              id="guardian-verify-note"
              rows={3}
              maxLength={300}
              value={verifyNote}
              onChange={(e) => setVerifyNote(e.target.value)}
              placeholder="VD: Ảnh bài 3 hơi mờ, chụp lại giúp bố mẹ nhé."
              className="w-full rounded-lg border bg-background px-3 py-2 text-base text-foreground placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring md:text-sm"
            />
          </div>
          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              className="h-11 sm:h-10"
              disabled={verifyCustomMutation.isPending}
              onClick={() => {
                setVerifyTarget(null);
                setVerifyNote('');
              }}
            >
              Hủy
            </Button>
            <Button
              type="button"
              className="h-11 gap-1.5 sm:h-10"
              disabled={verifyCustomMutation.isPending || !verifyTarget}
              onClick={() => {
                if (!verifyTarget) return;
                verifyCustomMutation.mutate({
                  taskId: verifyTarget.taskId,
                  approved: false,
                  note: verifyNote.trim() || undefined,
                });
              }}
            >
              {verifyCustomMutation.isPending ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : null}
              Gửi nhắc nhở con
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
