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
  'Làm bài tập thầy cô giao trên lớp',
  'Đã làm bài tập SGK/SBT thay thế',
  'Ôn thi / kiểm tra môn khác',
  'Lý do sức khỏe / việc gia đình',
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
            Phần <strong>{taskLabel}</strong> chỉ được tính là bỏ qua sau khi
            phụ huynh đồng ý. Hãy ghi rõ lý do để bố mẹ xem xét nhé.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-3">
          <div className="space-y-1.5">
            <span className="text-xs font-medium text-muted-foreground">
              Chọn nhanh lý do phổ biến:
            </span>
            <div className="flex flex-wrap gap-1.5">
              {QUICK_REASONS.map((preset) => (
                <button
                  key={preset}
                  type="button"
                  onClick={() => setReason(preset)}
                  className="min-h-[44px] rounded-full border bg-muted/50 px-3 py-2 text-xs font-medium text-foreground transition-colors hover:bg-muted sm:min-h-0 sm:py-1.5"
                >
                  {preset}
                </button>
              ))}
            </div>
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
              placeholder="Ví dụ: Hôm nay cô giao phiếu bài tập Toán nên con làm bài cô giao trước..."
              className="w-full rounded-lg border bg-background px-3 py-2 text-base text-foreground placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring md:text-sm"
            />
            {trimmed.length > 0 && trimmed.length < 5 ? (
              <p className="text-xs text-rose-600 dark:text-rose-400">
                Vui lòng nhập ít nhất 5 ký tự để phụ huynh dễ hiểu lý do nhé.
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
