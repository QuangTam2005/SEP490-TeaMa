'use client';

import { useState, type ReactNode } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  ArrowRight,
  Award,
  CheckCircle2,
  Loader2,
  Sparkles,
  XCircle,
} from 'lucide-react';
import { toast } from 'sonner';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { extractApiErrorMessage } from '@/components/shared/form-error-utils';
import { cn } from '@/lib/utils';
import { quizService } from '@/services';
import { useAuthStore } from '@/store/auth-store';
import type {
  QuizOption,
  StudentDailyQuizItemResponse,
  SubmitDailyQuizAnswerResponse,
} from '@/types';

interface DailyQuizGateProps {
  children: ReactNode;
}

interface PendingFeedbackState {
  item: StudentDailyQuizItemResponse;
  result: SubmitDailyQuizAnswerResponse;
}

export function DailyQuizGate({ children }: DailyQuizGateProps) {
  const queryClient = useQueryClient();
  const isHydrated = useAuthStore((state) => state.isHydrated);
  const user = useAuthStore((state) => state.user);
  const isStudent = isHydrated && user?.role === 'STUDENT';

  const [pendingFeedback, setPendingFeedback] =
    useState<PendingFeedbackState | null>(null);
  const [completionSummary, setCompletionSummary] =
    useState<SubmitDailyQuizAnswerResponse | null>(null);

  const quizQuery = useQuery({
    queryKey: ['student-daily-quiz'],
    queryFn: () => quizService.getTodayQuiz(),
    enabled: isStudent,
  });

  const submitAnswerMutation = useMutation({
    mutationFn: ({
      sessionId,
      item,
      selectedOption,
    }: {
      sessionId: number;
      item: StudentDailyQuizItemResponse;
      selectedOption: QuizOption;
    }) =>
      quizService.submitStudentAnswer(sessionId, item.id, { selectedOption }),
    onSuccess: (result, variables) => {
      setPendingFeedback({ item: variables.item, result });
    },
    onError: (err) => {
      toast.error(extractApiErrorMessage(err));
      void queryClient.invalidateQueries({ queryKey: ['student-daily-quiz'] });
    },
  });

  if (!isHydrated) {
    return (
      <div className="mx-auto max-w-2xl py-8">
        <Card>
          <CardContent className="flex items-center justify-center gap-2 py-12 text-sm text-muted-foreground">
            <Loader2 className="h-4 w-4 animate-spin text-brand-strong" />
            <span>Đang chuẩn bị không gian học tập...</span>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (!isStudent) {
    return <>{children}</>;
  }

  if (quizQuery.isLoading) {
    return (
      <div className="mx-auto max-w-2xl py-8">
        <Card>
          <CardContent className="flex items-center justify-center gap-2 py-12 text-sm text-muted-foreground">
            <Loader2 className="h-4 w-4 animate-spin text-brand-strong" />
            <span>Đang kiểm tra trắc nghiệm đầu ngày...</span>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (quizQuery.isError) {
    // Do not lock student out if network/server error occurs on optional/initial quiz check
    return <>{children}</>;
  }

  if (completionSummary) {
    return (
      <div className="mx-auto max-w-xl py-6 sm:py-10">
        <Card className="overflow-hidden border-brand/25 shadow-sm">
          <CardHeader className="border-b bg-brand-soft/45 text-center dark:bg-sky-950/35">
            <div className="mx-auto mb-2 flex h-12 w-12 items-center justify-center rounded-full bg-emerald-100 text-emerald-700 dark:bg-emerald-950/70 dark:text-emerald-300">
              <Award className="h-6 w-6" strokeWidth={1.75} />
            </div>
            <CardTitle className="text-xl font-bold text-foreground">
              Hoàn thành trắc nghiệm đầu ngày
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-5 p-6 text-center">
            <div className="grid grid-cols-2 gap-3">
              <div className="rounded-xl border bg-background p-4">
                <p className="text-xs font-medium text-muted-foreground">
                  Số câu đúng
                </p>
                <p className="mt-1 font-mono text-2xl font-bold text-emerald-700 dark:text-emerald-400">
                  {completionSummary.correctCount}/{completionSummary.totalQuestions}
                </p>
              </div>
              <div className="rounded-xl border bg-background p-4">
                <p className="text-xs font-medium text-muted-foreground">
                  Tỷ lệ chính xác
                </p>
                <p className="mt-1 font-mono text-2xl font-bold text-brand-strong dark:text-sky-300">
                  {completionSummary.accuracyPercent}%
                </p>
              </div>
            </div>

            <Button
              type="button"
              size="lg"
              className="h-12 w-full gap-2 text-base font-semibold"
              onClick={() => {
                setCompletionSummary(null);
                setPendingFeedback(null);
                void queryClient.invalidateQueries({
                  queryKey: ['student-daily-quiz'],
                });
              }}
            >
              <span>Vào bài học hôm nay</span>
              <ArrowRight className="h-4 w-4" strokeWidth={1.75} />
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  const data = quizQuery.data;
  const session = data?.session;

  if (!data?.gateLocked || !session) {
    return <>{children}</>;
  }

  const nextUnanswered =
    session.items.find((item) => item.selectedOption === null) ?? null;
  const activeItem = pendingFeedback?.item ?? nextUnanswered;

  if (!activeItem) {
    return <>{children}</>;
  }

  const feedback = pendingFeedback?.result ?? null;
  const currentAnswered = feedback
    ? feedback.answeredCount
    : session.answeredCount;
  const currentCorrect = feedback ? feedback.correctCount : session.correctCount;
  const progressPercent = Math.round(
    (currentAnswered * 100) / Math.max(1, session.totalQuestions)
  );

  const handleNext = async () => {
    if (!feedback) {
      return;
    }
    if (!feedback.gateLocked || feedback.sessionStatus === 'COMPLETED') {
      setCompletionSummary(feedback);
      setPendingFeedback(null);
      return;
    }
    setPendingFeedback(null);
    await queryClient.invalidateQueries({ queryKey: ['student-daily-quiz'] });
  };

  return (
    <div className="mx-auto max-w-2xl py-4 sm:py-8">
      <Card className="overflow-hidden border-brand/25 shadow-sm">
        <CardHeader className="space-y-3 border-b bg-brand-soft/35 dark:bg-sky-950/30">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div className="flex items-center gap-2">
              <Sparkles
                className="h-5 w-5 text-brand-strong dark:text-sky-400"
                strokeWidth={1.75}
              />
              <CardTitle className="text-lg font-bold text-foreground">
                Trắc nghiệm đầu ngày
              </CardTitle>
            </div>
            <div className="flex items-center gap-2">
              <Badge variant="brand" className="font-mono">
                Câu {activeItem.itemOrder}/{session.totalQuestions}
              </Badge>
              <Badge variant="secondary" className="font-mono">
                Đúng: {currentCorrect}
              </Badge>
            </div>
          </div>

          <div className="h-2 w-full overflow-hidden rounded-full bg-muted">
            <div
              className="h-full rounded-full bg-brand-strong transition-all duration-300 dark:bg-sky-500"
              style={{ width: `${progressPercent}%` }}
            />
          </div>
        </CardHeader>

        <CardContent className="space-y-5 p-5 sm:p-6">
          <div className="space-y-2">
            {activeItem.topicLabel ? (
              <Badge variant="info">{activeItem.topicLabel}</Badge>
            ) : null}
            <h2 className="break-words text-lg font-bold leading-snug text-foreground sm:text-xl [overflow-wrap:anywhere]">
              {activeItem.questionText}
            </h2>
          </div>

          <div className="grid grid-cols-1 gap-3">
            {(['A', 'B'] as const).map((optionKey) => {
              const optionText =
                optionKey === 'A' ? activeItem.optionA : activeItem.optionB;
              const isSelected = feedback?.selectedOption === optionKey;
              const isCorrectOption = feedback?.correctOption === optionKey;

              return (
                <button
                  key={optionKey}
                  type="button"
                  disabled={Boolean(feedback) || submitAnswerMutation.isPending}
                  onClick={() =>
                    submitAnswerMutation.mutate({
                      sessionId: session.sessionId,
                      item: activeItem,
                      selectedOption: optionKey,
                    })
                  }
                  className={cn(
                    'flex min-h-[58px] w-full items-center justify-between gap-3 rounded-xl border p-4 text-left transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
                    !feedback &&
                      'bg-card hover:border-brand-strong hover:bg-brand-soft/40 active:scale-[0.99]',
                    feedback &&
                      isCorrectOption &&
                      'border-emerald-500 bg-emerald-50/80 text-emerald-950 dark:border-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-100',
                    feedback &&
                      isSelected &&
                      !feedback.correct &&
                      'border-rose-500 bg-rose-50/80 text-rose-950 dark:border-rose-700 dark:bg-rose-950/40 dark:text-rose-100',
                    feedback &&
                      !isCorrectOption &&
                      !isSelected &&
                      'opacity-60'
                  )}
                >
                  <div className="flex min-w-0 items-center gap-3">
                    <span
                      className={cn(
                        'flex h-9 w-9 shrink-0 items-center justify-center rounded-lg border font-mono text-sm font-bold',
                        feedback && isCorrectOption
                          ? 'border-emerald-600 bg-emerald-600 text-white'
                          : feedback && isSelected && !feedback.correct
                            ? 'border-rose-600 bg-rose-600 text-white'
                            : 'border-border bg-background text-foreground'
                      )}
                    >
                      {optionKey}
                    </span>
                    <span className="break-words text-base font-medium leading-snug [overflow-wrap:anywhere]">
                      {optionText}
                    </span>
                  </div>

                  {feedback && isCorrectOption ? (
                    <CheckCircle2
                      className="h-5 w-5 shrink-0 text-emerald-600 dark:text-emerald-400"
                      strokeWidth={2}
                    />
                  ) : null}
                  {feedback && isSelected && !feedback.correct ? (
                    <XCircle
                      className="h-5 w-5 shrink-0 text-rose-600 dark:text-rose-400"
                      strokeWidth={2}
                    />
                  ) : null}
                </button>
              );
            })}
          </div>

          {submitAnswerMutation.isPending ? (
            <div className="flex items-center justify-center gap-2 text-sm text-muted-foreground">
              <Loader2 className="h-4 w-4 animate-spin text-brand-strong" />
              <span>Đang kiểm tra đáp án...</span>
            </div>
          ) : null}

          {feedback ? (
            <div className="space-y-4">
              <div
                className={cn(
                  'rounded-xl border p-4 text-sm',
                  feedback.correct
                    ? 'border-emerald-200 bg-emerald-50/70 text-emerald-950 dark:border-emerald-900/70 dark:bg-emerald-950/35 dark:text-emerald-100'
                    : 'border-rose-200 bg-rose-50/70 text-rose-950 dark:border-rose-900/70 dark:bg-rose-950/35 dark:text-rose-100'
                )}
              >
                <p className="font-bold">
                  {feedback.correct
                    ? 'Chính xác! Làm tốt lắm.'
                    : `Chưa đúng rồi — Đáp án đúng là ${feedback.correctOption}.`}
                </p>
                {feedback.explanation ? (
                  <p className="mt-1.5 break-words leading-relaxed [overflow-wrap:anywhere]">
                    {feedback.explanation}
                  </p>
                ) : null}
              </div>

              <Button
                type="button"
                size="lg"
                className="h-12 w-full gap-2 text-base font-semibold"
                onClick={() => void handleNext()}
              >
                <span>
                  {!feedback.gateLocked || feedback.sessionStatus === 'COMPLETED'
                    ? 'Hoàn thành & Xem tổng kết'
                    : 'Câu tiếp theo'}
                </span>
                <ArrowRight className="h-4 w-4" strokeWidth={1.75} />
              </Button>
            </div>
          ) : null}
        </CardContent>
      </Card>
    </div>
  );
}
