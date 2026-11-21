'use client';

import { useMemo, useState, type ReactNode } from 'react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import {
  BarChart3,
  CalendarRange,
  ChevronLeft,
  ChevronRight,
  ClipboardList,
  FileCode2,
  FlaskConical,
  Kanban,
  Layers,
  LayoutDashboard,
  LogOut,
  Menu,
  Settings,
} from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from '@/components/ui/sheet';
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import { ThemeToggle } from '@/components/shared/theme-toggle';
import { StatusBadge } from '@/components/shared/status-badge';
import { projectService } from '@/services';
import { useAuthStore } from '@/store/auth-store';
import { useMockRuntime } from '@/mocks/mock-runtime';
import { cn } from '@/lib/utils';

interface WorkspaceShellProps {
  projectId: number;
  children: ReactNode;
}

interface NavItem {
  segment: string;
  label: string;
  icon: typeof LayoutDashboard;
  group: 'core' | 'engineering' | 'ops';
}

const NAV_ITEMS: NavItem[] = [
  {
    segment: 'overview',
    label: 'Tổng quan & 10 Mốc',
    icon: LayoutDashboard,
    group: 'core',
  },
  {
    segment: 'planning',
    label: 'Kế hoạch (Report 2)',
    icon: CalendarRange,
    group: 'engineering',
  },
  {
    segment: 'srs',
    label: 'SRS & Truy vết (R3)',
    icon: FileCode2,
    group: 'engineering',
  },
  {
    segment: 'execution',
    label: 'Thực thi & Kanban',
    icon: Kanban,
    group: 'engineering',
  },
  {
    segment: 'testing',
    label: 'Kiểm thử & QA (R5)',
    icon: FlaskConical,
    group: 'engineering',
  },
  {
    segment: 'tracking',
    label: 'Sổ theo dõi (Tracking)',
    icon: ClipboardList,
    group: 'ops',
  },
  {
    segment: 'analytics',
    label: 'Phân tích & Xuất file',
    icon: BarChart3,
    group: 'ops',
  },
  {
    segment: 'settings',
    label: 'Cài đặt & Thành viên',
    icon: Settings,
    group: 'ops',
  },
];

const GROUP_LABELS: Record<NavItem['group'], string> = {
  core: 'Điều hành',
  engineering: 'Kỹ thuật & Tài liệu',
  ops: 'Vận hành & Quản trị',
};

function computeCurrentWeek(startDate?: string): number {
  if (!startDate) {
    return 5;
  }
  const startMs = Date.parse(startDate);
  if (Number.isNaN(startMs)) {
    return 5;
  }
  const diffDays = Math.floor((Date.now() - startMs) / 86_400_000);
  const week = Math.floor(diffDays / 7) + 1;
  return Math.min(15, Math.max(1, week));
}

