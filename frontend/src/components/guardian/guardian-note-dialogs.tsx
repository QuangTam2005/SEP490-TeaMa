'use client';

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
import type { GuardianTaskItem } from '@/types';

interface GuardianRejectSkipDialogProps {
  target: GuardianTaskItem | null;
  note: string;
  onNoteChange: (value: string) => void;
  isSubmitting: boolean;
  onClose: () => void;
  onConfirm: (taskId: number, note?: string) => void;
}

export function GuardianRejectSkipDialog({
  target,
  note,
  onNoteChange,
  isSubmitting,
  onClose,
  onConfirm,
}: GuardianRejectSkipDialogProps) {
  return (
    <Dialog
      open={Boolean(target)}
      onOpenChange={(open) => {
        if (!open) {
          onClose();
        }
      }}
    >
      <DialogContent className="w-[calc(100%-1.5rem)] sm:w-full">
        <DialogHeader>
          <DialogTitle>
            Từ chối yêu cầu bỏ qua · {target?.subjectName}
          </DialogTitle>
          <DialogDescription>
            Giữ nguyên bài học để con tự hoàn thành.
          </DialogDescription>
        </DialogHeader>
        {target?.skipRequestReason ? (
          <p className="break-words rounded-lg border bg-muted/50 px-3 py-2 text-xs text-muted-foreground [overflow-wrap:anywhere]">
            Lý do con gửi: &ldquo;{target.skipRequestReason}&rdquo;
          </p>
        ) : null}
        <div className="space-y-1.5">
          <Label htmlFor="guardian-reject-note">Lời nhắn cho con (tùy chọn)</Label>
          <textarea
            id="guardian-reject-note"
            rows={3}
            maxLength={300}
            value={note}
            onChange={(e) => onNoteChange(e.target.value)}
            placeholder="Lời nhắn cho con..."
            className="w-full rounded-lg border bg-background px-3 py-2 text-base text-foreground placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring md:text-sm"
          />
        </div>
        <DialogFooter>
          <Button
            type="button"
            variant="outline"
            className="h-11 sm:h-10"
            disabled={isSubmitting}
            onClick={onClose}
          >
            Hủy
          </Button>
          <Button
            type="button"
            className="h-11 gap-1.5 sm:h-10"
            disabled={isSubmitting || !target}
            onClick={() => {
              if (!target) return;
              onConfirm(target.taskId, note.trim() || undefined);
            }}
          >
            {isSubmitting ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
            Xác nhận từ chối
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

interface GuardianVerifyRedoDialogProps {
  target: GuardianTaskItem | null;
  note: string;
  onNoteChange: (value: string) => void;
  isSubmitting: boolean;
  onClose: () => void;
  onConfirm: (taskId: number, note?: string) => void;
}

export function GuardianVerifyRedoDialog({
  target,
  note,
  onNoteChange,
  isSubmitting,
  onClose,
  onConfirm,
}: GuardianVerifyRedoDialogProps) {
  return (
    <Dialog
      open={Boolean(target)}
      onOpenChange={(open) => {
        if (!open) {
          onClose();
        }
      }}
    >
      <DialogContent className="w-[calc(100%-1.5rem)] sm:w-full">
        <DialogHeader>
          <DialogTitle>Nhắc con làm lại · {target?.subjectName}</DialogTitle>
          <DialogDescription>
            Ghi chú phần con cần sửa hoặc bổ sung trên Drive.
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-1.5">
          <Label htmlFor="guardian-verify-note">Lời nhắn cho con</Label>
          <textarea
            id="guardian-verify-note"
            rows={3}
            maxLength={300}
            value={note}
            onChange={(e) => onNoteChange(e.target.value)}
            placeholder="VD: Ảnh bài 3 hơi mờ, chụp lại giúp bố mẹ nhé."
            className="w-full rounded-lg border bg-background px-3 py-2 text-base text-foreground placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring md:text-sm"
          />
        </div>
        <DialogFooter>
          <Button
            type="button"
            variant="outline"
            className="h-11 sm:h-10"
            disabled={isSubmitting}
            onClick={onClose}
          >
            Hủy
          </Button>
          <Button
            type="button"
            className="h-11 gap-1.5 sm:h-10"
            disabled={isSubmitting || !target}
            onClick={() => {
              if (!target) return;
              onConfirm(target.taskId, note.trim() || undefined);
            }}
          >
            {isSubmitting ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
            Gửi nhắc nhở con
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
