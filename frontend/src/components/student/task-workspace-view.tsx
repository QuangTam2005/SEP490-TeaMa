'use client';

import { useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  AlertTriangle,
  ArrowLeft,
  BookOpenCheck,
  CheckCircle2,
  Compass,
  ExternalLink,
  History,
  Loader2,
  Lock,
  PencilLine,
  RefreshCw,
  Send,
  ShieldAlert,
  SkipForward,
  Sparkles,
} from 'lucide-react';
import { toast } from 'sonner';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { QueryStateBoundary } from '@/components/shared/query-state-boundary';
import { StatusBadge } from '@/components/shared/status-badge';
import { extractApiErrorMessage } from '@/components/shared/form-error-utils';
import { SkipRequestDialog } from '@/components/student/skip-request-dialog';
import {
  MAX_FEEDBACK_CHARS,
  MAX_POST_EVAL_REVISIONS,
  MAX_SUBMISSION_CHARS,
  MIN_SUBMISSION_CHARS,
  countNormalizedChars,
  formatViDate,
  isValidStudyLength,
} from '@/lib/study-utils';
import {
  ANTI_PASTE_NOTICE,
  isBlockedBeforeInputType,
  isSuddenBulkInsert,
} from '@/lib/anti-paste';
import { cn } from '@/lib/utils';
import { useMockRuntime } from '@/mocks/mock-runtime';
import { IS_MOCK_MODE, studyService } from '@/services';
import { useAuthStore } from '@/store/auth-store';
import type { StudySubmissionResponse, StudyTaskSummaryResponse } from '@/types';

interface TaskWorkspaceViewProps {
  taskId: number;
}

function createIdempotencyKey(taskId: number, versionHint: number): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return `sub-${taskId}-v${versionHint}-${crypto.randomUUID()}`;
  }
  return `sub-${taskId}-v${versionHint}-${Date.now()}-${Math.random()
    .toString(36)
    .slice(2, 10)}`;
}

