'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import {
  ArrowRight,
  BookOpenCheck,
  CalendarDays,
  CheckCircle2,
  ClipboardList,
  Compass,
  Eye,
  ShieldCheck,
  Sparkles,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import {
  DataTable,
  type DataTableColumn,
} from '@/components/shared/data-table';
import { QueryStateBoundary } from '@/components/shared/query-state-boundary';
import { StatusBadge } from '@/components/shared/status-badge';
import {
  ALL_SUBJECTS,
  formatViDate,
  getNextTargetDateIso,
} from '@/lib/study-utils';
import { cn } from '@/lib/utils';
import { useMockRuntime } from '@/mocks/mock-runtime';
import { guardianService } from '@/services';
import type { GuardianTaskItem, SubjectCode } from '@/types';

const SUBJECT_FILTERS: { code: SubjectCode | 'ALL'; label: string }[] = [
  { code: 'ALL', label: 'Tất cả môn' },
  ...ALL_SUBJECTS.map((s) => ({ code: s.code, label: s.name })),
];

export function GuardianDashboardView() {
  const router = useRouter();
  const runtime = useMockRuntime();
  const [selectedDate, setSelectedDate] = useState<string>(() =>
    getNextTargetDateIso()
  );
  const [selectedSubject, setSelectedSubject] = useState<SubjectCode | 'ALL'>(
    'ALL'
  );

  const dashboardQuery = useQuery({
    queryKey: [
      'guardian-dashboard',
      selectedDate,
      selectedSubject,
      runtime.resetCounter,
      runtime.scenario,
      runtime.simulatedRole,
    ],
    queryFn: () =>
      guardianService.getDashboard({
        date: selectedDate,
        subjectCode: selectedSubject,
      }),
  });

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
        <span className="text-sm text-foreground">
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
      header: 'Phiên bản',
      align: 'center',
      mono: true,
      render: (row) => (
        <span>{row.currentSubmissionId ? `${Math.min(row.revisionCount + 1, 3)}/3` : '0/3'}</span>
      ),
    },
    {
      key: 'action',
      header: 'Chi tiết',
      align: 'right',
      render: (row) =>
        row.currentSubmissionId ? (
          <Button asChild size="sm" variant="outline" className="h-9 gap-1.5">
            <Link href={`/guardian/submissions/${row.currentSubmissionId}`}>
              <Eye className="h-3.5 w-3.5" strokeWidth={1.75} />
              Xem bài gốc
            </Link>
          </Button>
        ) : (
          <span className="text-xs text-muted-foreground">—</span>
        ),
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
      </div>

      {/* Subject filter pills */}
      <div className="flex flex-wrap gap-2">
        {SUBJECT_FILTERS.map((filter) => (
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

      <QueryStateBoundary
        data={dashboardQuery.data}
        isLoading={dashboardQuery.isLoading}
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
                        <p className="text-sm text-muted-foreground">
                          {item.lessonTitle ?? 'Chưa nộp bài'}
                        </p>
                      </div>
                      <StatusBadge status={item.status} />
                    </div>

                    {item.evaluation ? (
                      <div className="flex flex-wrap gap-1.5">
                        <StatusBadge status={item.evaluation.overallStatus} />
                        <StatusBadge status={item.evaluation.accuracy} />
                      </div>
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
                    ) : null}
                  </CardContent>
                </Card>
              ))}
            </div>
          </div>
        )}
      </QueryStateBoundary>
    </div>
  );
}
