'use client';

import { useRef, useState, type DragEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Download,
  FileText,
  Loader2,
  Upload,
} from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import { StatusBadge } from '@/components/shared/status-badge';
import { extractApiErrorMessage } from '@/components/shared/form-error-utils';
import { projectService } from '@/services';
import type { MilestoneDeliverableResponse } from '@/types';
import { cn } from '@/lib/utils';

interface DeliverableUploadRowProps {
  projectId: number;
  deliverable: MilestoneDeliverableResponse;
  readOnly?: boolean;
}

function formatBytes(bytes: number | null): string {
  if (!bytes || bytes <= 0) {
    return '0 B';
  }
  if (bytes >= 1024 * 1024) {
    return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
  }
  return `${Math.round(bytes / 1024)} KB`;
}

export function DeliverableUploadRow({
  projectId,
  deliverable,
  readOnly = false,
}: DeliverableUploadRowProps) {
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const [progress, setProgress] = useState<number>(0);
  const [isDragging, setIsDragging] = useState(false);
  const [inlineError, setInlineError] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const uploadMutation = useMutation({
    mutationFn: (file: File) =>
      projectService.uploadDeliverable(
        projectId,
        deliverable.id,
        file,
        (percent) => setProgress(percent)
      ),
    onSuccess: (updated) => {
      setInlineError(null);
      setProgress(0);
      void queryClient.invalidateQueries({
        queryKey: ['project-milestones', projectId],
      });
      toast.success(`Đã tải lên ${updated.fileName ?? 'tệp báo cáo'}`);
    },
    onError: (err) => {
      setProgress(0);
      const message = extractApiErrorMessage(err);
      setInlineError(message);
      toast.error(message);
    },
  });

  const handleFileSelected = (file: File | undefined) => {
    if (!file || readOnly) {
      return;
    }
    setInlineError(null);
    uploadMutation.mutate(file);
  };

  const handleDrop = (event: DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    setIsDragging(false);
    if (readOnly) {
      return;
    }
    const file = event.dataTransfer.files?.[0];
    handleFileSelected(file);
  };

  return (
    <div
      onDragOver={(e) => {
        if (readOnly) return;
        e.preventDefault();
        setIsDragging(true);
      }}
      onDragLeave={() => setIsDragging(false)}
      onDrop={handleDrop}
      className={cn(
        'flex flex-col gap-3 px-4 py-3.5 transition-colors sm:flex-row sm:items-center sm:justify-between',
        isDragging && 'bg-brand-soft'
      )}
    >
      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center gap-2">
          <FileText
            className="h-4 w-4 shrink-0 text-muted-foreground"
            strokeWidth={1.75}
          />
          <p className="text-sm font-medium text-foreground">
            {deliverable.itemName}
          </p>
          {deliverable.isRequired ? (
            <span className="rounded border bg-muted px-1.5 py-0.5 font-mono text-[10px] font-medium text-muted-foreground">
              Bắt buộc
            </span>
          ) : null}
        </div>

        {deliverable.fileName ? (
          <p className="mt-1 pl-6 font-mono text-xs text-muted-foreground tabular-nums">
            {deliverable.fileName} • {formatBytes(deliverable.fileSize)}
            {deliverable.uploadedByName
              ? ` • Nộp bởi ${deliverable.uploadedByName}`
              : ''}
          </p>
        ) : (
          <p className="mt-1 pl-6 text-xs text-muted-foreground">
            Chưa có tệp nộp. Kéo thả hoặc bấm tải lên (.docx, .pdf, .xlsx, .zip
            — tối đa 50MB).
          </p>
        )}

        {uploadMutation.isPending ? (
          <div className="mt-2 pl-6">
            <div className="mb-1 flex items-center justify-between font-mono text-[11px] text-brand tabular-nums">
              <span>Đang tải lên...</span>
              <span>{progress}%</span>
            </div>
            <div className="h-1.5 w-full max-w-xs overflow-hidden rounded-full bg-muted">
              <div
                className="h-full bg-brand transition-all duration-150"
                style={{ width: `${progress}%` }}
              />
            </div>
          </div>
        ) : null}

        {inlineError ? (
          <p className="mt-1.5 pl-6 text-xs text-rose-600 dark:text-rose-400">
            {inlineError}
          </p>
        ) : null}
      </div>

      <div className="flex shrink-0 items-center gap-2 pl-6 sm:pl-0">
        <StatusBadge status={deliverable.status} />

        {deliverable.fileName ? (
          <Tooltip>
            <TooltipTrigger asChild>
              <Button
                type="button"
                variant="outline"
                size="icon"
                onClick={() =>
                  toast.info(
                    `Đang tải xuống ${deliverable.fileName} (Mock preview)`
                  )
                }
                aria-label={`Tải xuống ${deliverable.fileName}`}
              >
                <Download className="h-4 w-4" strokeWidth={1.75} />
              </Button>
            </TooltipTrigger>
            <TooltipContent>Tải xuống tệp</TooltipContent>
          </Tooltip>
        ) : null}

        {!readOnly ? (
          <>
            <input
              ref={fileInputRef}
              type="file"
              accept=".docx,.pdf,.xlsx,.zip,.pptx,.png,.jpg,.webp"
              className="hidden"
              onChange={(e) => {
                handleFileSelected(e.target.files?.[0]);
                e.target.value = '';
              }}
            />
            <Button
              type="button"
              variant={deliverable.fileName ? 'outline' : 'default'}
              size="sm"
              disabled={uploadMutation.isPending}
              onClick={() => fileInputRef.current?.click()}
            >
              {uploadMutation.isPending ? (
                <Loader2 className="h-3.5 w-3.5 animate-spin" strokeWidth={1.75} />
              ) : (
                <Upload className="h-3.5 w-3.5" strokeWidth={1.75} />
              )}
              <span>{deliverable.fileName ? 'Nộp lại' : 'Tải lên'}</span>
            </Button>
          </>
        ) : null}
      </div>
    </div>
  );
}
