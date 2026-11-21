'use client';

import { useMemo, useState } from 'react';
import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import {
  CalendarRange,
  CheckCircle2,
  ChevronDown,
  ChevronRight,
  FileCheck2,
  GitBranch,
  Settings,
  Users,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { StatusBadge } from '@/components/shared/status-badge';
import { QueryStateBoundary } from '@/components/shared/query-state-boundary';
import { DeliverableUploadRow } from '@/components/workspace/deliverable-upload-row';
import { useMockRuntime } from '@/mocks/mock-runtime';
import { projectService } from '@/services';
import type { MilestoneStatus } from '@/types';
import { cn } from '@/lib/utils';

interface OverviewMilestonesViewProps {
  projectId: number;
}

type FilterStatus = 'ALL' | MilestoneStatus;

const FILTER_OPTIONS: { value: FilterStatus; label: string }[] = [
  { value: 'ALL', label: 'Tất cả (10)' },
  { value: 'PENDING', label: 'Chờ nộp' },
  { value: 'SUBMITTED', label: 'Đã nộp' },
  { value: 'NEEDS_REVISION', label: 'Cần sửa' },
  { value: 'APPROVED', label: 'Đã duyệt' },
];

export function OverviewMilestonesView({
  projectId,
}: OverviewMilestonesViewProps) {
  const [filterStatus, setFilterStatus] = useState<FilterStatus>('ALL');
  const [expandedIds, setExpandedIds] = useState<Record<number, boolean>>({
    1004: true,
  });
  const mockRuntime = useMockRuntime();
  const isSupervisor = mockRuntime.simulatedRole === 'SUPERVISOR';

  const projectQuery = useQuery({
    queryKey: ['project', projectId],
    queryFn: () => projectService.getProject(projectId),
  });

  const milestonesQuery = useQuery({
    queryKey: ['project-milestones', projectId],
    queryFn: () => projectService.getMilestones(projectId),
  });

  const stats = useMemo(() => {
    const list = milestonesQuery.data ?? [];
    let approvedOga = 0;
    let submittedOga = 0;
    let approvedMilestones = 0;
    let totalDeliverables = 0;
    let submittedDeliverables = 0;

    for (const m of list) {
      if (m.status === 'APPROVED') {
        approvedMilestones += 1;
        approvedOga += m.ogaWeightPercent;
      } else if (m.status === 'SUBMITTED') {
        submittedOga += m.ogaWeightPercent;
      }
      for (const d of m.deliverables) {
        totalDeliverables += 1;
        if (d.status === 'SUBMITTED' || d.status === 'APPROVED') {
          submittedDeliverables += 1;
        }
      }
    }

    return {
      approvedOga,
      submittedOga,
      approvedMilestones,
      totalMilestones: list.length,
      totalDeliverables,
      submittedDeliverables,
    };
  }, [milestonesQuery.data]);

  const filteredMilestones = useMemo(() => {
    const list = milestonesQuery.data ?? [];
    if (filterStatus === 'ALL') {
      return list;
    }
    return list.filter((m) => m.status === filterStatus);
  }, [milestonesQuery.data, filterStatus]);

  const toggleMilestone = (id: number) => {
    setExpandedIds((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  const expandAll = (open: boolean) => {
    const next: Record<number, boolean> = {};
    for (const m of milestonesQuery.data ?? []) {
      next[m.id] = open;
    }
    setExpandedIds(next);
  };

  const project = projectQuery.data;

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
        <div>
          <p className="font-mono text-xs font-medium text-brand">
            {project?.projectCode ?? `Project #${projectId}`} • Nhóm{' '}
            {project?.groupName ?? '—'}
          </p>
          <h1 className="mt-1 text-2xl font-bold tracking-tight text-foreground">
            {project?.projectName ?? 'Tổng quan & 10 Cột mốc 15 tuần'}
          </h1>
        </div>

        <Button variant="outline" asChild className="h-9 self-start sm:self-auto">
          <Link href={`/projects/${projectId}/settings`}>
            <Settings className="h-4 w-4" strokeWidth={1.75} />
            Thành viên &amp; Magic Link
          </Link>
        </Button>
      </div>

      {/* 4 Summary Metric Tiles */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <div className="rounded-lg border bg-card p-4">
          <div className="flex items-center justify-between text-xs text-muted-foreground">
            <span>Tỷ trọng hồ sơ đã duyệt</span>
            <CheckCircle2 className="h-4 w-4 text-brand" strokeWidth={1.75} />
          </div>
          <p className="mt-2 font-mono text-2xl font-bold text-foreground tabular-nums">
            {stats.approvedOga}%
          </p>
          <p className="mt-1 font-mono text-xs text-muted-foreground tabular-nums">
            +{stats.submittedOga}% chờ GVHD xem xét
          </p>
        </div>

        <div className="rounded-lg border bg-card p-4">
          <div className="flex items-center justify-between text-xs text-muted-foreground">
            <span>Cột mốc hoàn thành</span>
            <CalendarRange
              className="h-4 w-4 text-muted-foreground"
              strokeWidth={1.75}
            />
          </div>
          <p className="mt-2 font-mono text-2xl font-bold text-foreground tabular-nums">
            {stats.approvedMilestones} / {stats.totalMilestones || 10}
          </p>
        </div>

        <div className="rounded-lg border bg-card p-4">
          <div className="flex items-center justify-between text-xs text-muted-foreground">
            <span>Đầu mục đã nộp</span>
            <FileCheck2
              className="h-4 w-4 text-muted-foreground"
              strokeWidth={1.75}
            />
          </div>
          <p className="mt-2 font-mono text-2xl font-bold text-foreground tabular-nums">
            {stats.submittedDeliverables} / {stats.totalDeliverables}
          </p>
        </div>

        <div className="rounded-lg border bg-card p-4">
          <div className="flex items-center justify-between text-xs text-muted-foreground">
            <span>Nhóm &amp; Multi-Repo</span>
            <Users
              className="h-4 w-4 text-muted-foreground"
              strokeWidth={1.75}
            />
          </div>
          <p className="mt-2 flex items-center gap-3 font-mono text-2xl font-bold text-foreground tabular-nums">
            <span>{project?.memberCount ?? 4} SV</span>
            <span className="text-sm font-normal text-muted-foreground">•</span>
            <span className="inline-flex items-center gap-1 text-lg">
              <GitBranch className="h-4 w-4 text-brand" strokeWidth={1.75} />
              {project?.repoCount ?? 2}
            </span>
          </p>
          <p className="mt-1 truncate text-xs text-muted-foreground">
            GVHD: {project?.supervisorName ?? 'Chưa cập nhật'}
          </p>
        </div>
      </div>

      {/* Filter & Bulk Controls */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-1.5">
          {FILTER_OPTIONS.map((opt) => (
            <button
              key={opt.value}
              type="button"
              onClick={() => setFilterStatus(opt.value)}
              className={cn(
                'h-8 rounded-md border px-3 text-xs font-medium transition-colors',
                filterStatus === opt.value
                  ? 'border-brand bg-brand-soft text-brand'
                  : 'bg-card text-muted-foreground hover:text-foreground'
              )}
            >
              {opt.label}
            </button>
          ))}
        </div>

        <div className="flex items-center gap-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => expandAll(true)}
          >
            Mở tất cả
          </Button>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => expandAll(false)}
          >
            Thu gọn
          </Button>
        </div>
      </div>

      {/* 10 Milestones Accordion */}
      <QueryStateBoundary
        data={filteredMilestones}
        isLoading={milestonesQuery.isLoading || projectQuery.isLoading}
        isError={milestonesQuery.isError || projectQuery.isError}
        error={milestonesQuery.error ?? projectQuery.error}
        onRetry={() => {
          void projectQuery.refetch();
          void milestonesQuery.refetch();
        }}
        emptyState={{
          icon: CalendarRange,
          title: 'Không có cột mốc phù hợp bộ lọc',
          description:
            'Chưa có cột mốc nào thuộc trạng thái đang chọn hoặc dự án chưa khởi tạo danh sách cột mốc.',
          action:
            filterStatus !== 'ALL' ? (
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setFilterStatus('ALL')}
              >
                Hiển thị tất cả cột mốc
              </Button>
            ) : undefined,
        }}
      >
        {(milestones) => (
          <div className="space-y-3">
            {milestones.map((milestone) => {
              const isOpen = Boolean(expandedIds[milestone.id]);
              const submittedCount = milestone.deliverables.filter(
                (d) => d.status === 'SUBMITTED' || d.status === 'APPROVED'
              ).length;

              return (
                <div
                  key={milestone.id}
                  className="overflow-hidden rounded-lg border bg-card"
                >
                  <button
                    type="button"
                    onClick={() => toggleMilestone(milestone.id)}
                    aria-expanded={isOpen}
                    className="flex w-full flex-wrap items-center justify-between gap-3 px-4 py-3.5 text-left transition-colors hover:bg-muted/40"
                  >
                    <div className="flex min-w-0 items-center gap-3">
                      {isOpen ? (
                        <ChevronDown
                          className="h-4 w-4 shrink-0 text-muted-foreground"
                          strokeWidth={1.75}
                        />
                      ) : (
                        <ChevronRight
                          className="h-4 w-4 shrink-0 text-muted-foreground"
                          strokeWidth={1.75}
                        />
                      )}

                      <span className="rounded border bg-muted px-2 py-0.5 font-mono text-xs font-semibold text-foreground tabular-nums">
                        {milestone.milestoneCode}
                      </span>

                      <div className="min-w-0">
                        <p className="truncate text-sm font-semibold text-foreground">
                          {milestone.name}
                        </p>
                        <p className="font-mono text-xs text-muted-foreground tabular-nums">
                          Tuần {String(milestone.weekNumber).padStart(2, '0')}
                          {milestone.dueDate ? ` • Hạn ${milestone.dueDate}` : ''}
                          {' • '}
                          {submittedCount}/{milestone.deliverables.length} đầu mục
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-2.5 pl-7 sm:pl-0">
                      <span className="rounded border bg-canvas px-2 py-0.5 font-mono text-xs font-semibold text-foreground tabular-nums">
                        {milestone.ogaWeightPercent > 0
                          ? `Ref ${milestone.ogaWeightPercent}%`
                          : 'Iteration'}
                      </span>
                      <StatusBadge status={milestone.status} />
                    </div>
                  </button>

                  {isOpen ? (
                    <div className="divide-y border-t bg-canvas/40">
                      {milestone.reviewNote ? (
                        <div className="bg-brand-soft/60 px-4 py-2.5 text-xs text-foreground">
                          <span className="font-semibold text-brand">
                            Nhận xét GVHD:{' '}
                          </span>
                          <span>{milestone.reviewNote}</span>
                        </div>
                      ) : null}
                      {milestone.deliverables.map((deliverable) => (
                        <DeliverableUploadRow
                          key={deliverable.id}
                          projectId={projectId}
                          deliverable={deliverable}
                          readOnly={isSupervisor}
                        />
                      ))}
                    </div>
                  ) : null}
                </div>
              );
            })}
          </div>
        )}
      </QueryStateBoundary>
    </div>
  );
}
