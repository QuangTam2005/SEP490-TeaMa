'use client';

import type { ReactNode } from 'react';
import { ErrorStateCard } from './error-state-card';
import { EmptyState, type EmptyStateProps } from './empty-state';

export interface QueryStateBoundaryProps<T> {
  data: T | undefined;
  isLoading: boolean;
  isError: boolean;
  error: unknown;
  onRetry?: () => void;
  isEmpty?: (data: T) => boolean;
  loadingFallback?: ReactNode;
  emptyState?: EmptyStateProps;
  errorTitle?: string;
  children: (data: T) => ReactNode;
}

function DefaultSkeleton() {
  return (
    <div className="space-y-3 rounded-lg border bg-card p-5" aria-busy="true">
      <div className="h-4 w-1/3 animate-pulse rounded bg-muted" />
      <div className="h-3 w-2/3 animate-pulse rounded bg-muted" />
      <div className="grid grid-cols-1 gap-3 pt-2 sm:grid-cols-3">
        <div className="h-16 animate-pulse rounded-md bg-muted" />
        <div className="h-16 animate-pulse rounded-md bg-muted" />
        <div className="h-16 animate-pulse rounded-md bg-muted" />
      </div>
    </div>
  );
}

/**
 * Standardizes Loading -> Error -> Empty -> Populated states across all TanStack Query views.
 */
export function QueryStateBoundary<T>({
  data,
  isLoading,
  isError,
  error,
  onRetry,
  isEmpty,
  loadingFallback,
  emptyState,
  errorTitle,
  children,
}: QueryStateBoundaryProps<T>) {
  if (isLoading) {
    return <>{loadingFallback ?? <DefaultSkeleton />}</>;
  }

  if (isError) {
    return (
      <ErrorStateCard error={error} title={errorTitle} onRetry={onRetry} />
    );
  }

  if (data === undefined) {
    return (
      <EmptyState
        title={emptyState?.title ?? 'Chưa có dữ liệu'}
        description={
          emptyState?.description ??
          'Hiện chưa có bản ghi nào được tạo trong mục này.'
        }
        icon={emptyState?.icon}
        action={emptyState?.action}
      />
    );
  }

  const empty = isEmpty
    ? isEmpty(data)
    : Array.isArray(data) && data.length === 0;

  if (empty && emptyState) {
    return <EmptyState {...emptyState} />;
  }

  return <>{children(data)}</>;
}