function EvaluationPanel({
  submission,
  isEvaluating,
  onRetryEvaluation,
  isRetrying,
}: {
  submission: StudySubmissionResponse | null;
  isEvaluating: boolean;
  onRetryEvaluation: (submissionId: number) => void;
  isRetrying: boolean;
}) {
  if (isEvaluating) {
    return (
      <Card className="border-brand/30 bg-brand-soft/40 dark:bg-sky-950/30">
        <CardContent className="flex flex-col items-center justify-center gap-3 py-10 text-center">
          <div className="flex h-12 w-12 items-center justify-center rounded-full bg-brand-strong text-white">
            <Loader2 className="h-6 w-6 animate-spin" strokeWidth={1.75} />
          </div>
          <p className="text-base font-semibold text-foreground">
            AI đang đọc bài và đối chiếu kiến thức SGK...
          </p>
          <p className="max-w-xs text-xs text-muted-foreground">
            Quá trình này mất vài giây. Con giữ nguyên trang để xem nhận xét nhé.
          </p>
        </CardContent>
      </Card>
    );
  }

  if (!submission) {
    return (
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="flex items-center gap-2 text-base">
            <Sparkles
              className="h-4 w-4 text-brand-strong dark:text-sky-400"
              strokeWidth={1.75}
            />
            Nhận xét từ AI Đồng hành
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3 text-sm text-muted-foreground">
          <p>
            Sau khi con viết đủ từ{' '}
            <strong className="text-foreground">
              {MIN_SUBMISSION_CHARS} đến {MAX_SUBMISSION_CHARS}
            </strong>{' '}
            ký tự và bấm nộp bài, AI sẽ kiểm tra độ đúng và đủ ý giúp con.
          </p>
          <div className="rounded-lg border border-brand/20 bg-brand-soft/40 p-3 text-xs text-brand-strong dark:text-sky-300">
            Mẹo nhỏ: Hãy viết bằng chính lời hiểu của con theo các ý 1, 2, 3 để AI nhận xét sát nhất.
          </div>
        </CardContent>
      </Card>
    );
  }

  if (submission.lastError && !submission.evaluation) {
    return (
      <Card className="border-rose-200 bg-rose-50/50 dark:border-rose-900 dark:bg-rose-950/30">
        <CardHeader className="pb-2">
          <CardTitle className="flex items-center gap-2 text-base text-rose-700 dark:text-rose-300">
            <AlertTriangle className="h-4 w-4" strokeWidth={1.75} />
            Chưa đánh giá được bài nộp
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <p className="text-sm text-rose-700 dark:text-rose-300">
            {submission.lastError}
          </p>
          <Button
            type="button"
            className="h-11 w-full gap-2"
            disabled={isRetrying}
            onClick={() => onRetryEvaluation(submission.id)}
          >
            {isRetrying ? (
              <Loader2 className="h-4 w-4 animate-spin" strokeWidth={1.75} />
            ) : (
              <RefreshCw className="h-4 w-4" strokeWidth={1.75} />
            )}
            Thử đánh giá lại
          </Button>
        </CardContent>
      </Card>
    );
  }

  const evaluation = submission.evaluation;
  if (!evaluation) {
    return null;
  }

  const clampedFeedback = evaluation.feedback.slice(0, MAX_FEEDBACK_CHARS);

  return (
    <Card className="border-brand/25">
      <CardHeader className="border-b bg-brand-soft/35 pb-3.5 dark:bg-sky-950/30">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <CardTitle className="flex items-center gap-2 text-base">
            <Sparkles
              className="h-4 w-4 text-brand-strong dark:text-sky-400"
              strokeWidth={1.75}
            />
            Nhận xét bài (Bản {submission.version})
          </CardTitle>
          <StatusBadge status={evaluation.overallStatus} />
        </div>
      </CardHeader>

      <CardContent className="space-y-4 pt-4">
        <div className="flex flex-wrap gap-2">
          <StatusBadge status={evaluation.accuracy} />
          <StatusBadge status={evaluation.completeness} />
          <StatusBadge status={evaluation.confidence} />
        </div>

        <div className="break-words whitespace-pre-wrap rounded-lg border border-brand/20 bg-brand-soft/35 p-3.5 text-sm leading-relaxed text-foreground [overflow-wrap:anywhere] dark:bg-sky-950/25">
          {clampedFeedback}
        </div>

        {evaluation.incorrectPoints.length > 0 ? (
          <div className="space-y-1.5">
            <p className="text-xs font-bold uppercase tracking-wide text-rose-600 dark:text-rose-400">
              Điểm cần chỉnh lại
            </p>
            <ul className="list-disc space-y-1 pl-4 text-sm text-foreground [overflow-wrap:anywhere]">
              {evaluation.incorrectPoints.map((point, idx) => (
                <li key={idx}>{point}</li>
              ))}
            </ul>
          </div>
        ) : null}

        {evaluation.missingPoints.length > 0 ? (
          <div className="space-y-1.5">
            <p className="text-xs font-bold uppercase tracking-wide text-amber-700 dark:text-amber-400">
              Ý nên bổ sung thêm
            </p>
            <ul className="list-disc space-y-1 pl-4 text-sm text-foreground [overflow-wrap:anywhere]">
              {evaluation.missingPoints.map((point, idx) => (
                <li key={idx}>{point}</li>
              ))}
            </ul>
          </div>
        ) : null}

        <div className="space-y-2 border-t pt-3">
          <p className="text-xs font-bold uppercase tracking-wide text-muted-foreground">
            Nguồn đối chiếu ({evaluation.sources.length})
          </p>
          {evaluation.sources.length === 0 ? (
            <p className="text-xs text-muted-foreground">
              Chưa tìm thấy nguồn tham chiếu phù hợp để kiểm chứng chi tiết.
            </p>
          ) : (
            <div className="space-y-2">
              {evaluation.sources.map((src) => (
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
                  {src.snippet ? (
                    <p className="mt-1 line-clamp-2 text-xs text-muted-foreground">
                      {src.snippet}
                    </p>
                  ) : null}
                </a>
              ))}
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
}

export function TaskWorkspaceView({ taskId }: TaskWorkspaceViewProps) {
  const queryClient = useQueryClient();
  const runtime = useMockRuntime();
  const userId = useAuthStore((state) => state.user?.id);
  const canUseDraftKey = IS_MOCK_MODE || Boolean(userId);
  const draftKey = `asc_draft_${userId ?? 'guest'}_${taskId}`;
  const initializedTaskRef = useRef<number | null>(null);
  const restoredDraftKeyRef = useRef<string | null>(null);
  const lastPasteToastRef = useRef(0);
  const pasteBannerTimerRef = useRef<number | null>(null);

  const [lessonTitle, setLessonTitle] = useState('');
  const [content, setContent] = useState('');
  const [pasteBlockedNotice, setPasteBlockedNotice] = useState(false);
  const [draftSavedAt, setDraftSavedAt] = useState<string | null>(null);
  const [isComposing, setIsComposing] = useState(true);
  const [selectedVersionId, setSelectedVersionId] = useState<number | null>(null);
  const [skipTarget, setSkipTarget] = useState<StudyTaskSummaryResponse | null>(
    null
  );
  const [pendingKey, setPendingKey] = useState<string>(() =>
    createIdempotencyKey(taskId, 1)
  );

  const taskQuery = useQuery({
    queryKey: ['student-task', taskId, runtime.resetCounter, runtime.scenario, runtime.simulatedRole],
    queryFn: () => studyService.getTaskDetail(taskId),
    refetchInterval: (query) =>
      query.state.data?.task.status === 'EVALUATING' ? 1500 : false,
  });

  useEffect(() => {
    const detail = taskQuery.data;
    if (!detail) {
      return;
    }

    const isFirstTaskInit = initializedTaskRef.current !== taskId;
    if (detail.submissions.length === 0) {
      if (isFirstTaskInit) {
        initializedTaskRef.current = taskId;
        setIsComposing(true);
        setSelectedVersionId(null);
      }
      if (
        canUseDraftKey &&
        restoredDraftKeyRef.current !== draftKey &&
        typeof window !== 'undefined'
      ) {
        restoredDraftKeyRef.current = draftKey;
        try {
          const raw = window.localStorage.getItem(draftKey);
          if (raw) {
            const parsed = JSON.parse(raw) as {
              lessonTitle?: string;
              content?: string;
              savedAt?: string;
            };
            if (parsed.lessonTitle || parsed.content) {
              setLessonTitle(parsed.lessonTitle ?? '');
              setContent(parsed.content ?? '');
              setDraftSavedAt(parsed.savedAt ?? null);
            }
          }
        } catch {
          // Ignore corrupted draft payload
        }
      }
      return;
    }

    const latest = detail.submissions[0];
    setSelectedVersionId((prev) => prev ?? latest.id);
    if (detail.task.status === 'EVALUATING') {
      setIsComposing(false);
    } else if (
      isFirstTaskInit &&
      (detail.task.status === 'EVALUATED' ||
        detail.task.status === 'EVALUATION_FAILED')
    ) {
      setIsComposing(false);
    }
    if (isFirstTaskInit) {
      initializedTaskRef.current = taskId;
    }
  }, [taskId, taskQuery.data, draftKey, canUseDraftKey]);

  // Browser-only debounced auto-save draft (never calls AI while typing)
  useEffect(() => {
    if (!canUseDraftKey || !isComposing || typeof window === 'undefined') {
      return;
    }
    if (!lessonTitle.trim() && !content.trim()) {
      return;
    }
    const baseVersion = taskQuery.data?.submissions[0]?.version ?? 0;
    const timer = window.setTimeout(() => {
      try {
        const nowTime = new Date().toLocaleTimeString('vi-VN', {
          hour: '2-digit',
          minute: '2-digit',
        });
        window.localStorage.setItem(
          draftKey,
          JSON.stringify({ lessonTitle, content, savedAt: nowTime, baseVersion })
        );
        setDraftSavedAt(nowTime);
      } catch {
        // Ignore storage quota errors
      }
    }, 400);
    return () => window.clearTimeout(timer);
  }, [draftKey, canUseDraftKey, isComposing, lessonTitle, content, taskQuery.data]);

  const clearSavedDraft = () => {
    if (typeof window !== 'undefined') {
      window.localStorage.removeItem(draftKey);
    }
    setDraftSavedAt(null);
  };

  useEffect(() => {
    return () => {
      if (pasteBannerTimerRef.current !== null && typeof window !== 'undefined') {
        window.clearTimeout(pasteBannerTimerRef.current);
      }
    };
  }, []);

  const notifyPasteBlocked = () => {
    setPasteBlockedNotice(true);
    if (typeof window !== 'undefined') {
      if (pasteBannerTimerRef.current !== null) {
        window.clearTimeout(pasteBannerTimerRef.current);
      }
      pasteBannerTimerRef.current = window.setTimeout(() => {
        setPasteBlockedNotice(false);
      }, 4500);
    }
    const now = Date.now();
    if (now - lastPasteToastRef.current >= 2500) {
      lastPasteToastRef.current = now;
      toast.warning(ANTI_PASTE_NOTICE);
    }
  };

  const submitMutation = useMutation({
    mutationFn: () =>
      studyService.submitTask(taskId, {
        lessonTitle,
        content,
        idempotencyKey: pendingKey,
      }),
    onSuccess: (created) => {
      clearSavedDraft();
      setIsComposing(false);
      setSelectedVersionId(created.id);
      setPendingKey(createIdempotencyKey(taskId, created.version + 1));
      void queryClient.invalidateQueries({ queryKey: ['student-task', taskId] });
      void queryClient.invalidateQueries({ queryKey: ['next-day-plan'] });
      void queryClient.invalidateQueries({ queryKey: ['student-history'] });
      void queryClient.invalidateQueries({ queryKey: ['student-streak'] });
      toast.success('Đã nộp bài! AI đang xem bài cho con nhé.');
    },
    onError: (err) => {
      toast.error(extractApiErrorMessage(err));
    },
  });

  const retryMutation = useMutation({
    mutationFn: (submissionId: number) =>
      studyService.retryEvaluation(submissionId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['student-task', taskId] });
      void queryClient.invalidateQueries({ queryKey: ['next-day-plan'] });
      toast.info('Đang thử đánh giá lại...');
    },
    onError: (err) => {
      toast.error(extractApiErrorMessage(err));
    },
  });

  const requestSkipMutation = useMutation({
    mutationFn: ({ targetTaskId, reason }: { targetTaskId: number; reason: string }) =>
      studyService.requestSkip(targetTaskId, { reason }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['student-task', taskId] });
      void queryClient.invalidateQueries({ queryKey: ['next-day-plan'] });
      void queryClient.invalidateQueries({ queryKey: ['guardian-dashboard'] });
      setSkipTarget(null);
      toast.success('Đã gửi yêu cầu xin bỏ qua tới phụ huynh');
    },
    onError: (err) => {
      toast.error(extractApiErrorMessage(err));
    },
  });

  const cancelSkipMutation = useMutation({
    mutationFn: (targetTaskId: number) =>
      studyService.cancelSkipRequest(targetTaskId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['student-task', taskId] });
      void queryClient.invalidateQueries({ queryKey: ['next-day-plan'] });
      void queryClient.invalidateQueries({ queryKey: ['guardian-dashboard'] });
      toast.success('Đã hủy yêu cầu xin bỏ qua');
    },
    onError: (err) => {
      toast.error(extractApiErrorMessage(err));
    },
  });

  const normalizedCount = countNormalizedChars(content);
  const lengthValid = isValidStudyLength(content);
  const progressPct = Math.min(
    100,
    Math.round((normalizedCount / MAX_SUBMISSION_CHARS) * 100)
  );

  const handleStartRevision = (latestSub?: StudySubmissionResponse) => {
    const baseVersion = latestSub?.version ?? 0;
    let restoredRevision = false;
    if (canUseDraftKey && typeof window !== 'undefined') {
      try {
        const raw = window.localStorage.getItem(draftKey);
        if (raw) {
          const parsed = JSON.parse(raw) as {
            lessonTitle?: string;
            content?: string;
            savedAt?: string;
            baseVersion?: number;
          };
          if (
            parsed.baseVersion === baseVersion &&
            (parsed.lessonTitle || parsed.content)
          ) {
            setLessonTitle(parsed.lessonTitle ?? latestSub?.lessonTitle ?? '');
            setContent(parsed.content ?? latestSub?.content ?? '');
            setDraftSavedAt(parsed.savedAt ?? null);
            restoredRevision = true;
          }
        }
      } catch {
        // Ignore malformed revision draft
      }
    }
    if (!restoredRevision) {
      setLessonTitle(latestSub?.lessonTitle ?? '');
      setContent(latestSub?.content ?? '');
    }
    setPendingKey(createIdempotencyKey(taskId, baseVersion + 1));
    setIsComposing(true);
  };

  return (
    <div className="space-y-5">
      <div>
        <Link
          href="/student/today"
          className="inline-flex h-10 items-center gap-2 rounded-lg border bg-card px-3.5 text-sm font-medium text-foreground transition-colors hover:bg-muted"
        >
          <ArrowLeft className="h-4 w-4" strokeWidth={1.75} />
          Quay lại Bài học ngày mai
        </Link>
      </div>

      <QueryStateBoundary
        data={taskQuery.data}
        isLoading={taskQuery.isLoading}
        isError={taskQuery.isError}
        error={taskQuery.error}
        onRetry={() => void taskQuery.refetch()}
      >
        {(detail) => {
          const { task, submissions } = detail;
          const isPreviewLocked =
            task.taskType === 'PREVIEW' && !detail.reviewCompleted;
          const isEvaluating = task.status === 'EVALUATING';
          const canRevise =
            submissions.length > 0 &&
            task.status === 'EVALUATED' &&
            task.revisionCount < MAX_POST_EVAL_REVISIONS;
          const activeSubmission =
            submissions.find((s) => s.id === selectedVersionId) ??
            submissions[0] ??
            null;

          if (task.status === 'SKIPPED') {
            return (
              <Card className="mx-auto max-w-xl">
                <CardContent className="flex flex-col items-center gap-4 py-10 text-center">
                  <div className="flex h-12 w-12 items-center justify-center rounded-full bg-muted text-muted-foreground">
                    <SkipForward className="h-6 w-6" strokeWidth={1.75} />
                  </div>
                  <div className="space-y-1">
                    <h1 className="text-lg font-bold text-foreground">
                      Nhiệm vụ đã được phụ huynh bỏ qua ({task.subjectName})
                    </h1>
                    <p className="text-sm text-muted-foreground">
                      Phụ huynh đã đánh dấu con không cần làm bài{' '}
                      <strong>
                        {task.taskType === 'REVIEW'
                          ? 'Ôn bài cũ'
                          : 'Soạn bài mới'}
                      </strong>{' '}
                      này trong ngày học {formatViDate(detail.targetDate)}.
                    </p>
                    {task.guardianNote ? (
                      <p className="mt-2 rounded-lg border bg-muted/50 px-3 py-2 text-xs text-muted-foreground">
                        Ghi chú phụ huynh: &ldquo;{task.guardianNote}&rdquo;
                      </p>
                    ) : null}
                  </div>
                  <Button asChild className="h-11 gap-2">
                    <Link href="/student/today">Về danh sách bài học</Link>
                  </Button>
                </CardContent>
              </Card>
            );
          }

          if (isPreviewLocked) {
            return (
              <Card className="mx-auto max-w-xl">
                <CardContent className="flex flex-col items-center gap-4 py-10 text-center">
                  <div className="flex h-12 w-12 items-center justify-center rounded-full bg-muted text-muted-foreground">
                    <Lock className="h-6 w-6" strokeWidth={1.75} />
                  </div>
                  <div className="space-y-1">
                    <h1 className="text-lg font-bold text-foreground">
                      Chưa mở khóa Soạn bài mới ({task.subjectName})
                    </h1>
                    <p className="text-sm text-muted-foreground">
                      Con cần nộp bài <strong>Ôn bài cũ</strong> của môn{' '}
                      {task.subjectName} trước nhé.
                    </p>
                  </div>
                  {task.skipRequestStatus === 'PENDING' && task.skipRequestReason ? (
                    <div className="w-full space-y-2 rounded-lg border border-amber-200 bg-amber-50/70 p-3 text-left text-xs text-amber-900 dark:border-amber-900/60 dark:bg-amber-950/30 dark:text-amber-200">
                      <p className="font-semibold">Đang chờ phụ huynh duyệt xin bỏ qua</p>
                      <p className="break-words [overflow-wrap:anywhere]">
                        Lý do: &ldquo;{task.skipRequestReason}&rdquo;
                      </p>
                      <Button
                        type="button"
                        size="sm"
                        variant="outline"
                        disabled={cancelSkipMutation.isPending}
                        onClick={() => cancelSkipMutation.mutate(task.id)}
                        className="h-9 bg-background text-xs"
                      >
                        {cancelSkipMutation.isPending ? 'Đang hủy...' : 'Hủy xin bỏ qua'}
                      </Button>
                    </div>
                  ) : null}
                  {task.skipRequestStatus === 'REJECTED' ? (
                    <div className="w-full rounded-lg border border-rose-200 bg-rose-50/60 p-3 text-left text-xs text-rose-800 dark:border-rose-900/60 dark:bg-rose-950/30 dark:text-rose-200">
                      <p className="font-semibold">Phụ huynh chưa đồng ý bỏ qua</p>
                      {task.guardianNote ? (
                        <p className="mt-1 break-words [overflow-wrap:anywhere]">
                          Lời nhắn: &ldquo;{task.guardianNote}&rdquo;
                        </p>
                      ) : null}
                    </div>
                  ) : null}
                  <div className="flex w-full flex-col gap-2 sm:flex-row sm:justify-center">
                    <Button asChild className="h-11 gap-2">
                      <Link href="/student/today">Về danh sách bài học</Link>
                    </Button>
                    {task.skipRequestStatus !== 'PENDING' ? (
                      <Button
                        type="button"
                        variant="outline"
                        className="h-11"
                        onClick={() => setSkipTarget(task)}
                      >
                        {task.skipRequestStatus === 'REJECTED'
                          ? 'Gửi lại xin bỏ qua'
                          : 'Xin bỏ qua phần Soạn bài mới'}
                      </Button>
                    ) : null}
                  </div>
                </CardContent>
              </Card>
            );
          }

          const canRequestSkipHere =
            submissions.length === 0 &&
            (task.status === 'NOT_STARTED' || task.status === 'IN_PROGRESS');

          return (
            <div className="space-y-6">
              <div className="flex flex-col gap-3 rounded-xl border bg-card p-5 sm:flex-row sm:items-center sm:justify-between">
                <div className="space-y-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <Badge variant="brand">Môn {task.subjectName}</Badge>
                    <Badge variant="outline">
                      {task.taskType === 'REVIEW' ? (
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
                    <StatusBadge status={task.status} />
                    {task.skipRequestStatus === 'PENDING' ? (
                      <Badge variant="warning" className="whitespace-nowrap">
                        Chờ PH duyệt bỏ qua
                      </Badge>
                    ) : null}
                  </div>
                  <h1 className="break-words text-xl font-bold tracking-tight text-foreground [overflow-wrap:anywhere] sm:text-2xl">
                    {activeSubmission?.lessonTitle ??
                      (task.taskType === 'REVIEW'
                        ? `Ôn bài cũ môn ${task.subjectName}`
                        : `Soạn bài mới môn ${task.subjectName}`)}
                  </h1>
                </div>

                <div className="flex flex-wrap items-center gap-2.5 text-xs font-medium text-muted-foreground">
                  <span>Ngày học trên lớp: {formatViDate(detail.targetDate)}</span>
                  <span className="rounded-md border bg-muted/50 px-2.5 py-1 font-semibold text-foreground">
                    Phiên bản: {submissions.length}/3 (đã sửa{' '}
                    {task.revisionCount}/{task.maxRevisions} lần)
                  </span>
                  {canRequestSkipHere && task.skipRequestStatus !== 'PENDING' ? (
                    <Button
                      type="button"
                      size="sm"
                      variant="outline"
                      className="h-9"
                      onClick={() => setSkipTarget(task)}
                    >
                      {task.skipRequestStatus === 'REJECTED'
                        ? 'Gửi lại xin bỏ qua'
                        : 'Xin phụ huynh bỏ qua'}
                    </Button>
                  ) : null}
                </div>
              </div>

              {task.skipRequestStatus === 'PENDING' && task.skipRequestReason ? (
                <div className="flex flex-col gap-2 rounded-xl border border-amber-200 bg-amber-50/70 px-4 py-3 text-sm text-amber-900 dark:border-amber-900/60 dark:bg-amber-950/30 dark:text-amber-200 sm:flex-row sm:items-center sm:justify-between">
                  <div className="min-w-0 space-y-0.5">
                    <p className="font-semibold">Đang chờ phụ huynh duyệt yêu cầu bỏ qua</p>
                    <p className="break-words text-xs [overflow-wrap:anywhere]">
                      Lý do đã gửi: &ldquo;{task.skipRequestReason}&rdquo; (Con vẫn có thể viết và nộp bài nếu đổi ý).
                    </p>
                  </div>
                  <Button
                    type="button"
                    size="sm"
                    variant="outline"
                    disabled={cancelSkipMutation.isPending}
                    onClick={() => cancelSkipMutation.mutate(task.id)}
                    className="h-9 shrink-0 bg-background"
                  >
                    {cancelSkipMutation.isPending ? 'Đang hủy...' : 'Hủy xin bỏ qua'}
                  </Button>
                </div>
              ) : null}

              {task.skipRequestStatus === 'REJECTED' ? (
                <div className="rounded-xl border border-rose-200 bg-rose-50/60 px-4 py-3 text-sm text-rose-900 dark:border-rose-900/60 dark:bg-rose-950/25 dark:text-rose-200">
                  <p className="font-semibold">Phụ huynh chưa đồng ý bỏ qua bài học này</p>
                  {task.guardianNote ? (
                    <p className="mt-0.5 break-words text-xs [overflow-wrap:anywhere]">
                      Lời nhắn của phụ huynh: &ldquo;{task.guardianNote}&rdquo;
                    </p>
                  ) : null}
                </div>
              ) : null}

              {task.guardianAcknowledgedAt ? (
                <div className="rounded-xl border border-emerald-200 bg-emerald-50/60 px-4 py-3 text-sm text-emerald-900 dark:border-emerald-900/60 dark:bg-emerald-950/25 dark:text-emerald-200">
                  <p className="font-semibold">
                    Phụ huynh đã xem bài ({new Date(task.guardianAcknowledgedAt).toLocaleString('vi-VN')})
                  </p>
                  {task.guardianNote ? (
                    <p className="mt-1 text-emerald-800 dark:text-emerald-300">
                      Lời nhắn từ bố mẹ: &ldquo;{task.guardianNote}&rdquo;
                    </p>
                  ) : null}
                </div>
              ) : null}

              <div className="grid grid-cols-1 gap-6 lg:grid-cols-12 xl:gap-8">
                {/* Left / Main editor or immutable submission reader */}
                <div className="min-w-0 space-y-4 lg:col-span-7 2xl:col-span-8">
                  {submissions.length > 0 ? (
                    <div className="flex flex-wrap items-center justify-between gap-2 rounded-xl border bg-card p-3">
                      <div className="flex flex-wrap items-center gap-1.5">
                        <History
                          className="mr-1 h-4 w-4 text-muted-foreground"
                          strokeWidth={1.75}
                        />
                        {submissions.map((sub) => (
                          <button
                            key={sub.id}
                            type="button"
                            onClick={() => {
                              setIsComposing(false);
                              setSelectedVersionId(sub.id);
                            }}
                            className={cn(
                              'h-11 rounded-lg border px-3.5 text-xs font-semibold transition-colors md:h-9 md:px-3',
                              !isComposing && activeSubmission?.id === sub.id
                                ? 'border-brand-strong bg-brand-soft text-brand-strong dark:text-sky-200'
                                : 'bg-background text-muted-foreground hover:text-foreground'
                            )}
                          >
                            Bản {sub.version}
                            {sub.isCurrent ? ' (Mới nhất)' : ''}
                          </button>
                        ))}
                      </div>

                      {canRevise && !isComposing ? (
                        <Button
                          type="button"
                          className="h-10 gap-1.5"
                          onClick={() => handleStartRevision(submissions[0])}
                        >
                          <PencilLine className="h-4 w-4" strokeWidth={1.75} />
                          Viết bản sửa ({task.revisionCount + 1}/
                          {task.maxRevisions})
                        </Button>
                      ) : null}
                    </div>
                  ) : null}

                  {isComposing ? (
                    <Card>
                      <CardHeader className="pb-4">
                        <CardTitle className="text-lg">
                          {submissions.length > 0
                            ? `Viết bản chỉnh sửa (Lần ${task.revisionCount + 1}/${task.maxRevisions})`
                            : 'Viết bài của con'}
                        </CardTitle>
                      </CardHeader>
                      <CardContent className="space-y-4">
                        <div className="space-y-1.5">
                          <Label htmlFor="study-lesson-title">Tên bài học</Label>
                          <Input
                            id="study-lesson-title"
                            value={lessonTitle}
                            onChange={(e) => setLessonTitle(e.target.value)}
                            maxLength={200}
                            placeholder="VD: Bài 11: Ước chung và Ước chung lớn nhất"
                            className="h-11 text-base md:text-sm"
                          />
                        </div>

                        <div className="space-y-2">
                          <div className="flex items-center justify-between gap-2">
                            <Label htmlFor="study-content-editor">
                              Nội dung tóm tắt kiến thức
                            </Label>
                            <span
                              className={cn(
                                'font-mono text-xs font-semibold',
                                lengthValid
                                  ? 'text-emerald-600 dark:text-emerald-400'
                                  : normalizedCount > MAX_SUBMISSION_CHARS
                                    ? 'text-rose-600 dark:text-rose-400'
                                    : 'text-brand-strong dark:text-sky-300'
                              )}
                            >
                              {normalizedCount} / {MAX_SUBMISSION_CHARS} ký tự
                            </span>
                          </div>

                          {pasteBlockedNotice ? (
                            <div
                              role="status"
                              className="flex items-start gap-2 rounded-lg border border-amber-300 bg-amber-50 px-3 py-2 text-xs font-medium text-amber-900 dark:border-amber-800 dark:bg-amber-950/40 dark:text-amber-200"
                            >
                              <ShieldAlert className="mt-0.5 h-4 w-4 shrink-0 text-amber-600 dark:text-amber-400" />
                              <span>{ANTI_PASTE_NOTICE}</span>
                            </div>
                          ) : null}

                          <textarea
                            id="study-content-editor"
                            value={content}
                            onPaste={(e) => {
                              e.preventDefault();
                              notifyPasteBlocked();
                            }}
                            onDrop={(e) => {
                              e.preventDefault();
                              notifyPasteBlocked();
                            }}
                            onContextMenu={(e) => {
                              e.preventDefault();
                              notifyPasteBlocked();
                            }}
                            onKeyDown={(e) => {
                              const key = e.key.toLowerCase();
                              if (
                                ((e.ctrlKey || e.metaKey) && key === 'v') ||
                                (e.shiftKey && e.key === 'Insert')
                              ) {
                                e.preventDefault();
                                notifyPasteBlocked();
                              }
                            }}
                            onBeforeInput={(e) => {
                              const native = e.nativeEvent;
                              const inputType =
                                'inputType' in native &&
                                typeof native.inputType === 'string'
                                  ? native.inputType
                                  : '';
                              if (isBlockedBeforeInputType(inputType)) {
                                e.preventDefault();
                                notifyPasteBlocked();
                              }
                            }}
                            onChange={(e) => {
                              const nextValue = e.target.value;
                              if (isSuddenBulkInsert(content, nextValue)) {
                                notifyPasteBlocked();
                                return;
                              }
                              setContent(nextValue);
                            }}
                            rows={12}
                            placeholder="Con tự gõ các ý chính đã học hoặc tóm tắt bài mới bằng lời hiểu của mình (400–3.000 ký tự, không hỗ trợ dán văn bản)..."
                            className="min-h-[210px] w-full rounded-lg border bg-background p-3.5 text-base leading-relaxed text-foreground placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring sm:min-h-[300px] md:text-sm xl:min-h-[420px]"
                          />

                          <p className="flex items-center gap-1.5 text-[11px] text-muted-foreground">
                            <ShieldAlert className="h-3.5 w-3.5 shrink-0 text-brand-strong dark:text-sky-400" />
                            <span>
                              Chế độ tự học trung thực: vui lòng tự gõ nội dung, hệ thống chặn dán văn bản từ bên ngoài.
                            </span>
                          </p>

                          <div className="space-y-1.5 rounded-lg border bg-muted/35 p-3">
                            <div className="h-2 w-full overflow-hidden rounded-full bg-muted">
                              <div
                                className={cn(
                                  'h-full transition-all',
                                  lengthValid
                                    ? 'bg-emerald-500'
                                    : normalizedCount > MAX_SUBMISSION_CHARS
                                      ? 'bg-rose-500'
                                      : 'bg-brand-strong'
                                )}
                                style={{ width: `${progressPct}%` }}
                              />
                            </div>
                            <p className="text-xs text-muted-foreground">
                              {normalizedCount < MIN_SUBMISSION_CHARS ? (
                                <>
                                  Cần thêm{' '}
                                  <strong className="text-foreground">
                                    {MIN_SUBMISSION_CHARS - normalizedCount}
                                  </strong>{' '}
                                  ký tự nữa để đạt tối thiểu {MIN_SUBMISSION_CHARS}.
                                </>
                              ) : normalizedCount > MAX_SUBMISSION_CHARS ? (
                                <span className="font-semibold text-rose-600 dark:text-rose-400">
                                  Đang vượt quá{' '}
                                  {normalizedCount - MAX_SUBMISSION_CHARS} ký tự
                                  (tối đa {MAX_SUBMISSION_CHARS}).
                                </span>
                              ) : (
                                <span className="inline-flex items-center gap-1 font-semibold text-emerald-700 dark:text-emerald-400">
                                  <CheckCircle2 className="h-3.5 w-3.5" />
                                  Độ dài hợp lệ ({MIN_SUBMISSION_CHARS}–
                                  {MAX_SUBMISSION_CHARS} ký tự). Con có thể nộp bài!
                                </span>
                              )}
                            </p>
                          </div>
                        </div>

                        <div className="flex flex-wrap items-center justify-between gap-2.5 pt-2">
                          <div className="flex items-center gap-2 text-xs text-muted-foreground">
                            {draftSavedAt ? (
                              <>
                                <span>Đã lưu nháp tự động lúc {draftSavedAt}</span>
                                <button
                                  type="button"
                                  onClick={() => {
                                    clearSavedDraft();
                                    setLessonTitle('');
                                    setContent('');
                                  }}
                                  className="font-semibold text-rose-600 hover:underline dark:text-rose-400"
                                >
                                  Xóa nháp
                                </button>
                              </>
                            ) : (
                              <span>Nháp được tự động lưu trên trình duyệt</span>
                            )}
                          </div>

                          <div className="flex w-full flex-col-reverse gap-2 sm:w-auto sm:flex-row sm:items-center sm:gap-2.5">
                            {submissions.length > 0 ? (
                            <Button
                              type="button"
                              variant="outline"
                              className="h-11 w-full sm:w-auto"
                              onClick={() => setIsComposing(false)}
                            >
                              Hủy viết bản mới
                            </Button>
                          ) : null}

                          <Button
                            type="button"
                            className="h-11 w-full gap-2 px-6 text-sm font-semibold sm:w-auto"
                            disabled={
                              !lengthValid ||
                              !lessonTitle.trim() ||
                              isEvaluating ||
                              submitMutation.isPending
                            }
                            onClick={() => submitMutation.mutate()}
                          >
                            {submitMutation.isPending ? (
                              <Loader2
                                className="h-4 w-4 animate-spin"
                                strokeWidth={1.75}
                              />
                            ) : (
                              <Send className="h-4 w-4" strokeWidth={1.75} />
                            )}
                            Nộp bài để AI xem
                          </Button>
                          </div>
                        </div>
                      </CardContent>
                    </Card>
                  ) : activeSubmission ? (
                    <Card>
                      <CardHeader className="border-b pb-3.5">
                        <CardTitle className="text-base">
                          Bài đã nộp (Bản {activeSubmission.version} ·{' '}
                          {activeSubmission.charCount} ký tự)
                        </CardTitle>
                      </CardHeader>
                      <CardContent className="space-y-3 pt-4">
                        <p className="break-words text-sm font-semibold text-foreground [overflow-wrap:anywhere]">
                          Bài: {activeSubmission.lessonTitle}
                        </p>
                        <div className="break-words whitespace-pre-wrap rounded-lg border bg-muted/25 p-4 text-sm leading-relaxed text-foreground [overflow-wrap:anywhere]">
                          {activeSubmission.content}
                        </div>
                        {submissions.length >= 3 ||
                        task.revisionCount >= MAX_POST_EVAL_REVISIONS ? (
                          <p className="text-xs text-muted-foreground">
                            Con đã nộp đủ tối đa 3 phiên bản (v1–v3) cho bài này.
                          </p>
                        ) : null}
                      </CardContent>
                    </Card>
                  ) : null}
                </div>

                {/* Right / AI evaluation panel (prioritized at top on mobile when reading evaluation) */}
                <div
                  className={cn(
                    'min-w-0 lg:sticky lg:top-24 lg:col-span-5 lg:self-start 2xl:col-span-4',
                    !isComposing && 'order-first lg:order-last'
                  )}
                >
                  <EvaluationPanel
                    submission={activeSubmission}
                    isEvaluating={isEvaluating}
                    isRetrying={retryMutation.isPending}
                    onRetryEvaluation={(subId) => retryMutation.mutate(subId)}
                  />
                </div>
              </div>
            </div>
          );
        }}
      </QueryStateBoundary>

      <SkipRequestDialog
        task={skipTarget}
        open={Boolean(skipTarget)}
        onOpenChange={(open) => {
          if (!open) {
            setSkipTarget(null);
          }
        }}
        isSubmitting={requestSkipMutation.isPending}
        onSubmit={(targetTaskId, reason) =>
          requestSkipMutation.mutate({ targetTaskId, reason })
        }
      />
    </div>
  );
}