export function WorkspaceShell({ projectId, children }: WorkspaceShellProps) {
  const pathname = usePathname();
  const router = useRouter();
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);

  const user = useAuthStore((state) => state.user);
  const clearSession = useAuthStore((state) => state.clearSession);
  const mockRuntime = useMockRuntime();

  const projectsQuery = useQuery({
    queryKey: ['projects'],
    queryFn: () => projectService.listProjects(),
  });

  const projectQuery = useQuery({
    queryKey: ['project', projectId],
    queryFn: () => projectService.getProject(projectId),
    enabled: Number.isFinite(projectId) && projectId > 0,
  });

  const activeNavItem = useMemo(() => {
    return (
      NAV_ITEMS.find((item) =>
        pathname.startsWith(`/projects/${projectId}/${item.segment}`)
      ) ?? NAV_ITEMS[0]
    );
  }, [pathname, projectId]);

  const currentProject = projectQuery.data;
  const currentWeek = computeCurrentWeek(currentProject?.startDate);

  const handleProjectSwitch = (nextProjectId: string) => {
    if (nextProjectId === 'all') {
      router.push('/projects');
      return;
    }
    router.push(`/projects/${nextProjectId}/${activeNavItem.segment}`);
  };

  const handleLogout = () => {
    clearSession();
    toast.success('Đã đăng xuất');
    router.push('/login');
  };

  const renderSidebarNav = (isCompact: boolean, onNavigate?: () => void) => {
    const groups: NavItem['group'][] = ['core', 'engineering', 'ops'];

    return (
      <nav aria-label="Điều hướng dự án" className="flex-1 overflow-y-auto px-2.5 py-3">
        {groups.map((groupKey) => {
          const items = NAV_ITEMS.filter((item) => item.group === groupKey);
          return (
            <div key={groupKey} className="mb-4 last:mb-0">
              {!isCompact ? (
                <p className="mb-1.5 px-2.5 text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
                  {GROUP_LABELS[groupKey]}
                </p>
              ) : (
                <div className="my-2 border-t" />
              )}

              <div className="space-y-0.5">
                {items.map((item) => {
                  const Icon = item.icon;
                  const href = `/projects/${projectId}/${item.segment}`;
                  const isActive = pathname.startsWith(href);

                  const linkNode = (
                    <Link
                      key={item.segment}
                      href={href}
                      onClick={onNavigate}
                      className={cn(
                        'flex h-9 items-center gap-2.5 rounded-md px-2.5 text-sm font-medium transition-colors duration-150',
                        isCompact && 'justify-center px-0',
                        isActive
                          ? 'bg-brand-soft text-brand'
                          : 'text-muted-foreground hover:bg-muted hover:text-foreground'
                      )}
                      aria-current={isActive ? 'page' : undefined}
                    >
                      <Icon className="h-4 w-4 shrink-0" strokeWidth={1.75} />
                      {!isCompact ? (
                        <span className="truncate">{item.label}</span>
                      ) : null}
                    </Link>
                  );

                  if (isCompact) {
                    return (
                      <Tooltip key={item.segment}>
                        <TooltipTrigger asChild>{linkNode}</TooltipTrigger>
                        <TooltipContent side="right">{item.label}</TooltipContent>
                      </Tooltip>
                    );
                  }

                  return linkNode;
                })}
              </div>
            </div>
          );
        })}
      </nav>
    );
  };

  return (
    <div className="flex min-h-screen bg-canvas">
      {/* Desktop Fixed Sidebar */}
      <aside
        className={cn(
          'hidden shrink-0 flex-col border-r bg-card transition-[width] duration-150 lg:flex',
          collapsed ? 'w-16' : 'w-[260px]'
        )}
      >
        {/* Brand & Project Switcher */}
        <div className="flex h-14 items-center justify-between border-b px-3">
          <Link
            href="/projects"
            className={cn(
              'inline-flex items-center gap-2 rounded-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
              collapsed && 'mx-auto'
            )}
          >
            <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-md bg-brand text-white">
              <Layers className="h-4 w-4" strokeWidth={1.75} />
            </span>
            {!collapsed ? (
              <span className="text-sm font-semibold tracking-tight text-foreground">
                TeaMa
              </span>
            ) : null}
          </Link>

          {!collapsed ? (
            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  onClick={() => setCollapsed(true)}
                  aria-label="Thu gọn thanh điều hướng"
                >
                  <ChevronLeft className="h-4 w-4" strokeWidth={1.75} />
                </Button>
              </TooltipTrigger>
              <TooltipContent>Thu gọn Sidebar</TooltipContent>
            </Tooltip>
          ) : null}
        </div>

        {!collapsed ? (
          <div className="border-b px-3 py-2.5">
            <label
              htmlFor="sidebar-project-switcher"
              className="mb-1 block text-[11px] font-medium text-muted-foreground"
            >
              Dự án hiện tại
            </label>
            <select
              id="sidebar-project-switcher"
              value={String(projectId)}
              onChange={(e) => handleProjectSwitch(e.target.value)}
              className="h-9 w-full rounded-md border bg-background px-2.5 font-mono text-xs font-medium text-foreground focus:outline-none focus:ring-2 focus:ring-ring"
            >
              {(projectsQuery.data ?? []).map((p) => (
                <option key={p.id} value={String(p.id)}>
                  {p.projectCode} • {p.groupName}
                </option>
              ))}
              <option value="all">← Tất cả dự án</option>
            </select>
          </div>
        ) : (
          <div className="flex justify-center border-b py-2">
            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  onClick={() => setCollapsed(false)}
                  aria-label="Mở rộng thanh điều hướng"
                >
                  <ChevronRight className="h-4 w-4" strokeWidth={1.75} />
                </Button>
              </TooltipTrigger>
              <TooltipContent side="right">Mở rộng Sidebar</TooltipContent>
            </Tooltip>
          </div>
        )}

        {renderSidebarNav(collapsed)}

        {/* Sidebar Footer */}
        <div className="border-t p-3">
          {!collapsed ? (
            <div className="flex items-center justify-between gap-2">
              <div className="min-w-0">
                <p className="truncate text-xs font-semibold text-foreground">
                  {user?.fullName ?? 'Nguyễn Quang Tâm'}
                </p>
                <div className="mt-1">
                  {mockRuntime.simulatedRole === 'SUPERVISOR' ? (
                    <span className="inline-flex rounded border bg-amber-50 px-1.5 py-0.5 font-mono text-[10px] font-medium text-amber-800 dark:bg-amber-950 dark:text-amber-300">
                      SUPERVISOR
                    </span>
                  ) : (
                    <StatusBadge
                      status={currentProject?.currentUserRole ?? 'LEADER'}
                    />
                  )}
                </div>
              </div>
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    onClick={handleLogout}
                    aria-label="Đăng xuất"
                  >
                    <LogOut className="h-4 w-4" strokeWidth={1.75} />
                  </Button>
                </TooltipTrigger>
                <TooltipContent>Đăng xuất</TooltipContent>
              </Tooltip>
            </div>
          ) : (
            <div className="flex justify-center">
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    onClick={handleLogout}
                    aria-label="Đăng xuất"
                  >
                    <LogOut className="h-4 w-4" strokeWidth={1.75} />
                  </Button>
                </TooltipTrigger>
                <TooltipContent side="right">Đăng xuất</TooltipContent>
              </Tooltip>
            </div>
          )}
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-30 flex h-14 items-center justify-between gap-3 border-b bg-card px-4 sm:px-6">
          <div className="flex min-w-0 items-center gap-2.5">
            {/* Mobile Drawer Trigger */}
            <Sheet open={mobileOpen} onOpenChange={setMobileOpen}>
              <SheetTrigger asChild>
                <Button
                  type="button"
                  variant="outline"
                  size="icon"
                  className="lg:hidden"
                  aria-label="Mở menu dự án"
                >
                  <Menu className="h-4 w-4" strokeWidth={1.75} />
                </Button>
              </SheetTrigger>
              <SheetContent side="left" className="flex w-72 flex-col p-0">
                <SheetHeader className="border-b px-4 py-3.5 text-left">
                  <SheetTitle className="flex items-center gap-2 text-sm">
                    <span className="flex h-7 w-7 items-center justify-center rounded-md bg-brand text-white">
                      <Layers className="h-4 w-4" strokeWidth={1.75} />
                    </span>
                    <span>
                      {currentProject?.projectCode ?? `Project #${projectId}`}
                    </span>
                  </SheetTitle>
                </SheetHeader>
                <div className="border-b px-3 py-2.5">
                  <select
                    aria-label="Chuyển dự án"
                    value={String(projectId)}
                    onChange={(e) => {
                      setMobileOpen(false);
                      handleProjectSwitch(e.target.value);
                    }}
                    className="h-9 w-full rounded-md border bg-background px-2.5 font-mono text-xs font-medium text-foreground"
                  >
                    {(projectsQuery.data ?? []).map((p) => (
                      <option key={p.id} value={String(p.id)}>
                        {p.projectCode} • {p.groupName}
                      </option>
                    ))}
                    <option value="all">← Tất cả dự án</option>
                  </select>
                </div>
                {renderSidebarNav(false, () => setMobileOpen(false))}
              </SheetContent>
            </Sheet>

            {/* Breadcrumb */}
            <div className="flex min-w-0 items-center gap-1.5 text-sm">
              <Link
                href="/projects"
                className="hidden text-muted-foreground hover:text-foreground sm:inline"
              >
                Dự án
              </Link>
              <span className="hidden text-muted-foreground sm:inline">/</span>
              <span className="truncate font-mono text-xs font-semibold text-brand">
                {currentProject?.projectCode ?? `#${projectId}`}
              </span>
              <span className="text-muted-foreground">/</span>
              <span className="truncate font-medium text-foreground">
                {activeNavItem.label}
              </span>
            </div>
          </div>

          <div className="flex shrink-0 items-center gap-2">
            <span className="hidden items-center gap-1.5 rounded-md border bg-muted/50 px-2.5 py-1 font-mono text-xs font-medium text-foreground tabular-nums md:inline-flex">
              W{String(currentWeek).padStart(2, '0')} / 15
            </span>
            <ThemeToggle />
          </div>
        </header>

        <main className="mx-auto w-full max-w-[1280px] flex-1 px-4 py-6 sm:px-6 lg:px-8">
          {children}
        </main>
      </div>
    </div>
  );
}
