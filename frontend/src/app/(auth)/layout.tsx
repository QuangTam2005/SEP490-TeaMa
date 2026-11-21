import type { ReactNode } from 'react';
import Link from 'next/link';
import { CheckCircle2, Layers } from 'lucide-react';
import { ThemeToggle } from '@/components/shared/theme-toggle';

interface AuthLayoutProps {
  children: ReactNode;
}

const HIGHLIGHTS = [
  'Tự động khởi tạo 10 SEP490 Milestones (OGA 50%) & 5 Quality Objectives',
  'Mời thành viên qua Email bảo mật (MailSender) — hỗ trợ mọi domain email',
  'Truy vết hai chiều Feature → Use Case → Screen Auth → WBS → Test Evidence',
  'Cổng Giảng viên chỉ đọc qua Magic Link, xuất Excel 5 sheets & SRS .md',
];

export default function AuthLayout({ children }: AuthLayoutProps) {
  return (
    <div className="min-h-screen bg-canvas">
      <header className="h-16 border-b bg-card">
        <div className="mx-auto flex h-full max-w-[1280px] items-center justify-between px-4 sm:px-6 lg:px-10">
          <Link
            href="/"
            className="inline-flex items-center gap-2.5 rounded-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
          >
            <span className="flex h-8 w-8 items-center justify-center rounded-md bg-brand text-white">
              <Layers className="h-4 w-4" strokeWidth={1.75} />
            </span>
            <span className="text-base font-semibold tracking-tight text-foreground">
              TeaMa
            </span>
            <span className="rounded border bg-muted px-1.5 py-0.5 font-mono text-[11px] font-medium text-muted-foreground">
              SEP490
            </span>
          </Link>

          <div className="flex items-center gap-2">
            <ThemeToggle />
          </div>
        </div>
      </header>

      <main className="mx-auto grid min-h-[calc(100svh-4rem)] max-w-[1280px] grid-cols-1 items-center gap-10 px-4 py-10 sm:px-6 lg:grid-cols-12 lg:px-10">
        <div className="flex justify-center lg:col-span-6">{children}</div>

        <aside className="hidden lg:col-span-6 lg:block">
          <div className="rounded-lg border bg-card p-8">
            <p className="font-mono text-xs font-medium text-brand">
              SEP490 Capstone Copilot
            </p>
            <h2 className="mt-2 text-2xl font-bold tracking-tight text-foreground">
              Một workspace thống nhất cho 15 tuần đồ án tốt nghiệp.
            </h2>
            <p className="mt-2.5 text-sm leading-relaxed text-muted-foreground">
              Đồng bộ tài liệu đặc tả, tiến độ code đa repository và kiểm thử
              chất lượng theo đúng khung đánh giá SEP490.
            </p>

            <ul className="mt-6 space-y-3 border-t pt-6 text-sm text-foreground">
              {HIGHLIGHTS.map((text) => (
                <li key={text} className="flex items-start gap-2.5">
                  <CheckCircle2
                    className="mt-0.5 h-4 w-4 shrink-0 text-brand"
                    strokeWidth={1.75}
                  />
                  <span>{text}</span>
                </li>
              ))}
            </ul>
          </div>
        </aside>
      </main>
    </div>
  );
}
