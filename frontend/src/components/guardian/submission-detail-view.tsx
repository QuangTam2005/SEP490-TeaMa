'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  ArrowLeft,
  BookOpenCheck,
  CheckCircle2,
  Compass,
  ExternalLink,
  HeartHandshake,
  History,
  Loader2,
  Send,
  Sparkles,
} from 'lucide-react';
import { toast } from 'sonner';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { QueryStateBoundary } from '@/components/shared/query-state-boundary';
import { StatusBadge } from '@/components/shared/status-badge';
import { extractApiErrorMessage } from '@/components/shared/form-error-utils';
import { MAX_FEEDBACK_CHARS, formatViDate } from '@/lib/study-utils';
import { cn } from '@/lib/utils';
import { useMockRuntime } from '@/mocks/mock-runtime';
import { guardianService } from '@/services';

interface SubmissionDetailViewProps {
  submissionId: number;
}

export function SubmissionDetailView({
  submissionId,
}: SubmissionDetailViewProps) {
  const queryClient = useQueryClient();
  const runtime = useMockRuntime();
  const [selectedVersionId, setSelectedVersionId] = useState<number>(
    submissionId
  );
  const [noteInput, setNoteInput] = useState('');

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
    if (detailQuery.data) {
      setNoteInput(detailQuery.data.guardianNote ?? '');
    }
  }, [detailQuery.data, submissionId]);

  const acknowledgeMutation = useMutation({
    mutationFn: (taskId: number) =>
      guardianService.acknowledgeTask(taskId, { note: noteInput.trim() || null }),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ['guardian-submission', submissionId],
      });
      void queryClient.invalidateQueries({ queryKey: ['guardian-dashboard'] });
      void queryClient.invalidateQueries({ queryKey: ['guardian-weekly'] });
      toast.success('Đã xác nhận xem bài và gửi lời nhắn động viên cho con');
    },
    onError: (err) => {
      toast.error(extractApiErrorMessage(err));
    },
  });

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
                    <StatusBadge status={detail.status} />
                  </div>
                  <h1 className="break-words text-xl font-bold tracking-tight text-foreground [overflow-wrap:anywhere] sm:text-2xl">
                    {activeVersion?.lessonTitle ?? 'Bài làm của học sinh'}
                  </h1>
                  <p className="text-xs text-muted-foreground">
                    Học sinh: <strong>{detail.studentName}</strong> · Ngày học:{' '}
                    {formatViDate(detail.targetDate)} · Phiên bản:{' '}
                    {detail.versions.length}/3
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
                <div className="grid grid-cols-1 gap-6 lg:grid-cols-12 xl:gap-8">
                  {/* Immutable student submission */}
                  <Card className="min-w-0 overflow-hidden lg:col-span-7 2xl:col-span-8">
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
                      <p className="break-words text-sm font-semibold text-foreground [overflow-wrap:anywhere]">
                        Tên bài: {activeVersion.lessonTitle}
                      </p>
                      <div className="break-words whitespace-pre-wrap rounded-lg border bg-muted/25 p-4 text-sm leading-relaxed text-foreground [overflow-wrap:anywhere]">
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
                  <Card className="min-w-0 overflow-hidden border-brand/25 lg:sticky lg:top-24 lg:col-span-5 lg:self-start 2xl:col-span-4">
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

                          <div className="break-words whitespace-pre-wrap rounded-lg border border-brand/20 bg-brand-soft/35 p-3.5 text-sm leading-relaxed text-foreground [overflow-wrap:anywhere] dark:bg-sky-950/25">
                            {activeVersion.evaluation.feedback.slice(
                              0,
                              MAX_FEEDBACK_CHARS
                            )}
                          </div>

                          {activeVersion.evaluation.incorrectPoints.length >
                          0 ? (
                            <div className="space-y-1.5">
                              <p className="text-xs font-bold uppercase tracking-wide text-rose-600 dark:text-rose-400">
                                Điểm sai / chưa chính xác
                              </p>
                              <ul className="list-disc space-y-1 pl-4 text-sm text-foreground [overflow-wrap:anywhere]">
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
                              <ul className="list-disc space-y-1 pl-4 text-sm text-foreground [overflow-wrap:anywhere]">
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
                                      <span className="min-w-0 break-words text-xs font-semibold text-brand-strong [overflow-wrap:anywhere] dark:text-sky-300">
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

              {/* Parent Acknowledge & Encouragement Note Card */}
              <Card className="border-emerald-200/80 bg-emerald-50/30 dark:border-emerald-900/60 dark:bg-emerald-950/15">
                <CardHeader className="pb-3">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <CardTitle className="flex items-center gap-2 text-base">
                      <HeartHandshake
                        className="h-5 w-5 text-emerald-600 dark:text-emerald-400"
                        strokeWidth={1.75}
                      />
                      Xác nhận của Phụ huynh &amp; Lời nhắn động viên
                    </CardTitle>
                    {detail.guardianAcknowledgedAt ? (
                      <Badge variant="success" className="gap-1">
                        <CheckCircle2 className="h-3.5 w-3.5" />
                        Đã xem lúc{' '}
                        {new Date(detail.guardianAcknowledgedAt).toLocaleString(
                          'vi-VN'
                        )}
                      </Badge>
                    ) : null}
                  </div>
                </CardHeader>
                <CardContent className="flex flex-col gap-3 sm:flex-row sm:items-center">
                  <Input
                    value={noteInput}
                    onChange={(e) => setNoteInput(e.target.value)}
                    maxLength={300}
                    placeholder="Viết lời khen hoặc dặn dò nhẹ nhàng gửi tới con (không bắt buộc)..."
                    className="h-11 flex-1 bg-background"
                  />
                  <Button
                    type="button"
                    className="h-11 gap-2 px-5 font-semibold"
                    disabled={
                      !detail.taskId || acknowledgeMutation.isPending
                    }
                    onClick={() => {
                      if (detail.taskId) {
                        acknowledgeMutation.mutate(detail.taskId);
                      }
                    }}
                  >
                    {acknowledgeMutation.isPending ? (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                      <Send className="h-4 w-4" strokeWidth={1.75} />
                    )}
                    {detail.guardianAcknowledgedAt
                      ? 'Cập nhật xác nhận'
                      : 'Bấm Đã xem & Gửi'}
                  </Button>
                </CardContent>
              </Card>
            </div>
          );
        }}
      </QueryStateBoundary>
    </div>
  );
}
