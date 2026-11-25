import type { ReactNode } from 'react';
import Link from 'next/link';
import { BookOpen } from 'lucide-react';
import { ThemeToggle } from '@/components/shared/theme-toggle';

interface AuthLayoutProps {
  children: ReactNode;
}

export default function AuthLayout({ children }: AuthLayoutProps) {
  return (
    <div className="min-h-screen bg-canvas">
      <header className="h-14 border-b bg-card">
        <div className="mx-auto flex h-full max-w-[1280px] items-center justify-between px-4 sm:px-6 lg:px-10">
          <Link
            href="/login"
            className="inline-flex items-center gap-2 rounded-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
          >
            <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-brand-strong text-white">
              <BookOpen className="h-4 w-4" strokeWidth={1.75} />
            </span>
            <span className="text-base font-semibold tracking-tight text-foreground">
              Study Companion
            </span>
          </Link>

          <ThemeToggle />
        </div>
      </header>

      <main className="mx-auto flex min-h-[calc(100svh-3.5rem)] max-w-[1280px] items-center justify-center px-4 py-10 sm:px-6">
        {children}
      </main>
    </div>
  );
}
