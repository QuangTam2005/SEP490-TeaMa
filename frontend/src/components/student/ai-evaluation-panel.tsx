'use client';

import { AlertTriangle, ExternalLink, Loader2, RefreshCw, Sparkles } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { StatusBadge } from '@/components/shared/status-badge';
import {
  MAX_FEEDBACK_CHARS,
  MAX_SUBMISSION_CHARS,
  MIN_SUBMISSION_CHARS,
} from '@/lib/study-utils';
import type { StudySubmissionResponse } from '@/types';

interface AiEvaluationPanelProps {
  submission: StudySubmissionResponse | null;
  isEvaluating: boolean;
  onRetryEvaluation: (submissionId: number) => void;
  isRetrying: boolean;
}

export function AiEvaluationPanel({
  submission,
  isEvaluating,
  onRetryEvaluation,
  isRetrying,
}: AiEvaluationPanelProps) {
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

        <div className="whitespace-pre-wrap break-words rounded-lg border border-brand/20 bg-brand-soft/35 p-3.5 text-sm leading-relaxed text-foreground [overflow-wrap:anywhere] dark:bg-sky-950/25">
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
