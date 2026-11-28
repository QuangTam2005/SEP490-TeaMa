'use client';

import { useMemo, useState } from 'react';
import { AlertCircle, CheckCircle2, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Label } from '@/components/ui/label';
import type { CreateQuizQuestionPayload, QuizOption } from '@/types';

interface GuardianQuizBulkDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  isSubmitting: boolean;
  onSubmit: (questions: CreateQuizQuestionPayload[]) => void;
}

const SAMPLE_PLACEHOLDER = `Thủ đô của Việt Nam là gì? | Hà Nội | Đà Nẵng | A | Hà Nội là thủ đô từ năm 1010 | Địa lí
2 x 8 bằng bao nhiêu? | 14 | 16 | B | 2 nhân 8 bằng 16 | Toán
Từ "Library" nghĩa là gì? | Thư viện | Hiệu sách | A`;

interface ParsedLineResult {
  questions: CreateQuizQuestionPayload[];
  errors: string[];
}

function parseBulkLines(raw: string): ParsedLineResult {
  const lines = raw
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line.length > 0);

  const questions: CreateQuizQuestionPayload[] = [];
  const errors: string[] = [];

  lines.forEach((line, index) => {
    const separator = line.includes('\t') ? '\t' : '|';
    const parts = line.split(separator).map((p) => p.trim());
    if (parts.length < 4) {
      errors.push(
        `Dòng ${index + 1}: cần ít nhất 4 cột (Câu hỏi | Đáp án A | Đáp án B | A hoặc B).`
      );
      return;
    }

    const [questionText, optionA, optionB, rawCorrect, explanation, topicLabel] =
      parts;
    const upperCorrect = rawCorrect.toUpperCase();
    if (upperCorrect !== 'A' && upperCorrect !== 'B') {
      errors.push(`Dòng ${index + 1}: đáp án đúng "${rawCorrect}" phải là A hoặc B.`);
      return;
    }
    if (!questionText || !optionA || !optionB) {
      errors.push(`Dòng ${index + 1}: câu hỏi và 2 đáp án không được trống.`);
      return;
    }

    questions.push({
      questionText: questionText.slice(0, 500),
      optionA: optionA.slice(0, 250),
      optionB: optionB.slice(0, 250),
      correctOption: upperCorrect as QuizOption,
      explanation: explanation ? explanation.slice(0, 500) : null,
      topicLabel: topicLabel ? topicLabel.slice(0, 80) : null,
      active: true,
    });
  });

  if (questions.length > 100) {
    errors.push('Mỗi lần thêm nhanh tối đa 100 câu hỏi.');
  }

  return { questions, errors };
}

export function GuardianQuizBulkDialog({
  open,
  onOpenChange,
  isSubmitting,
  onSubmit,
}: GuardianQuizBulkDialogProps) {
  const [rawText, setRawText] = useState('');

  const parsed = useMemo(() => parseBulkLines(rawText), [rawText]);

  const handleConfirm = () => {
    if (parsed.questions.length === 0 || parsed.errors.length > 0) {
      return;
    }
    onSubmit(parsed.questions);
    setRawText('');
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>Thêm nhanh nhiều câu hỏi</DialogTitle>
          <DialogDescription>
            Mỗi dòng 1 câu theo mẫu{' '}
            <code className="rounded bg-muted px-1 py-0.5 font-mono text-xs">
              Câu hỏi | Đáp án A | Đáp án B | A/B | Giải thích | Chủ đề
            </code>{' '}
            (hỗ trợ dán trực tiếp từ Excel).
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <Label htmlFor="quiz-bulk-input">Danh sách câu hỏi (tối đa 100 câu/lần)</Label>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              className="h-8 text-xs text-brand-strong"
              onClick={() => setRawText(SAMPLE_PLACEHOLDER)}
            >
              Chèn mẫu ví dụ
            </Button>
          </div>

          <textarea
            id="quiz-bulk-input"
            rows={8}
            value={rawText}
            onChange={(e) => setRawText(e.target.value)}
            placeholder={SAMPLE_PLACEHOLDER}
            className="w-full rounded-lg border bg-background p-3 font-mono text-xs leading-relaxed text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring sm:text-sm"
          />

          <div className="flex flex-wrap items-center gap-3 text-xs">
            <span className="inline-flex items-center gap-1.5 font-semibold text-emerald-700 dark:text-emerald-400">
              <CheckCircle2 className="h-4 w-4" strokeWidth={1.75} />
              Hợp lệ: {parsed.questions.length} câu
            </span>
            {parsed.errors.length > 0 ? (
              <span className="inline-flex items-center gap-1.5 font-semibold text-rose-600 dark:text-rose-400">
                <AlertCircle className="h-4 w-4" strokeWidth={1.75} />
                {parsed.errors.length} dòng cần sửa
              </span>
            ) : null}
          </div>

          {parsed.errors.length > 0 ? (
            <div className="max-h-28 overflow-y-auto rounded-lg border border-rose-200 bg-rose-50/70 p-2.5 text-xs text-rose-800 dark:border-rose-900/70 dark:bg-rose-950/30 dark:text-rose-200">
              {parsed.errors.map((err) => (
                <p key={err}>{err}</p>
              ))}
            </div>
          ) : null}
        </div>

        <DialogFooter className="gap-2">
          <Button
            type="button"
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={isSubmitting}
          >
            Hủy
          </Button>
          <Button
            type="button"
            disabled={
              isSubmitting ||
              parsed.questions.length === 0 ||
              parsed.errors.length > 0
            }
            onClick={handleConfirm}
            className="gap-2"
          >
            {isSubmitting ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
            <span>Thêm {parsed.questions.length} câu hỏi</span>
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
