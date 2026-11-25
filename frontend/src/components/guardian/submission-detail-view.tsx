'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import {
  ArrowLeft,
  BookOpenCheck,
  Compass,
  ExternalLink,
  History,
  Lock,
  Sparkles,
} from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { QueryStateBoundary } from '@/components/shared/query-state-boundary';
import { StatusBadge } from '@/components/shared/status-badge';
import { formatViDate } from '@/lib/study-utils';
import { cn } from '@/lib/utils';
import { useMockRuntime } from '@/mocks/mock-runtime';
import { guardianService } from '@/services';

interface SubmissionDetailViewProps {
  submissionId: number;
}

export function SubmissionDetailView({
  submissionId,
}: SubmissionDetailViewProps) {
  const runtime = useMockRuntime();
  const [selectedVersionId, setSelectedVersionId] = useState<number>(
    submissionId
  );

  const detailQuery = useQuery({
    queryKey: [
      'guardian-submission',
      submissionId,
      runtime.resetCounter,
      runtime.scenario,
      runtime.simulatedRole,
    ],
    queryFn: () => guardianService.getSubmissionDetail(submissionId),
  });

  useEffect(() => {
    if (detailQuery.data?.versions.length) {
      const hasTarget = detailQuery.data.versions.some(
        (v) => v.id === submissionId
      );
      setSelectedVersionId(
        hasTarget ? submissionId : detailQuery.data.versions[0].id
      );
    }
  }, [detailQuery.data, submissionId]);

  return (
    <div className="space-y-5">
      <div>
        <Link
          href="/guardian/dashboard"
          className="inline-flex h-10 items-center gap-2 rounded-lg border bg-card px-3.5 text-sm font-medium text-foreground transition-colors hover:bg-muted"
        >
          <ArrowLeft className="h-4 w-4" strokeWidth={1.75} />
          Quay lại Bảng theo dõi
        </Link>
      </div>

      <QueryStateBoundary
        data={detailQuery.data}
        isLoading={detailQuery.isLoading}
        isError={detailQuery.isError}
        error={detailQuery.error}
        onRetry={() => void detailQuery.refetch()}
      >
        {(detail) => {
          const activeVersion =
            detail.versions.find((v) => v.id === selectedVersionId) ??
            detail.versions[0];

          return (
            <div className="space-y-6">
              <div className="flex flex-col gap-3 rounded-xl border bg-card p-5 sm:flex-row sm:items-center sm:justify-between">
                <div className="space-y-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <Badge variant="brand">Môn {detail.subjectName}</Badge>
                    <Badge variant="outline">
                      {detail.taskType === 'REVIEW' ? (
                        <span className="inline-flex items-center gap-1">
                          <BookOpenCheck className="h-3.5 w-3.5" />
                          Ôn bài cũ
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1">
                          <Compass className="h-3.5 w-3.5" />
                          Soạn bài mới
                        </span>
                      )}
                    </Badge>
                    <Badge variant="secondary">
                      <Lock className="h-3 w-3" strokeWidth={1.75} />
                      Bản gốc chỉ xem
                    </Badge>
                    <StatusBadge status={detail.status} />
                  </div>
                  <h1 className="text-xl font-bold tracking-tight text-foreground sm:text-2xl">
                    {activeVersion?.lessonTitle ?? 'Bài làm của học sinh'}
                  </h1>
                  <p className="text-xs text-muted-foreground">
                    Học sinh: <strong>{detail.studentName}</strong> · Ngày học:{' '}
                    {formatViDate(detail.targetDate)} · Đã sửa:{' '}
                    {detail.revisionCount}/3 lần
                  </p>
                </div>
              </div>

              {detail.versions.length > 0 ? (
                <div className="flex flex-wrap items-center gap-2 rounded-xl border bg-card p-3">
                  <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-muted-foreground">
                    <History className="h-4 w-4" strokeWidth={1.75} />
                    Lịch sử phiên bản:
                  </span>
                  {detail.versions.map((ver) => (
                    <button
                      key={ver.id}
                      type="button"
                      onClick={() => setSelectedVersionId(ver.id)}
                      className={cn(
                        'h-9 rounded-lg border px-3 text-xs font-semibold transition-colors',
                        activeVersion?.id === ver.id
                          ? 'border-brand-strong bg-brand-soft text-brand-strong dark:text-sky-200'
                          : 'bg-background text-muted-foreground hover:text-foreground'
                      )}
                    >
                      Bản {ver.version} ({ver.charCount} ký tự)
                      {ver.isCurrent ? ' · Mới nhất' : ''}
                    </button>
                  ))}
                </div>
              ) : null}

              {activeVersion ? (
                <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
                  {/* Immutable student submission */}
                  <Card className="lg:col-span-7">
                    <CardHeader className="border-b pb-3.5">
                      <div className="flex flex-wrap items-center justify-between gap-2">
                        <CardTitle className="text-base">
                          Bài viết của học sinh (Bản {activeVersion.version})
                        </CardTitle>
                        <span className="font-mono text-xs text-muted-foreground">
                          {activeVersion.charCount} ký tự
                        </span>
                      </div>
                    </CardHeader>
                    <CardContent className="space-y-3 pt-4">
                      <p className="text-sm font-semibold text-foreground">
                        Tên bài: {activeVersion.lessonTitle}
                      </p>
                      <div className="whitespace-pre-wrap rounded-lg border bg-muted/25 p-4 text-sm leading-relaxed text-foreground">
                        {activeVersion.content}
                      </div>
                      <p className="text-xs text-muted-foreground">
                        Thời điểm nộp:{' '}
                        {new Date(activeVersion.submittedAt).toLocaleString(
                          'vi-VN'
                        )}
                      </p>
                    </CardContent>
                  </Card>

                  {/* Read-only AI Evaluation + Sources (Zero cost/token leak) */}
                  <Card className="border-brand/25 lg:col-span-5">
                    <CardHeader className="border-b bg-brand-soft/35 pb-3.5 dark:bg-sky-950/30">
                      <div className="flex flex-wrap items-center justify-between gap-2">
                        <CardTitle className="flex items-center gap-2 text-base">
                          <Sparkles
                            className="h-4 w-4 text-brand-strong dark:text-sky-400"
                            strokeWidth={1.75}
                          />
                          Đánh giá AI
                        </CardTitle>
                        {activeVersion.evaluation ? (
                          <StatusBadge
                            status={activeVersion.evaluation.overallStatus}
                          />
                        ) : null}
                      </div>
                    </CardHeader>

                    <CardContent className="space-y-4 pt-4">
                      {activeVersion.evaluation ? (
                        <>
                          <div className="flex flex-wrap gap-2">
                            <StatusBadge
                              status={activeVersion.evaluation.accuracy}
                            />
                            <StatusBadge
                              status={activeVersion.evaluation.completeness}
                            />
                            <StatusBadge
                              status={activeVersion.evaluation.confidence}
                            />
                          </div>

                          <div className="rounded-lg border border-brand/20 bg-brand-soft/35 p-3.5 text-sm leading-relaxed text-foreground dark:bg-sky-950/25">
                            {activeVersion.evaluation.feedback}
                          </div>

                          {activeVersion.evaluation.incorrectPoints.length >
                          0 ? (
                            <div className="space-y-1.5">
                              <p className="text-xs font-bold uppercase tracking-wide text-rose-600 dark:text-rose-400">
                                Điểm sai / chưa chính xác
                              </p>
                              <ul className="list-disc space-y-1 pl-4 text-sm text-foreground">
                                {activeVersion.evaluation.incorrectPoints.map(
                                  (point, idx) => (
                                    <li key={idx}>{point}</li>
                                  )
                                )}
                              </ul>
                            </div>
                          ) : null}

                          {activeVersion.evaluation.missingPoints.length > 0 ? (
                            <div className="space-y-1.5">
                              <p className="text-xs font-bold uppercase tracking-wide text-amber-700 dark:text-amber-400">
                                Điểm còn thiếu
                              </p>
                              <ul className="list-disc space-y-1 pl-4 text-sm text-foreground">
                                {activeVersion.evaluation.missingPoints.map(
                                  (point, idx) => (
                                    <li key={idx}>{point}</li>
                                  )
                                )}
                              </ul>
                            </div>
                          ) : null}

                          <div className="space-y-2 border-t pt-3">
                            <p className="text-xs font-bold uppercase tracking-wide text-muted-foreground">
                              Nguồn đối chiếu (
                              {activeVersion.evaluation.sources.length})
                            </p>
                            {activeVersion.evaluation.sources.length === 0 ? (
                              <p className="text-xs text-muted-foreground">
                                Không có nguồn tham khảo được đính kèm.
                              </p>
                            ) : (
                              <div className="space-y-2">
                                {activeVersion.evaluation.sources.map((src) => (
                                  <a
                                    key={src.id}
                                    href={src.url}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    className="block rounded-lg border bg-background p-2.5 transition-colors hover:border-brand hover:bg-brand-soft/30"
                                  >
                                    <div className="flex items-start justify-between gap-2">
                                      <span className="text-xs font-semibold text-brand-strong dark:text-sky-300">
                                        {src.title}
                                      </span>
                                      <ExternalLink className="mt-0.5 h-3.5 w-3.5 shrink-0 text-muted-foreground" />
                                    </div>
                                    <p className="mt-1 text-[11px] text-muted-foreground">
                                      Truy xuất:{' '}
                                      {new Date(src.retrievedAt).toLocaleString(
                                        'vi-VN'
                                      )}
                                    </p>
                                  </a>
                                ))}
                              </div>
                            )}
                          </div>
                        </>
                      ) : (
                        <p className="text-sm text-muted-foreground">
                          {activeVersion.lastError ??
                            'Phiên bản này chưa hoàn tất đánh giá AI.'}
                        </p>
                      )}
                    </CardContent>
                  </Card>
                </div>
              ) : null}
            </div>
          );
        }}
      </QueryStateBoundary>
    </div>
  );
}
