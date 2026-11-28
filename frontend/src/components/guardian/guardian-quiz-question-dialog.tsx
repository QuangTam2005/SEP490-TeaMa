'use client';

import { useEffect } from 'react';
import { zodResolver } from '@hookform/resolvers/zod';
import { Controller, useForm } from 'react-hook-form';
import { z } from 'zod';
import { Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { cn } from '@/lib/utils';
import type { CreateQuizQuestionPayload, QuizQuestionResponse } from '@/types';

const questionSchema = z.object({
  questionText: z
    .string()
    .trim()
    .min(1, 'Nhập nội dung câu hỏi')
    .max(500, 'Tối đa 500 ký tự'),
  optionA: z
    .string()
    .trim()
    .min(1, 'Nhập đáp án A')
    .max(250, 'Tối đa 250 ký tự'),
  optionB: z
    .string()
    .trim()
    .min(1, 'Nhập đáp án B')
    .max(250, 'Tối đa 250 ký tự'),
  correctOption: z.enum(['A', 'B']),
  topicLabel: z.string().trim().max(80, 'Tối đa 80 ký tự').optional(),
  explanation: z.string().trim().max(500, 'Tối đa 500 ký tự').optional(),
  active: z.boolean(),
});

type QuestionFormValues = z.infer<typeof questionSchema>;

interface GuardianQuizQuestionDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  editingQuestion: QuizQuestionResponse | null;
  isSubmitting: boolean;
  onSubmit: (payload: CreateQuizQuestionPayload, questionId?: number) => void;
}

export function GuardianQuizQuestionDialog({
  open,
  onOpenChange,
  editingQuestion,
  isSubmitting,
  onSubmit,
}: GuardianQuizQuestionDialogProps) {
  const {
    control,
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<QuestionFormValues>({
    resolver: zodResolver(questionSchema),
    defaultValues: {
      questionText: '',
      optionA: '',
      optionB: '',
      correctOption: 'A',
      topicLabel: '',
      explanation: '',
      active: true,
    },
  });

  useEffect(() => {
    if (!open) return;
    if (editingQuestion) {
      reset({
        questionText: editingQuestion.questionText,
        optionA: editingQuestion.optionA,
        optionB: editingQuestion.optionB,
        correctOption: editingQuestion.correctOption,
        topicLabel: editingQuestion.topicLabel ?? '',
        explanation: editingQuestion.explanation ?? '',
        active: editingQuestion.active,
      });
    } else {
      reset({
        questionText: '',
        optionA: '',
        optionB: '',
        correctOption: 'A',
        topicLabel: '',
        explanation: '',
        active: true,
      });
    }
  }, [open, editingQuestion, reset]);

  const handleFormSubmit = (values: QuestionFormValues) => {
    onSubmit(
      {
        questionText: values.questionText,
        optionA: values.optionA,
        optionB: values.optionB,
        correctOption: values.correctOption,
        topicLabel: values.topicLabel || null,
        explanation: values.explanation || null,
        active: values.active,
      },
      editingQuestion?.id
    );
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] w-[calc(100%-1.5rem)] overflow-y-auto sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>
            {editingQuestion ? 'Chỉnh sửa câu hỏi' : 'Thêm câu hỏi mới (2 lựa chọn)'}
          </DialogTitle>
          <DialogDescription>
            Con sẽ chọn A hoặc B và xem giải thích ngắn ngay sau khi chọn.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="quiz-q-text">Nội dung câu hỏi *</Label>
            <textarea
              id="quiz-q-text"
              rows={3}
              placeholder="VD: Từ tiếng Anh nào có nghĩa là 'thư viện'?"
              className="w-full rounded-lg border bg-background px-3 py-2 text-base text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring md:text-sm"
              {...register('questionText')}
            />
            {errors.questionText ? (
              <p className="text-xs text-rose-600 dark:text-rose-400">{errors.questionText.message}</p>
            ) : null}
          </div>

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label htmlFor="quiz-opt-a">Đáp án A *</Label>
              <Input id="quiz-opt-a" placeholder="VD: Library" {...register('optionA')} />
              {errors.optionA ? (
                <p className="text-xs text-rose-600 dark:text-rose-400">{errors.optionA.message}</p>
              ) : null}
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="quiz-opt-b">Đáp án B *</Label>
              <Input id="quiz-opt-b" placeholder="VD: Bakery" {...register('optionB')} />
              {errors.optionB ? (
                <p className="text-xs text-rose-600 dark:text-rose-400">{errors.optionB.message}</p>
              ) : null}
            </div>
          </div>

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label>Đáp án đúng *</Label>
              <Controller
                control={control}
                name="correctOption"
                render={({ field }) => (
                  <div className="grid grid-cols-2 gap-2">
                    {(['A', 'B'] as const).map((opt) => (
                      <button
                        key={opt}
                        type="button"
                        onClick={() => field.onChange(opt)}
                        className={cn(
                          'h-10 rounded-lg border font-mono text-sm font-bold transition-colors',
                          field.value === opt
                            ? 'border-emerald-600 bg-emerald-600 text-white'
                            : 'bg-background text-foreground hover:bg-muted'
                        )}
                      >
                        Đáp án {opt}
                      </button>
                    ))}
                  </div>
                )}
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="quiz-topic">Chủ đề (tùy chọn)</Label>
              <Input
                id="quiz-topic"
                placeholder="VD: Tiếng Anh, Toán"
                {...register('topicLabel')}
              />
              {errors.topicLabel ? (
                <p className="text-xs text-rose-600 dark:text-rose-400">{errors.topicLabel.message}</p>
              ) : null}
            </div>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="quiz-exp">Giải thích ngắn khi con chọn xong (tùy chọn)</Label>
            <Input
              id="quiz-exp"
              placeholder="VD: Library là thư viện, Bakery là tiệm bánh"
              {...register('explanation')}
            />
            {errors.explanation ? (
              <p className="text-xs text-rose-600 dark:text-rose-400">{errors.explanation.message}</p>
            ) : null}
          </div>

          <label className="flex cursor-pointer items-center gap-2 text-sm text-foreground">
            <input
              type="checkbox"
              className="h-4 w-4 rounded border-border accent-sky-600"
              {...register('active')}
            />
            <span>Kích hoạt câu hỏi này trong vòng bóc</span>
          </label>

          <DialogFooter className="gap-2">
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={isSubmitting}
            >
              Hủy
            </Button>
            <Button type="submit" disabled={isSubmitting} className="gap-2">
              {isSubmitting ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
              <span>{editingQuestion ? 'Lưu thay đổi' : 'Thêm câu hỏi'}</span>
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
