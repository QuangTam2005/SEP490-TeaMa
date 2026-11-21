'use client';

import { useMemo, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import {
  ArrowRight,
  FolderKanban,
  GitBranch,
  Layers,
  LogOut,
  Search,
  Settings,
  Users,
} from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { ThemeToggle } from '@/components/shared/theme-toggle';
import { StatusBadge } from '@/components/shared/status-badge';
import { QueryStateBoundary } from '@/components/shared/query-state-boundary';
import { CreateProjectDialog } from '@/components/projects/create-project-dialog';
import { useMockRuntime } from '@/mocks/mock-runtime';
import { projectService } from '@/services';
import { useAuthStore } from '@/store/auth-store';

function ProjectsGridSkeleton() {
  return (
    <div className="grid grid-cols-1 gap-5 md:grid-cols-2 lg:grid-cols-3">
      {[1, 2, 3].map((idx) => (
        <div
          key={idx}
          className="space-y-4 rounded-lg border bg-card p-5"
          aria-busy="true"
        >
          <div className="flex items-center justify-between">
            <div className="h-4 w-24 animate-pulse rounded bg-muted" />
            <div className="h-5 w-16 animate-pulse rounded bg-muted" />
          </div>
          <div className="h-5 w-3/4 animate-pulse rounded bg-muted" />
          <div className="h-2 w-full animate-pulse rounded bg-muted" />
          <div className="flex justify-between pt-2">
            <div className="h-4 w-20 animate-pulse rounded bg-muted" />
            <div className="h-4 w-20 animate-pulse rounded bg-muted" />
          </div>
        </div>
      ))}
    </div>
  );
}

export function ProjectsDirectoryView() {
  const router = useRouter();
  const [searchTerm, setSearchTerm] = useState('');
  const user = useAuthStore((state) => state.user);
  const clearSession = useAuthStore((state) => state.clearSession);
  const mockRuntime = useMockRuntime();

  const projectsQuery = useQuery({
    queryKey: ['projects'],
    queryFn: () => projectService.listProjects(),
  });

  const filteredProjects = useMemo(() => {
    const list = projectsQuery.data ?? [];
    const keyword = searchTerm.trim().toLowerCase();
    if (!keyword) {
      return list;
    }
    return list.filter(
      (p) =>
        p.projectCode.toLowerCase().includes(keyword) ||
        p.projectName.toLowerCase().includes(keyword) ||
        p.groupName.toLowerCase().includes(keyword)
    );
  }, [projectsQuery.data, searchTerm]);

  const isSupervisorMode = mockRuntime.simulatedRole === 'SUPERVISOR';

  const handleLogout = () => {
    clearSession();
    toast.success('Đã đăng xuất');
    router.push('/login');
  };

  return (
    <div className="min-h-screen bg-canvas">
      <header className="sticky top-0 z-30 h-16 border-b bg-card">
        <div className="mx-auto flex h-full max-w-[1280px] items-center justify-between px-4 sm:px-6 lg:px-10">
          <div className="flex items-center gap-3">
            <Link
              href="/"
              className="inline-flex items-center gap-2 rounded-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            >
              <span className="flex h-8 w-8 items-center justify-center rounded-md bg-brand text-white">
                <Layers className="h-4 w-4" strokeWidth={1.75} />
              </span>
              <span className="text-base font-semibold tracking-tight text-foreground">
                TeaMa
              </span>
            </Link>
            <span className="text-muted-foreground">/</span>
            <span className="text-sm font-medium text-foreground">
              Danh sách dự án
            </span>
          </div>

          <div className="flex items-center gap-2.5">
            <ThemeToggle />
            <span className="hidden rounded-md border bg-muted/50 px-2.5 py-1 text-xs font-medium text-foreground sm:inline-block">
              {user?.fullName ?? 'Nguyễn Quang Tâm'}
            </span>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleLogout}
            >
              <LogOut className="h-4 w-4" strokeWidth={1.75} />
              <span className="hidden sm:inline">Đăng xuất</span>
            </Button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-[1280px] px-4 py-8 sm:px-6 lg:px-10">
        <div className="mb-6 flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">
              Dự án đồ án SEP490
            </h1>
            <p className="mt-1 text-sm text-muted-foreground">
              Chọn không gian làm việc của nhóm hoặc khởi tạo dự án mới cho kỳ
              15 tuần.
            </p>
          </div>

          <CreateProjectDialog disabled={isSupervisorMode} />
        </div>

        <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div className="relative w-full sm:max-w-sm">
            <Search
              className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground"
              strokeWidth={1.75}
            />
            <Input
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Tìm theo mã dự án, tên đề tài hoặc nhóm..."
              className="pl-9"
              aria-label="Tìm kiếm dự án"
            />
          </div>
          <Link
            href="/invite?token=valid-new"
            className="text-xs font-medium text-brand hover:underline"
          >
            Xem trước luồng nhận lời mời Email (/invite) &rarr;
          </Link>
        </div>

        <QueryStateBoundary
          data={filteredProjects}
          isLoading={projectsQuery.isLoading}
          isError={projectsQuery.isError}
          error={projectsQuery.error}
          onRetry={() => void projectsQuery.refetch()}
          loadingFallback={<ProjectsGridSkeleton />}
          emptyState={{
            icon: FolderKanban,
            title: searchTerm
              ? 'Không tìm thấy dự án phù hợp'
              : 'Bạn chưa tham gia dự án nào',
            description: searchTerm
              ? 'Thử xóa từ khóa tìm kiếm để hiển thị lại toàn bộ dự án của bạn.'
              : 'Khởi tạo dự án mới để tự động tạo 10 cột mốc OGA, hoặc mở đường dẫn lời mời từ email của Nhóm trưởng.',
            action: searchTerm ? (
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setSearchTerm('')}
              >
                Xóa bộ lọc
              </Button>
            ) : (
              <CreateProjectDialog
                disabled={isSupervisorMode}
                triggerLabel="Tạo dự án đầu tiên"
              />
            ),
          }}
        >
          {(projects) => (
            <div className="grid grid-cols-1 gap-5 md:grid-cols-2 lg:grid-cols-3">
              {projects.map((project) => {
                const progressPercent = Math.round(
                  (project.completedMilestones / project.totalMilestones) * 100
                );

                return (
                  <div
                    key={project.id}
                    className="group flex flex-col justify-between rounded-lg border bg-card p-5 transition-[transform,border-color] duration-150 hover:-translate-y-0.5 hover:border-foreground/25 motion-reduce:transform-none"
                  >
                    <div>
                      <div className="flex items-center justify-between gap-2">
                        <span className="font-mono text-xs font-semibold text-brand">
                          {project.projectCode}
                        </span>
                        <StatusBadge status={project.currentUserRole} />
                      </div>

                      <h2 className="mt-2.5 text-base font-semibold text-foreground">
                        {project.projectName}
                      </h2>
                      <p className="mt-1 text-xs text-muted-foreground">
                        Nhóm {project.groupName}
                        {project.softwareType ? ` • ${project.softwareType}` : ''}
                      </p>

                      <div className="mt-5">
                        <div className="mb-1.5 flex items-center justify-between text-xs">
                          <span className="text-muted-foreground">
                            Tiến độ cột mốc
                          </span>
                          <span className="font-mono font-medium text-foreground tabular-nums">
                            {project.completedMilestones}/
                            {project.totalMilestones} ({progressPercent}%)
                          </span>
                        </div>
                        <div className="h-1.5 w-full overflow-hidden rounded-full bg-muted">
                          <div
                            className="h-full bg-brand transition-all duration-300"
                            style={{ width: `${progressPercent}%` }}
                          />
                        </div>
                      </div>
                    </div>

                    <div className="mt-5 flex items-center justify-between border-t pt-4">
                      <div className="flex items-center gap-3.5 font-mono text-xs text-muted-foreground tabular-nums">
                        <span className="inline-flex items-center gap-1">
                          <Users className="h-3.5 w-3.5" strokeWidth={1.75} />
                          {project.memberCount}
                        </span>
                        <span className="inline-flex items-center gap-1">
                          <GitBranch
                            className="h-3.5 w-3.5"
                            strokeWidth={1.75}
                          />
                          {project.repoCount}
                        </span>
                      </div>

                      <div className="flex items-center gap-1.5">
                        <Button
                          variant="ghost"
                          size="icon"
                          asChild
                          aria-label={`Cài đặt ${project.projectCode}`}
                        >
                          <Link href={`/projects/${project.id}/settings`}>
                            <Settings
                              className="h-4 w-4"
                              strokeWidth={1.75}
                            />
                          </Link>
                        </Button>
                        <Button variant="outline" size="sm" asChild>
                          <Link href={`/projects/${project.id}/overview`}>
                            Mở dự án
                            <ArrowRight
                              className="h-3.5 w-3.5"
                              strokeWidth={1.75}
                            />
                          </Link>
                        </Button>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </QueryStateBoundary>
      </main>
    </div>
  );
}
