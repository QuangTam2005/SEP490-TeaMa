'use client';

import { useState } from 'react';
import Link from 'next/link';
import { ArrowRight, Layers, Menu } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from '@/components/ui/sheet';
import { ThemeToggle } from '@/components/shared/theme-toggle';

const NAV_LINKS = [
  { href: '#milestones', label: '10 Cột mốc' },
  { href: '#wbs-srs', label: 'WBS & SRS' },
  { href: '#supervisor', label: 'Cổng Giảng viên' },
];

export function LandingHeader() {
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <header className="fixed inset-x-0 top-0 z-40 h-16 border-b bg-card">
      <div className="mx-auto flex h-full max-w-[1280px] items-center justify-between px-4 sm:px-6 lg:px-10">
        <a href="#top" className="skip-link">
          Bỏ qua tới nội dung chính
        </a>

        <Link
          href="/"
          className="inline-flex items-center gap-2.5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 rounded-md"
        >
          <span className="flex h-8 w-8 items-center justify-center rounded-md bg-brand text-white">
            <Layers className="h-4 w-4" strokeWidth={1.75} />
          </span>
          <span className="text-base font-semibold tracking-tight text-foreground">
            TeaMa
          </span>
          <span className="hidden rounded border bg-muted px-1.5 py-0.5 font-mono text-[11px] font-medium text-muted-foreground sm:inline-block">
            SEP490
          </span>
        </Link>

        <nav
          aria-label="Điều hướng chính"
          className="hidden items-center gap-7 md:flex"
        >
          {NAV_LINKS.map((item) => (
            <a
              key={item.href}
              href={item.href}
              className="text-sm font-medium text-muted-foreground transition-colors duration-150 hover:text-foreground"
            >
              {item.label}
            </a>
          ))}
        </nav>

        <div className="hidden items-center gap-2.5 md:flex">
          <ThemeToggle />
          <Button variant="ghost" asChild className="h-9 px-3">
            <Link href="/login">Đăng nhập</Link>
          </Button>
          <Button asChild className="h-9 px-4">
            <Link href="/projects">
              Mở Workspace
              <ArrowRight className="h-4 w-4" strokeWidth={1.75} />
            </Link>
          </Button>
        </div>

        <div className="flex items-center gap-2 md:hidden">
          <ThemeToggle className="h-10 w-10" />
          <Sheet open={mobileOpen} onOpenChange={setMobileOpen}>
            <SheetTrigger asChild>
              <Button
                type="button"
                variant="outline"
                size="icon"
                className="h-10 w-10"
                aria-label="Mở menu điều hướng"
              >
                <Menu className="h-5 w-5" strokeWidth={1.75} />
              </Button>
            </SheetTrigger>
            <SheetContent side="right" className="w-72 p-6">
              <SheetHeader className="border-b pb-4 text-left">
                <SheetTitle className="flex items-center gap-2 text-base">
                  <span className="flex h-7 w-7 items-center justify-center rounded-md bg-brand text-white">
                    <Layers className="h-4 w-4" strokeWidth={1.75} />
                  </span>
                  TeaMa SEP490
                </SheetTitle>
              </SheetHeader>

              <nav
                aria-label="Điều hướng di động"
                className="mt-5 flex flex-col gap-1"
              >
                {NAV_LINKS.map((item) => (
                  <a
                    key={item.href}
                    href={item.href}
                    onClick={() => setMobileOpen(false)}
                    className="flex h-11 items-center rounded-md px-3 text-sm font-medium text-foreground transition-colors hover:bg-muted"
                  >
                    {item.label}
                  </a>
                ))}
              </nav>

              <div className="mt-6 flex flex-col gap-2.5 border-t pt-5">
                <Button variant="outline" asChild className="h-11 w-full">
                  <Link href="/login" onClick={() => setMobileOpen(false)}>
                    Đăng nhập
                  </Link>
                </Button>
                <Button asChild className="h-11 w-full">
                  <Link href="/projects" onClick={() => setMobileOpen(false)}>
                    Mở Workspace
                    <ArrowRight className="h-4 w-4" strokeWidth={1.75} />
                  </Link>
                </Button>
              </div>
            </SheetContent>
          </Sheet>
        </div>
      </div>
    </header>
  );
}
