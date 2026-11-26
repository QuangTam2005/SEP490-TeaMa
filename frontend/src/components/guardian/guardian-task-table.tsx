'use client';

import { useRouter } from 'next/navigation';
import { ExternalLink } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent } from '@/components/ui/card';
import {
  DataTable,
  type DataTableColumn,
} from '@/components/shared/data-table';
import { StatusBadge } from '@/components/shared/status-badge';
import {
  GuardianTaskActions,
  type GuardianTaskActionHandlers,
} from '@/components/guardian/guardian-task-actions';
import type { GuardianTaskItem } from '@/types';

interface GuardianTaskTableProps {
  items: GuardianTaskItem[];
  actions: GuardianTaskActionHandlers;
}

export function GuardianTaskTable({ items, actions }: GuardianTaskTableProps) {
  const router = useRouter();

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
                    {row.customDriveFolderUrl ? 'PH giao (Drive)' : 'PH giao'}
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
                {row.customSubmissionNote || row.customSubmissionUrl ? (
                  <p className="break-words rounded border border-sky-200 bg-sky-50/70 px-2 py-1 text-xs text-sky-900 [overflow-wrap:anywhere] dark:border-sky-900/60 dark:bg-sky-950/30 dark:text-sky-200">
                    {row.customSubmissionNote ? (
                      <>Con nộp: &ldquo;{row.customSubmissionNote}&rdquo;</>
                    ) : null}
                    {row.customSubmissionUrl ? (
                      <>
                        {row.customSubmissionNote ? ' · ' : null}
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
              <p className="break-words rounded border border-amber-200 bg-amber-50/70 px-2 py-1 text-xs text-amber-900 [overflow-wrap:anywhere] dark:border-amber-900/60 dark:bg-amber-950/30 dark:text-amber-200">
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
      render: (row) => (
        <GuardianTaskActions row={row} variant="table" {...actions} />
      ),
    },
  ];

  return (
    <>
      <div className="hidden md:block">
        <DataTable
          columns={columns}
          data={items}
          rowKey={(row) => row.taskId}
          onRowClick={(row) => {
            if (row.currentSubmissionId) {
              router.push(`/guardian/submissions/${row.currentSubmissionId}`);
            }
          }}
        />
      </div>

      <div className="grid grid-cols-1 gap-3 md:hidden">
        {items.map((item) => (
          <Card key={item.taskId}>
            <CardContent className="space-y-3 p-4">
              <div className="flex items-start justify-between gap-2">
                <div>
                  <p className="font-semibold text-foreground">
                    {item.subjectName} ·{' '}
                    {item.taskType === 'REVIEW' ? 'Ôn bài cũ' : 'Soạn bài mới'}
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
                      {item.customDriveFolderUrl ? 'PH giao (Drive)' : 'PH giao'}
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
                  {item.customSubmissionNote || item.customSubmissionUrl ? (
                    <p className="break-words text-sky-900 [overflow-wrap:anywhere] dark:text-sky-200">
                      {item.customSubmissionNote ? (
                        <>Con nộp: &ldquo;{item.customSubmissionNote}&rdquo;</>
                      ) : null}
                      {item.customSubmissionUrl ? (
                        <>
                          {item.customSubmissionNote ? ' · ' : null}
                          <a
                            href={item.customSubmissionUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="inline-flex items-center gap-0.5 font-semibold underline"
                          >
                            Link bài
                            <ExternalLink className="h-3 w-3" />
                          </a>
                        </>
                      ) : null}
                    </p>
                  ) : null}
                </div>
              ) : null}

              {item.skipRequestReason &&
              (item.skipRequestStatus === 'PENDING' ||
                item.skipRequestStatus === 'APPROVED') ? (
                <p className="break-words rounded-md border border-amber-200 bg-amber-50/70 px-2.5 py-1.5 text-xs text-amber-900 [overflow-wrap:anywhere] dark:border-amber-900/60 dark:bg-amber-950/30 dark:text-amber-200">
                  Lý do con xin bỏ qua: &ldquo;{item.skipRequestReason}&rdquo;
                </p>
              ) : null}

              {item.evaluation || item.guardianAcknowledgedAt ? (
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

              <GuardianTaskActions row={item} variant="mobile" {...actions} />
            </CardContent>
          </Card>
        ))}
      </div>
    </>
  );
}
