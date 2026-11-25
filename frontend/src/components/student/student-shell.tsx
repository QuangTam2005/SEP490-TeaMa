'use client';

import { useState, type ReactNode } from 'react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import {
  BookCheck,
  BookOpen,
  CalendarDays,
  KeyRound,
  LogOut,
  Menu,
  X,
} from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { ThemeToggle } from '@/components/shared/theme-toggle';
import { useAuthStore } from '@/store/auth-store';
import { cn } from '@/lib/utils';

const STUDENT_NAV_ITEMS = [
  {
    href: '/student/today',
    label: 'Bài học ngày mai',
    icon: BookCheck,
  },
  {
    href: '/student/timetable',
    label: 'Thời khóa biểu',
    icon: CalendarDays,
  },
] as const;

interface StudentShellProps {
  children: ReactNode;
}

export function StudentShell({ children }: StudentShellProps) {
  const pathname = usePathname();
  const router = useRouter();
  const user = useAuthStore((state) => state.user);
  const clearSession = useAuthStore((state) => state.clearSession);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const handleLogout = () => {
    clearSession();
    router.push('/login');
  };

  return (
    <div className="min-h-screen bg-canvas">
      <a href="#student-main-content" className="skip-link">
        Chuyển tới nội dung chính
      </a>

      <header className="sticky top-0 z-30 border-b bg-card">
        <div className="mx-auto flex h-16 max-w-[1200px] items-center justify-between gap-3 px-4 sm:px-6 lg:px-8">
          <div className="flex items-center gap-3">
            <Link
              href="/student/today"
              className="inline-flex items-center gap-2.5 rounded-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            >
              <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-brand-strong text-white">
                <BookOpen className="h-4 w-4" strokeWidth={1.75} />
              </span>
              <span className="text-base font-bold tracking-tight text-foreground">
                Study Companion
              </span>
            </Link>
            <Badge variant="brand" className="hidden sm:inline-flex">
              {user?.fullName ?? 'Nguyễn Minh Khôi'} · Lớp 6
            </Badge>
          </div>

          <nav
            aria-label="Điều hướng học sinh"
            className="hidden items-center gap-1.5 md:flex"
          >
            {STUDENT_NAV_ITEMS.map((item) => {
              const Icon = item.icon;
              const active = pathname.startsWith(item.href);
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={cn(
                    'inline-flex h-10 items-center gap-2 rounded-lg px-4 text-sm transition-colors',
                    active
                      ? 'bg-brand-soft font-semibold text-brand-strong dark:text-sky-300'
                      : 'font-medium text-muted-foreground hover:bg-muted hover:text-foreground'
                  )}
                >
                  <Icon className="h-4 w-4" strokeWidth={1.75} />
                  <span>{item.label}</span>
                </Link>
              );
            })}
          </nav>

          <div className="flex items-center gap-2">
            <Link
              href="/link-student"
              className="hidden h-10 items-center gap-1.5 rounded-lg border bg-background px-3 text-xs font-semibold text-foreground transition-colors hover:bg-muted sm:inline-flex"
            >
              <KeyRound className="h-3.5 w-3.5 text-brand-strong dark:text-sky-400" strokeWidth={1.75} />
              <span>Mã liên kết</span>
            </Link>

            <ThemeToggle />

            <Button
              type="button"
              variant="ghost"
              size="icon"
              onClick={handleLogout}
              aria-label="Đăng xuất"
              className="hidden h-10 w-10 text-muted-foreground hover:text-foreground sm:inline-flex"
            >
              <LogOut className="h-4 w-4" strokeWidth={1.75} />
            </Button>

            <Button
              type="button"
              variant="outline"
              size="icon"
              className="h-10 w-10 md:hidden"
              onClick={() => setMobileMenuOpen((prev) => !prev)}
              aria-expanded={mobileMenuOpen}
              aria-label="Mở menu điều hướng"
            >
              {mobileMenuOpen ? (
                <X className="h-5 w-5" strokeWidth={1.75} />
              ) : (
                <Menu className="h-5 w-5" strokeWidth={1.75} />
              )}
            </Button>
          </div>
        </div>

        {mobileMenuOpen ? (
          <div className="border-t bg-card px-4 py-3 md:hidden">
            <div className="space-y-2">
              {STUDENT_NAV_ITEMS.map((item) => {
                const Icon = item.icon;
                const active = pathname.startsWith(item.href);
                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    onClick={() => setMobileMenuOpen(false)}
                    className={cn(
                      'flex h-11 items-center gap-2.5 rounded-lg px-3.5 text-sm',
                      active
                        ? 'bg-brand-soft font-semibold text-brand-strong dark:text-sky-300'
                        : 'font-medium text-foreground hover:bg-muted'
                    )}
                  >
                    <Icon className="h-4 w-4" strokeWidth={1.75} />
                    <span>{item.label}</span>
                  </Link>
                );
              })}

              <Link
                href="/link-student"
                onClick={() => setMobileMenuOpen(false)}
                className="flex h-11 items-center gap-2.5 rounded-lg border px-3.5 text-sm font-medium text-foreground hover:bg-muted"
              >
                <KeyRound className="h-4 w-4 text-brand-strong" strokeWidth={1.75} />
                <span>Mã liên kết Phụ huynh</span>
              </Link>

              <button
                type="button"
                onClick={handleLogout}
                className="flex h-11 w-full items-center gap-2.5 rounded-lg px-3.5 text-sm font-medium text-rose-600 hover:bg-rose-50 dark:text-rose-400 dark:hover:bg-rose-950/40"
              >
                <LogOut className="h-4 w-4" strokeWidth={1.75} />
                <span>Đăng xuất</span>
              </button>
            </div>
          </div>
        ) : null}
      </header>

      <main
        id="student-main-content"
        className="mx-auto max-w-[1200px] px-4 py-6 sm:px-6 sm:py-8 lg:px-8"
      >
        {children}
      </main>
    </div>
  );
}
