'use client';

import { useEffect, useState } from 'react';
import { FolderUp, Loader2 } from 'lucide-react';
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
import { isValidHttpsUrl } from '@/lib/study-utils';
import type {
  GuardianAssignCustomTaskRequest,
  GuardianTaskItem,
} from '@/types';

const QUICK_CUSTOM_TASKS = [
  { label: 'Xem video bài học', title: 'Xem hết video bài học & ghi nhớ ý chính' },
  { label: 'Làm phiếu bài tập', title: 'Làm phiếu bài tập & nộp lên Drive' },
  { label: 'Bài tập SGK/SBT', title: 'Giải bài tập SGK/SBT & nộp lên Drive' },
  { label: 'Viết ra vở', title: 'Viết/tóm tắt ra vở & chụp ảnh gửi Drive' },
];

interface AssignCustomTaskDialogProps {
  task: GuardianTaskItem | null;
  defaultDriveFolderUrl?: string | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  isSubmitting: boolean;
  onSubmit: (taskId: number, payload: GuardianAssignCustomTaskRequest) => void;
}

export function AssignCustomTaskDialog({
  task,
  defaultDriveFolderUrl,
  open,
  onOpenChange,
  isSubmitting,
  onSubmit,
}: AssignCustomTaskDialogProps) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [driveFolderUrl, setDriveFolderUrl] = useState('');
  const [saveAsDefault, setSaveAsDefault] = useState(true);

  useEffect(() => {
    if (open && task) {
      setTitle(task.customTaskTitle ?? '');
      setDescription(task.customTaskDescription ?? '');
      const initialUrl =
        task.assignmentMode === 'CUSTOM_DRIVE'
          ? (task.customDriveFolderUrl ?? '')
          : (defaultDriveFolderUrl ?? '');
      setDriveFolderUrl(initialUrl);
      setSaveAsDefault(!defaultDriveFolderUrl);
    }
  }, [open, task, defaultDriveFolderUrl]);

  if (!task) {
    return null;
  }

  const trimmedTitle = title.trim();
  const trimmedDesc = description.trim();
  const trimmedUrl = driveFolderUrl.trim();
  const isUrlValid =
    trimmedUrl.length === 0 || isValidHttpsUrl(trimmedUrl);
  const isValid =
    trimmedTitle.length >= 5 &&
    trimmedTitle.length <= 200 &&
    trimmedDesc.length <= 500 &&
    isUrlValid;

  const taskLabel = task.taskType === 'REVIEW' ? 'Ôn bài cũ' : 'Soạn bài mới';

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[92vh] w-[calc(100%-1.5rem)] overflow-y-auto sm:w-full">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <FolderUp className="h-5 w-5 text-brand-strong" strokeWidth={1.75} />
            Giao bài thay thế · {task.subjectName}
          </DialogTitle>
          <DialogDescription>
            Thay phần <strong>{taskLabel}</strong> bằng nhiệm vụ phụ huynh tự giao.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          <div className="flex flex-wrap gap-1.5">
            {QUICK_CUSTOM_TASKS.map((preset) => (
              <button
                key={preset.label}
                type="button"
                onClick={() => setTitle(preset.title)}
                className="min-h-[44px] rounded-full border bg-muted/50 px-3 py-1.5 text-xs font-medium text-foreground transition-colors hover:bg-muted sm:min-h-0"
              >
                {preset.label}
              </button>
            ))}
          </div>

          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <Label htmlFor="custom-task-title">Nhiệm vụ thay thế *</Label>
              <span className="font-mono text-xs text-muted-foreground">
                {trimmedTitle.length}/200
              </span>
            </div>
            <Input
              id="custom-task-title"
              maxLength={200}
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="VD: Làm phiếu Toán tuần 12"
            />
            {trimmedTitle.length > 0 && trimmedTitle.length < 5 ? (
              <p className="text-xs text-rose-600 dark:text-rose-400">
                Nhập ít nhất 5 ký tự.
              </p>
            ) : null}
          </div>

          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <Label htmlFor="custom-task-desc">Ghi chú thêm (tùy chọn)</Label>
              <span className="font-mono text-xs text-muted-foreground">
                {trimmedDesc.length}/500
              </span>
            </div>
            <textarea
              id="custom-task-desc"
              rows={2}
              maxLength={500}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Số trang, lưu ý..."
              className="w-full rounded-lg border bg-background px-3 py-2 text-base text-foreground placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring md:text-sm"
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="custom-drive-url">
              Link thư mục Google Drive (tùy chọn)
            </Label>
            <Input
              id="custom-drive-url"
              type="url"
              maxLength={500}
              value={driveFolderUrl}
              onChange={(e) => setDriveFolderUrl(e.target.value)}
              placeholder="Bỏ trống nếu không cần nộp ảnh/file lên Drive..."
            />
            {trimmedUrl.length > 0 ? (
              <p className="text-xs text-muted-foreground">
                Mở quyền chỉnh sửa cho &ldquo;Bất kỳ ai có đường liên kết&rdquo;.
              </p>
            ) : null}
            {trimmedUrl.length > 0 && !isUrlValid ? (
              <p className="text-xs text-rose-600 dark:text-rose-400">
                Link phải bắt đầu bằng https://
              </p>
            ) : null}
          </div>

          {trimmedUrl.length > 0 ? (
            <label className="flex cursor-pointer items-center gap-2 text-xs font-medium text-foreground">
              <input
                type="checkbox"
                checked={saveAsDefault}
                onChange={(e) => setSaveAsDefault(e.target.checked)}
                className="h-4 w-4 rounded border"
              />
              Lưu làm link Drive mặc định
            </label>
          ) : null}
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
            onClick={() =>
              onSubmit(task.taskId, {
                title: trimmedTitle,
                description: trimmedDesc || null,
                driveFolderUrl: trimmedUrl || null,
                saveAsDefaultDriveFolder: Boolean(trimmedUrl) && saveAsDefault,
              })
            }
          >
            {isSubmitting ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : null}
            {task.assignmentMode === 'CUSTOM_DRIVE'
              ? 'Cập nhật bài giao'
              : 'Giao bài thay thế'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
