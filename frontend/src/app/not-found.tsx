import Link from 'next/link';
import { ArrowLeft, FileQuestion } from 'lucide-react';
import { Button } from '@/components/ui/button';

export default function NotFound() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-canvas px-4 py-12 dark:bg-background">
      <div className="w-full max-w-md rounded-lg border bg-card p-8 text-center">
        <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-lg border bg-muted/60 text-muted-foreground">
          <FileQuestion className="h-6 w-6" strokeWidth={1.75} />
        </div>
        <p className="font-mono text-xs font-medium text-brand">404</p>
        <h1 className="mt-1 text-lg font-semibold text-foreground">
          Không tìm thấy trang yêu cầu
        </h1>
        <div className="mt-6 flex items-center justify-center">
          <Button asChild>
            <Link href="/login">
              <ArrowLeft className="h-4 w-4" strokeWidth={1.75} />
              Về trang đăng nhập
            </Link>
          </Button>
        </div>
      </div>
    </main>
  );
}
