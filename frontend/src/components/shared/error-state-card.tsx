'use client';

import type { ReactNode } from 'react';
import {
  AlertTriangle,
  FileQuestion,
  Lock,
  RefreshCw,
  ServerCrash,
  WifiOff,
} from 'lucide-react';
import { ApiClientError } from '@/lib/api-client';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

export interface ErrorStateCardProps {
  error: unknown;
  title?: string;
  onRetry?: () => void;
  action?: ReactNode;
  className?: string;
}

interface ErrorVisual {
  icon: typeof AlertTriangle;
  defaultTitle: string;
  toneClass: string;
}

function resolveErrorVisual(error: unknown): ErrorVisual {
  if (error instanceof ApiClientError) {
    if (error.status === 0) {
      return {
        icon: WifiOff,
        defaultTitle: 'Mất kết nối tới máy chủ',
        toneClass: 'text-amber-600 dark:text-amber-400 bg-amber-50 dark:bg-amber-950/40',
      };
    }
    if (error.status === 403 || error.status === 401) {
      return {
        icon: Lock,
        defaultTitle: 'Không có quyền truy cập',
        toneClass: 'text-rose-600 dark:text-rose-400 bg-rose-50 dark:bg-rose-950/40',
      };
    }
    if (error.status === 404 || error.status === 410) {
      return {
        icon: FileQuestion,
        defaultTitle: 'Không tìm thấy dữ liệu',
        toneClass: 'text-slate-600 dark:text-slate-300 bg-slate-100 dark:bg-slate-800',
      };
    }
    if (error.status >= 500) {
      return {
        icon: ServerCrash,
        defaultTitle: 'Hệ thống đang gặp sự cố',
        toneClass: 'text-rose-600 dark:text-rose-400 bg-rose-50 dark:bg-rose-950/40',
      };
    }
  }

  return {
    icon: AlertTriangle,
    defaultTitle: 'Không thể tải dữ liệu',
    toneClass: 'text-amber-600 dark:text-amber-400 bg-amber-50 dark:bg-amber-950/40',
  };
}

export function ErrorStateCard({
  error,
  title,
  onRetry,
  action,
  className,
}: ErrorStateCardProps) {
  const visual = resolveErrorVisual(error);
  const Icon = visual.icon;
  const apiError = error instanceof ApiClientError ? error : null;
  const message =
    apiError?.message ??
    (error instanceof Error
      ? error.message
      : 'Đã xảy ra lỗi khi tải dữ liệu. Vui lòng thử lại.');

  return (
    <div
      role="alert"
      className={cn(
        'flex flex-col items-center justify-center rounded-lg border bg-card px-6 py-10 text-center',
        className
      )}
    >
      <div
        className={cn(
          'mb-4 flex h-11 w-11 items-center justify-center rounded-lg',
          visual.toneClass
        )}
      >
        <Icon className="h-5 w-5" strokeWidth={1.75} />
      </div>

      <h3 className="text-base font-semibold text-foreground">
        {title ?? visual.defaultTitle}
      </h3>
      <p className="mt-1.5 max-w-md text-sm text-muted-foreground">{message}</p>

      {apiError?.errorCode ? (
        <span className="mt-3 inline-flex items-center rounded border bg-muted/60 px-2 py-0.5 font-mono text-xs text-muted-foreground">
          {apiError.errorCode} • HTTP {apiError.status}
        </span>
      ) : null}

      <div className="mt-5 flex flex-wrap items-center justify-center gap-2.5">
        {onRetry ? (
          <Button type="button" variant="outline" size="sm" onClick={onRetry}>
            <RefreshCw className="h-4 w-4" strokeWidth={1.75} />
            Thử lại
          </Button>
        ) : null}
        {action}
      </div>
    </div>
  );
}
