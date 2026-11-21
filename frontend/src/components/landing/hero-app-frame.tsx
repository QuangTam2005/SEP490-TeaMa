'use client';

import { useState } from 'react';
import {
  CalendarRange,
  FileCode2,
  GitMerge,
  GitPullRequest,
  Paperclip,
} from 'lucide-react';
import { StatusBadge } from '@/components/shared/status-badge';
import { SEED_MILESTONES_101 } from '@/mocks/fixtures/seed-data';
import { cn } from '@/lib/utils';

type PreviewTab = 'milestones' | 'wbs' | 'github';

interface PreviewWbsRow {
  code: string;
  title: string;
  category: 'DESIGN' | 'CODING' | 'TESTING';
  fields: number;
  transactions: number;
  complexity: 'SIMPLE' | 'MEDIUM' | 'COMPLEX';
  mandays: string;
}

interface PreviewPrTask {
  code: string;
  title: string;
  status: 'DONE' | 'IN_REVIEW';
  prs: {
    repo: string;
    number: number;
    state: 'MERGED' | 'OPEN';
  }[];
}

const PREVIEW_WBS_ROWS: PreviewWbsRow[] = [
  {
    code: 'TASK-08',
    title: 'Thiết kế ERD 22 bảng & Flyway V1–V4',
    category: 'DESIGN',
    fields: 18,
    transactions: 6,
    complexity: 'COMPLEX',
    mandays: '3.0d',
  },
  {
    code: 'TASK-12',
    title: 'API gửi lời mời Email & xác thực Token',
    category: 'CODING',
    fields: 10,
    transactions: 5,
    complexity: 'MEDIUM',
    mandays: '2.0d',
  },
  {
    code: 'TASK-15',
    title: 'Ma trận Screen Authorization 8 mức quyền',
    category: 'CODING',
    fields: 14,
    transactions: 8,
    complexity: 'COMPLEX',
    mandays: '2.5d',
  },
  {
    code: 'TASK-21',
    title: 'System Test & Evidence Upload (<= 5MB)',
    category: 'TESTING',
    fields: 6,
    transactions: 3,
    complexity: 'SIMPLE',
    mandays: '1.5d',
  },
];

const PREVIEW_PR_TASKS: PreviewPrTask[] = [
  {
    code: 'TASK-12',
    title: 'Owner Email Invitation & MailSender Flow',
    status: 'DONE',
    prs: [
      { repo: 'BE', number: 14, state: 'MERGED' },
      { repo: 'FE', number: 9, state: 'MERGED' },
    ],
  },
  {
    code: 'TASK-15',
    title: 'ScreenAuthMatrix 8-level JSONB Toggle',
    status: 'IN_REVIEW',
    prs: [
      { repo: 'BE', number: 18, state: 'MERGED' },
      { repo: 'FE', number: 12, state: 'OPEN' },
    ],
  },
  {
    code: 'TASK-24',
    title: 'Apache POI 4-Sheet Project Tracking Exporter',
    status: 'IN_REVIEW',
    prs: [{ repo: 'BE', number: 21, state: 'OPEN' }],
  },
];

const PREVIEW_TABS: {
  id: PreviewTab;
  label: string;
  icon: typeof CalendarRange;
}[] = [
  { id: 'milestones', label: '10 Cột mốc (15T)', icon: CalendarRange },
  { id: 'wbs', label: 'WBS & Complexity', icon: FileCode2 },
  { id: 'github', label: 'GitHub Multi-Repo', icon: GitPullRequest },
];

