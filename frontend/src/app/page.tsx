import Link from 'next/link';
import {
  ArrowRight,
  CalendarRange,
  CheckCircle2,
  FileCode2,
  GitPullRequest,
  Layers,
  ShieldCheck,
} from 'lucide-react';

export default function HomePage() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center bg-canvas px-4 py-12 dark:bg-background">
      <div className="w-full max-w-2xl rounded-lg border bg-card p-8 shadow-sm">
        <div className="mb-6 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-brand text-white">
              <Layers className="h-5 w-5" strokeWidth={1.75} />
            </div>
            <div>
              <h1 className="text-2xl font-bold tracking-tight">
                SEP490 Capstone Copilot
              </h1>
              <p className="font-mono text-xs text-muted-foreground">
                SEP490-TeaMa • FPT University
              </p>
            </div>
          </div>
          <span className="inline-flex items-center gap-1.5 rounded-md bg-brand-soft px-2.5 py-1 font-mono text-xs font-medium text-brand">
            <CheckCircle2 className="h-4 w-4" strokeWidth={1.75} />
            MVP Ready
          </span>
        </div>

        <p className="mb-6 text-sm text-muted-foreground">
          Không gian quản trị đồ án 15 tuần: 10 Milestones OGA, WBS &amp; Complexity,
          SRS Traceability, Multi-Repo GitHub Sync và xuất báo cáo chuẩn FPTU.
        </p>

        <div className="mb-8 grid grid-cols-1 gap-3 sm:grid-cols-3">
          <div className="flex items-center gap-2.5 rounded-md border p-3">
            <CalendarRange className="h-4 w-4 shrink-0 text-brand" strokeWidth={1.75} />
            <span className="text-sm font-medium">10 Milestones</span>
          </div>
          <div className="flex items-center gap-2.5 rounded-md border p-3">
            <FileCode2 className="h-4 w-4 shrink-0 text-brand" strokeWidth={1.75} />
            <span className="text-sm font-medium">SRS &amp; Test Hub</span>
          </div>
          <div className="flex items-center gap-2.5 rounded-md border p-3">
            <GitPullRequest className="h-4 w-4 shrink-0 text-brand" strokeWidth={1.75} />
            <span className="text-sm font-medium">Multi-Repo PR</span>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <Link
            href="/login"
            className="inline-flex items-center gap-2 rounded-md bg-brand px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-brand-hover"
          >
            <ShieldCheck className="h-4 w-4" strokeWidth={1.75} />
            Đăng nhập
            <ArrowRight className="h-4 w-4" strokeWidth={1.75} />
          </Link>
          <Link
            href="/projects"
            className="inline-flex items-center gap-2 rounded-md border bg-card px-4 py-2 text-sm font-medium transition-colors hover:bg-muted"
          >
            Workspace
          </Link>
        </div>
      </div>
    </main>
  );
}
