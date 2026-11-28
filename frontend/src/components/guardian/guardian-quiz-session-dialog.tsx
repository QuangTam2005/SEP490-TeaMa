'use client';

import { useQuery } from '@tanstack/react-query';
import { CheckCircle2, Loader2, XCircle } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { ErrorStateCard } from '@/components/shared/error-state-card';
import { formatViDate } from '@/lib/study-utils';
import { cn } from '@/lib/utils';
import { quizService } from '@/services';

interface GuardianQuizSessionDialogProps {
  sessionId: number | null;
  studentId: number | null;
  onClose: () => void;
}

export function GuardianQuizSessionDialog({
  sessionId,
  studentId,
  onClose,
}: GuardianQuizSessionDialogProps) {
  const detailQuery = useQuery({
    queryKey: ['guardian-quiz-session-detail', sessionId, studentId],
    queryFn: () => quizService.getGuardianSessionDetail(sessionId!, studentId),
    enabled: Boolean(sessionId),
  });

  return (
    <Dialog open={Boolean(sessionId)} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[85vh] w-[calc(100%-1.5rem)] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>Chi tiết bài trắc nghiệm</DialogTitle>
          <DialogDescription>
            {detailQuery.data
              ? `${detailQuery.data.studentName} · Ngày ${formatViDate(detailQuery.data.sessionDate)} · ${detailQuery.data.cycleLabel}`
              : 'Xem lại lựa chọn của con ở từng câu hỏi.'}
          </DialogDescription>
        </DialogHeader>

        {detailQuery.isLoading ? (
          <div className="flex items-center justify-center gap-2 py-10 text-sm text-muted-foreground">
            <Loader2 className="h-4 w-4 animate-spin text-brand-strong" />
            <span>Đang tải chi tiết phiên...</span>
          </div>
        ) : detailQuery.isError ? (
          <ErrorStateCard
            error={detailQuery.error}
            title="Không thể tải chi tiết phiên làm bài"
            onRetry={() => void detailQuery.refetch()}
          />
        ) : detailQuery.data ? (
          <div className="space-y-4">
            <div className="flex flex-wrap items-center gap-2 rounded-xl border bg-muted/30 p-3 text-sm">
              <Badge variant={detailQuery.data.status === 'COMPLETED' ? 'success' : 'info'}>
                {detailQuery.data.status === 'COMPLETED' ? 'Hoàn thành' : 'Đang làm dở'}
              </Badge>
              <span className="font-mono font-semibold text-foreground">
                Đúng {detailQuery.data.correctCount}/{detailQuery.data.totalQuestions} (
                {detailQuery.data.accuracyPercent}%)
              </span>
            </div>

            <div className="space-y-3">
              {detailQuery.data.items.map((item) => {
                const answered = item.selectedOption !== null;
                return (
                  <div key={item.itemId} className="space-y-2 rounded-xl border p-3.5">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <div className="flex items-center gap-2">
                        <Badge variant="brand" className="font-mono">
                          Câu {item.itemOrder}
                        </Badge>
                        {item.topicLabel ? (
                          <Badge variant="secondary">{item.topicLabel}</Badge>
                        ) : null}
                      </div>

                      {answered ? (
                        item.correct ? (
                          <span className="inline-flex items-center gap-1 text-xs font-semibold text-emerald-700 dark:text-emerald-400">
                            <CheckCircle2 className="h-4 w-4" strokeWidth={2} />
                            Con chọn {item.selectedOption} (Đúng)
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 text-xs font-semibold text-rose-600 dark:text-rose-400">
                            <XCircle className="h-4 w-4" strokeWidth={2} />
                            Con chọn {item.selectedOption} (Đáp án đúng: {item.correctOption})
                          </span>
                        )
                      ) : (
                        <Badge variant="secondary">Chưa trả lời</Badge>
                      )}
                    </div>

                    <p className="break-words text-sm font-semibold text-foreground [overflow-wrap:anywhere]">
                      {item.questionText}
                    </p>

                    <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                      {(['A', 'B'] as const).map((opt) => {
                        const text = opt === 'A' ? item.optionA : item.optionB;
                        const isPicked = item.selectedOption === opt;
                        const isCorrect = item.correctOption === opt;
                        return (
                          <div
                            key={opt}
                            className={cn(
                              'break-words rounded-lg border px-3 py-2 text-xs [overflow-wrap:anywhere]',
                              isCorrect
                                ? 'border-emerald-300 bg-emerald-50/70 text-emerald-900 dark:border-emerald-800 dark:bg-emerald-950/30 dark:text-emerald-200'
                                : isPicked
                                  ? 'border-rose-300 bg-rose-50/70 text-rose-900 dark:border-rose-800 dark:bg-rose-950/30 dark:text-rose-200'
                                  : 'bg-background text-muted-foreground'
                            )}
                          >
                            <span className="font-mono font-bold">{opt}. </span>
                            {text}
                          </div>
                        );
                      })}
                    </div>

                    {item.explanation ? (
                      <p className="break-words text-xs text-muted-foreground [overflow-wrap:anywhere]">
                        Giải thích: {item.explanation}
                      </p>
                    ) : null}
                  </div>
                );
              })}
            </div>
          </div>
        ) : null}
      </DialogContent>
    </Dialog>
  );
}
