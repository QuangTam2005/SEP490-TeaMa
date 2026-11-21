'use client';

import { useEffect } from 'react';
import Link from 'next/link';
import { Home, RefreshCw, ServerCrash } from 'lucide-react';
import { Button } from '@/components/ui/button';

interface GlobalErrorProps {
  error: Error & { digest?: string };
  reset: () => void;
}

export default function GlobalError({ error, reset }: GlobalErrorProps) {
  useEffect(() => {
    console.error('Unhandled route error:', error);
  }, [error]);

  return (
    <main className="flex min-h-screen items-center justify-center bg-canvas px-4 py-12 dark:bg-background">
      <div className="w-full max-w-md rounded-lg border bg-card p-8 text-center">
        <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-lg bg-rose-50 text-rose-600 dark:bg-rose-950/40 dark:text-rose-400">
          <ServerCrash className="h-6 w-6" strokeWidth={1.75} />
        </div>
        <h1 className="text-lg font-semibold text-foreground">
          Đã xảy ra lỗi hiển thị
        </h1>
        <p className="mt-2 text-sm text-muted-foreground">
          {error.message ||
            'Hệ thống gặp sự cố ngoài dự kiến khi tải trang này.'}
        </p>
        {error.digest ? (
          <p className="mt-2 font-mono text-xs text-muted-foreground">
            Digest: {error.digest}
          </p>
        ) : null}
        <div className="mt-6 flex items-center justify-center gap-2.5">
          <Button type="button" onClick={reset}>
            <RefreshCw className="h-4 w-4" strokeWidth={1.75} />
            Thử tải lại
          </Button>
          <Button variant="outline" asChild>
            <Link href="/">
              <Home className="h-4 w-4" strokeWidth={1.75} />
              Về trang chủ
            </Link>
          </Button>
        </div>
      </div>
    </main>
  );
}