export function HeroAppFrame() {
  const [activeTab, setActiveTab] = useState<PreviewTab>('milestones');
  const previewMilestones = SEED_MILESTONES_101.slice(0, 5);

  return (
    <div className="overflow-hidden rounded-lg border bg-card">
      {/* Window Header */}
      <div className="flex flex-wrap items-center justify-between gap-2 border-b bg-muted/50 px-4 py-2.5">
        <div className="flex items-center gap-2">
          <span className="h-2.5 w-2.5 rounded-full bg-border" />
          <span className="h-2.5 w-2.5 rounded-full bg-border" />
          <span className="h-2.5 w-2.5 rounded-full bg-border" />
          <span className="ml-2 font-mono text-xs font-medium text-foreground">
            SEP490-G12 • TeaMa Copilot
          </span>
        </div>
        <span className="font-mono text-xs text-muted-foreground tabular-nums">
          W05 / 15 • 3/10 Cột mốc đạt
        </span>
      </div>

      {/* Interactive Tab Bar */}
      <div
        role="tablist"
        aria-label="Xem trước màn hình ứng dụng"
        className="flex border-b bg-card px-2"
      >
        {PREVIEW_TABS.map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              type="button"
              role="tab"
              aria-selected={isActive}
              onClick={() => setActiveTab(tab.id)}
              className={cn(
                'relative inline-flex h-10 items-center gap-1.5 px-3 text-xs font-medium transition-colors duration-150',
                isActive
                  ? 'text-foreground'
                  : 'text-muted-foreground hover:text-foreground'
              )}
            >
              <Icon
                className={cn(
                  'h-3.5 w-3.5',
                  isActive ? 'text-brand' : 'text-muted-foreground'
                )}
                strokeWidth={1.75}
              />
              <span>{tab.label}</span>
              {isActive ? (
                <span className="absolute inset-x-2 bottom-0 h-0.5 bg-brand" />
              ) : null}
            </button>
          );
        })}
      </div>

      {/* Live HTML Product View */}
      <div className="min-h-[296px] p-4">
        {activeTab === 'milestones' ? (
          <div className="space-y-2">
            <div className="grid grid-cols-12 px-2 pb-1 text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
              <span className="col-span-2">Mã</span>
              <span className="col-span-5">Cột mốc</span>
              <span className="col-span-2 text-right">Tỷ trọng</span>
              <span className="col-span-3 text-right">Trạng thái</span>
            </div>
            <div className="divide-y rounded-md border">
              {previewMilestones.map((m) => (
                <div
                  key={m.id}
                  className="grid grid-cols-12 items-center px-2.5 py-2 text-xs transition-colors hover:bg-muted/40"
                >
                  <span className="col-span-2 font-mono font-semibold text-foreground tabular-nums">
                    {m.milestoneCode}
                  </span>
                  <div className="col-span-5 min-w-0 pr-2">
                    <p className="truncate font-medium text-foreground">
                      {m.name}
                    </p>
                    <p className="flex items-center gap-1 font-mono text-[11px] text-muted-foreground tabular-nums">
                      <Paperclip className="h-3 w-3 shrink-0" strokeWidth={1.75} />
                      <span>
                        W{String(m.weekNumber).padStart(2, '0')} •{' '}
                        {m.deliverables.length} đầu mục
                      </span>
                    </p>
                  </div>
                  <span className="col-span-2 text-right font-mono font-medium text-foreground tabular-nums">
                    {m.ogaWeightPercent > 0 ? `${m.ogaWeightPercent}%` : '—'}
                  </span>
                  <div className="col-span-3 flex justify-end">
                    <StatusBadge status={m.status} />
                  </div>
                </div>
              ))}
            </div>
          </div>
        ) : null}

        {activeTab === 'wbs' ? (
          <div className="space-y-2">
            <div className="grid grid-cols-12 px-2 pb-1 text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
              <span className="col-span-2">Task</span>
              <span className="col-span-5">Hạng mục WBS</span>
              <span className="col-span-2 text-right">F / T</span>
              <span className="col-span-3 text-right">Độ phức tạp</span>
            </div>
            <div className="divide-y rounded-md border">
              {PREVIEW_WBS_ROWS.map((row) => (
                <div
                  key={row.code}
                  className="grid grid-cols-12 items-center px-2.5 py-2 text-xs transition-colors hover:bg-muted/40"
                >
                  <span className="col-span-2 font-mono font-semibold text-foreground tabular-nums">
                    {row.code}
                  </span>
                  <div className="col-span-5 min-w-0 pr-2">
                    <p className="truncate font-medium text-foreground">
                      {row.title}
                    </p>
                    <p className="font-mono text-[11px] text-muted-foreground tabular-nums">
                      {row.category} • {row.mandays}
                    </p>
                  </div>
                  <span className="col-span-2 text-right font-mono text-muted-foreground tabular-nums">
                    {row.fields}F / {row.transactions}T
                  </span>
                  <div className="col-span-3 flex justify-end">
                    <StatusBadge status={row.complexity} />
                  </div>
                </div>
              ))}
            </div>
          </div>
        ) : null}

        {activeTab === 'github' ? (
          <div className="space-y-2.5">
            {PREVIEW_PR_TASKS.map((task) => (
              <div
                key={task.code}
                className="rounded-md border p-3 transition-colors hover:bg-muted/30"
              >
                <div className="flex items-center justify-between gap-2">
                  <div className="flex min-w-0 items-center gap-2">
                    <span className="font-mono text-xs font-semibold text-brand">
                      [{task.code}]
                    </span>
                    <p className="truncate text-xs font-medium text-foreground">
                      {task.title}
                    </p>
                  </div>
                  <StatusBadge status={task.status} />
                </div>
                <div className="mt-2.5 flex flex-wrap items-center gap-2">
                  {task.prs.map((pr) => (
                    <span
                      key={`${pr.repo}-${pr.number}`}
                      className={cn(
                        'inline-flex items-center gap-1 rounded border px-2 py-0.5 font-mono text-[11px] font-medium tabular-nums',
                        pr.state === 'MERGED'
                          ? 'border-emerald-200 bg-emerald-50 text-emerald-700 dark:border-emerald-900 dark:bg-emerald-950/60 dark:text-emerald-300'
                          : 'border-amber-200 bg-amber-50 text-amber-700 dark:border-amber-900 dark:bg-amber-950/60 dark:text-amber-300'
                      )}
                    >
                      {pr.state === 'MERGED' ? (
                        <GitMerge className="h-3 w-3" strokeWidth={1.75} />
                      ) : (
                        <GitPullRequest className="h-3 w-3" strokeWidth={1.75} />
                      )}
                      <span>
                        {pr.repo} #{pr.number} • {pr.state}
                      </span>
                    </span>
                  ))}
                </div>
              </div>
            ))}
          </div>
        ) : null}
      </div>
    </div>
  );
}
