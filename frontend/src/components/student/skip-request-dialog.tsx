'use client';

import { useEffect, useState } from 'react';
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
import { Label } from '@/components/ui/label';
import type { StudyTaskSummaryResponse } from '@/types';

const QUICK_REASONS = [
  { label: 'Làm bài cô giao', value: 'Làm bài tập thầy cô giao trên lớp' },
  { label: 'Bài tập SGK/SBT', value: 'Đã làm bài tập SGK/SBT thay thế' },
  { label: 'Ôn kiểm tra', value: 'Ôn thi / kiểm tra môn khác' },
  { label: 'Việc gia đình', value: 'Lý do sức khỏe / việc gia đình' },
];

interface SkipRequestDialogProps {
  task: StudyTaskSummaryResponse | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  isSubmitting: boolean;
  onSubmit: (taskId: number, reason: string) => void;
}

export function SkipRequestDialog({
  task,
  open,
  onOpenChange,
  isSubmitting,
  onSubmit,
}: SkipRequestDialogProps) {
  const [reason, setReason] = useState('');

  useEffect(() => {
    if (open) {
      setReason(task?.skipRequestReason ?? '');
    }
  }, [open, task]);

  if (!task) {
    return null;
  }

  const trimmed = reason.trim();
  const isValid = trimmed.length >= 5 && trimmed.length <= 300;
  const taskLabel = task.taskType === 'REVIEW' ? 'Ôn bài cũ' : 'Soạn bài mới';

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="w-[calc(100%-1.5rem)] sm:w-full">
        <DialogHeader>
          <DialogTitle>
            Xin phụ huynh bỏ qua · Môn {task.subjectName}
          </DialogTitle>
          <DialogDescription>
            Cần phụ huynh đồng ý để bỏ qua phần <strong>{taskLabel}</strong>.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-3">
          <div className="flex flex-wrap gap-1.5">
            {QUICK_REASONS.map((preset) => (
              <button
                key={preset.label}
                type="button"
                onClick={() => setReason(preset.value)}
                className="min-h-[44px] rounded-full border bg-muted/50 px-3 py-1.5 text-xs font-medium text-foreground transition-colors hover:bg-muted sm:min-h-0"
              >
                {preset.label}
              </button>
            ))}
          </div>

          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <Label htmlFor="skip-reason-input">Lý do xin bỏ qua</Label>
              <span className="font-mono text-xs text-muted-foreground">
                {trimmed.length}/300
              </span>
            </div>
            <textarea
              id="skip-reason-input"
              rows={3}
              maxLength={300}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="Nhập lý do xin bỏ qua..."
              className="w-full rounded-lg border bg-background px-3 py-2 text-base text-foreground placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring md:text-sm"
            />
            {trimmed.length > 0 && trimmed.length < 5 ? (
              <p className="text-xs text-rose-600 dark:text-rose-400">
                Nhập ít nhất 5 ký tự.
              </p>
            ) : null}
          </div>
        </div>

        <DialogFooter>
          <Button
            type="button"
            variant="outline"
            className="h-11 sm:h-10"
            disabled={isSubmitting}
            onClick={() => onOpenChange(false)}
          >
            Hủy
          </Button>
          <Button
            type="button"
            className="h-11 gap-1.5 sm:h-10"
            disabled={!isValid || isSubmitting}
            onClick={() => onSubmit(task.id, trimmed)}
          >
            {isSubmitting ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : null}
            Gửi xin phép phụ huynh
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
