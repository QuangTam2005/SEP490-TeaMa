'use client';

import { useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  ArrowLeft,
  BookOpenCheck,
  Compass,
  History,
  Lock,
  PencilLine,
  SkipForward,
} from 'lucide-react';
import { toast } from 'sonner';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { QueryStateBoundary } from '@/components/shared/query-state-boundary';
import { StatusBadge } from '@/components/shared/status-badge';
import { extractApiErrorMessage } from '@/components/shared/form-error-utils';
import { AiEvaluationPanel } from '@/components/student/ai-evaluation-panel';
import { CustomTaskWorkspace } from '@/components/student/custom-task-workspace';
import { SkipRequestDialog } from '@/components/student/skip-request-dialog';
import {
  StudyEssayEditor,
  SubmittedEssayCard,
} from '@/components/student/study-essay-editor';
import { TaskSkipBanners } from '@/components/student/task-skip-banners';
import {
  MAX_POST_EVAL_REVISIONS,
  MAX_SUBMISSION_CHARS,
  countNormalizedChars,
  formatViDate,
  isValidStudyLength,
} from '@/lib/study-utils';
import { ANTI_PASTE_NOTICE } from '@/lib/anti-paste';
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
      void queryClient.invalidateQueries({ queryKey: ['student-history'] });
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
      void queryClient.invalidateQueries({ queryKey: ['student-history'] });
      void queryClient.invalidateQueries({ queryKey: ['guardian-dashboard'] });
      toast.success('Đã hủy yêu cầu xin bỏ qua');
    },
    onError: (err) => {
      toast.error(extractApiErrorMessage(err));
    },
  });

  const submitCustomMutation = useMutation({
    mutationFn: () => studyService.submitCustomTask(taskId, {}),
    onSuccess: (res) => {
      void queryClient.invalidateQueries({ queryKey: ['student-task', taskId] });
      void queryClient.invalidateQueries({ queryKey: ['next-day-plan'] });
      void queryClient.invalidateQueries({ queryKey: ['student-history'] });
      void queryClient.invalidateQueries({ queryKey: ['guardian-dashboard'] });
      void queryClient.invalidateQueries({ queryKey: ['guardian-weekly'] });
      void queryClient.invalidateQueries({ queryKey: ['student-streak'] });
      toast.success(
        res.customDriveFolderUrl
          ? 'Đã gửi xác nhận nộp bài lên Google Drive cho phụ huynh!'
          : 'Đã gửi xác nhận hoàn thành nhiệm vụ cho phụ huynh!'
      );
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
          className="inline-flex h-11 items-center gap-2 rounded-lg border bg-card px-3.5 text-sm font-medium text-foreground transition-colors hover:bg-muted sm:h-10"
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
                        {task.taskType === 'REVIEW' ? 'Ôn bài cũ' : 'Soạn bài mới'}
                      </strong>{' '}
                      này trong ngày học {formatViDate(detail.targetDate)}.
                    </p>
                    {task.guardianNote ? (
                      <p className="mt-2 break-words rounded-lg border bg-muted/50 px-3 py-2 text-xs text-muted-foreground [overflow-wrap:anywhere]">
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
                  <TaskSkipBanners
                    task={task}
                    isCancellingSkip={cancelSkipMutation.isPending}
                    onCancelSkip={(id) => cancelSkipMutation.mutate(id)}
                  />
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
            !task.customSubmittedAt &&
            (task.status === 'NOT_STARTED' || task.status === 'IN_PROGRESS');

          if (task.assignmentMode === 'CUSTOM_DRIVE') {
            return (
              <CustomTaskWorkspace
                task={task}
                targetDate={detail.targetDate}
                canRequestSkipHere={canRequestSkipHere}
                isCancellingSkip={cancelSkipMutation.isPending}
                isSubmitting={submitCustomMutation.isPending}
                onCancelSkip={(id) => cancelSkipMutation.mutate(id)}
                onOpenSkipDialog={setSkipTarget}
                onConfirmComplete={() => submitCustomMutation.mutate()}
              />
            );
          }

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
                      className="h-11 sm:h-9"
                      onClick={() => setSkipTarget(task)}
                    >
                      {task.skipRequestStatus === 'REJECTED'
                        ? 'Gửi lại xin bỏ qua'
                        : 'Xin phụ huynh bỏ qua'}
                    </Button>
                  ) : null}
                </div>
              </div>

              <TaskSkipBanners
                task={task}
                isCancellingSkip={cancelSkipMutation.isPending}
                onCancelSkip={(id) => cancelSkipMutation.mutate(id)}
              />

              {task.guardianAcknowledgedAt ? (
                <div className="rounded-xl border border-emerald-200 bg-emerald-50/60 px-4 py-3 text-sm text-emerald-900 dark:border-emerald-900/60 dark:bg-emerald-950/25 dark:text-emerald-200">
                  <p className="font-semibold">
                    Phụ huynh đã xem bài (
                    {new Date(task.guardianAcknowledgedAt).toLocaleString('vi-VN')})
                  </p>
                  {task.guardianNote ? (
                    <p className="mt-1 break-words text-emerald-800 dark:text-emerald-300 [overflow-wrap:anywhere]">
                      Lời nhắn từ bố mẹ: &ldquo;{task.guardianNote}&rdquo;
                    </p>
                  ) : null}
                </div>
              ) : null}

              <div className="grid grid-cols-1 gap-6 lg:grid-cols-12 xl:gap-8">
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
                          className="h-11 gap-1.5 sm:h-10"
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
                    <StudyEssayEditor
                      submissionsCount={submissions.length}
                      revisionCount={task.revisionCount}
                      maxRevisions={task.maxRevisions}
                      lessonTitle={lessonTitle}
                      content={content}
                      normalizedCount={normalizedCount}
                      lengthValid={lengthValid}
                      progressPct={progressPct}
                      pasteBlockedNotice={pasteBlockedNotice}
                      draftSavedAt={draftSavedAt}
                      isEvaluating={isEvaluating}
                      isSubmitting={submitMutation.isPending}
                      onLessonTitleChange={setLessonTitle}
                      onContentChange={setContent}
                      onNotifyPasteBlocked={notifyPasteBlocked}
                      onClearDraft={() => {
                        clearSavedDraft();
                        setLessonTitle('');
                        setContent('');
                      }}
                      onCancelRevision={() => setIsComposing(false)}
                      onSubmit={() => submitMutation.mutate()}
                    />
                  ) : activeSubmission ? (
                    <SubmittedEssayCard
                      submission={activeSubmission}
                      submissionsCount={submissions.length}
                      revisionCount={task.revisionCount}
                    />
                  ) : null}
                </div>

                <div
                  className={cn(
                    'min-w-0 lg:sticky lg:top-24 lg:col-span-5 lg:self-start 2xl:col-span-4',
                    !isComposing && 'order-first lg:order-last'
                  )}
                >
                  <AiEvaluationPanel
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
