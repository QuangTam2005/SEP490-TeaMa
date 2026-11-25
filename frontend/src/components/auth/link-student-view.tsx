'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  ArrowLeft,
  Check,
  CheckCircle2,
  Copy,
  KeyRound,
  Link2,
  Loader2,
  RefreshCw,
  ShieldCheck,
} from 'lucide-react';
import { toast } from 'sonner';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { BrandLogo } from '@/components/shared/brand-logo';
import { QueryStateBoundary } from '@/components/shared/query-state-boundary';
import { ThemeToggle } from '@/components/shared/theme-toggle';
import { extractApiErrorMessage } from '@/components/shared/form-error-utils';
import { useMockRuntime } from '@/mocks/mock-runtime';
import { IS_MOCK_MODE, authService, linkService } from '@/services';
import { useAuthStore } from '@/store/auth-store';
import type { UserRole } from '@/types';

export function LinkStudentView() {
  const queryClient = useQueryClient();
  const user = useAuthStore((state) => state.user);
  const isHydrated = useAuthStore((state) => state.isHydrated);
  const syncUser = useAuthStore((state) => state.syncUser);
  const runtime = useMockRuntime();
  const [codeInput, setCodeInput] = useState('');
  const [copied, setCopied] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const isAuthReady = IS_MOCK_MODE || (isHydrated && user !== null);
  const activeRole: UserRole = IS_MOCK_MODE
    ? runtime.simulatedRole
    : user?.role ?? 'STUDENT';

  const homeHref =
    activeRole === 'GUARDIAN' ? '/guardian/dashboard' : '/student/today';

  const linkQuery = useQuery({
    queryKey: ['student-link-code', activeRole, runtime.resetCounter, runtime.scenario],
    queryFn: () => linkService.getCurrentLink(),
    enabled: isAuthReady,
  });

  const generateMutation = useMutation({
    mutationFn: () => linkService.generateLinkCode(),
    onSuccess: (updated) => {
      queryClient.setQueryData(
        ['student-link-code', activeRole, runtime.resetCounter, runtime.scenario],
        updated
      );
      toast.success(`Đã tạo mã mới: ${updated.linkCode}`);
    },
    onError: (err) => {
      toast.error(extractApiErrorMessage(err));
    },
  });

  const acceptMutation = useMutation({
    mutationFn: (code: string) => linkService.acceptLinkCode(code),
    onSuccess: async (linked) => {
      setSubmitError(null);
      setCodeInput('');
      queryClient.setQueryData(
        ['student-link-code', activeRole, runtime.resetCounter, runtime.scenario],
        linked
      );
      await queryClient.invalidateQueries({ queryKey: ['linked-children'] });
      await queryClient.invalidateQueries({ queryKey: ['guardian-dashboard'] });
      await queryClient.invalidateQueries({ queryKey: ['guardian-weekly'] });
      await queryClient.invalidateQueries({ queryKey: ['student-timetable'] });
      if (!IS_MOCK_MODE) {
        try {
          const refreshed = await authService.getCurrentUser();
          syncUser(refreshed);
        } catch {
          // Ignore silent refresh failure
        }
      }
      toast.success(`Đã liên kết với ${linked.linkedUserName ?? 'học sinh'}`);
    },
    onError: (err) => {
      const msg = extractApiErrorMessage(err);
      setSubmitError(msg);
      toast.error(msg);
    },
  });

  const handleCopy = async (code: string) => {
    try {
      await navigator.clipboard.writeText(code);
      setCopied(true);
      toast.success('Đã sao chép mã liên kết');
      setTimeout(() => setCopied(false), 1800);
    } catch {
      toast.error('Không thể sao chép tự động, vui lòng chép thủ công');
    }
  };

  const handleAcceptSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitError(null);
    acceptMutation.mutate(codeInput);
  };

  return (
    <div className="min-h-screen bg-canvas">
      <header className="h-16 border-b bg-card">
        <div className="mx-auto flex h-full max-w-[1600px] items-center justify-between px-4 sm:px-6 lg:px-8 xl:px-10 2xl:px-12">
          <div className="flex items-center gap-3">
            <Link
              href={homeHref}
              className="inline-flex h-10 items-center gap-2 rounded-md border bg-background px-3 text-sm font-medium text-foreground transition-colors hover:bg-muted"
            >
              <ArrowLeft className="h-4 w-4" strokeWidth={1.75} />
              <span>Quay lại</span>
            </Link>
            <div className="hidden items-center gap-2 sm:flex">
              <BrandLogo title="Smart Otter · Liên kết gia đình" size="sm" />
            </div>
          </div>

          <ThemeToggle />
        </div>
      </header>

      <main className="mx-auto max-w-xl px-4 py-8 sm:px-6">
        {!isAuthReady ? (
          <div className="flex justify-center py-12">
            <Loader2 className="h-6 w-6 animate-spin text-brand-strong" />
          </div>
        ) : (
        <QueryStateBoundary
          data={linkQuery.data}
          isLoading={linkQuery.isLoading}
          isError={linkQuery.isError}
          error={linkQuery.error}
          onRetry={() => void linkQuery.refetch()}
          isEmpty={(d) =>
            activeRole === 'STUDENT' &&
            (!d ||
              d.status === 'REVOKED' ||
              (d.status === 'PENDING' &&
                new Date(d.expiresAt).getTime() <= Date.now()))
          }
          emptyState={{
            title: 'Chưa có mã liên kết',
            description: 'Bấm nút bên dưới để tạo mã 8 ký tự cho bố mẹ nhập.',
            action:
              activeRole === 'STUDENT' ? (
                <Button
                  className="h-11 gap-2"
                  onClick={() => generateMutation.mutate()}
                  disabled={generateMutation.isPending}
                >
                  <KeyRound className="h-4 w-4" strokeWidth={1.75} />
                  Tạo mã 8 ký tự
                </Button>
              ) : undefined,
          }}
        >
          {(linkData) =>
            activeRole === 'STUDENT' ? (
              <Card className="border-brand/25">
                <CardHeader className="pb-4">
                  <div className="flex items-center justify-between gap-2">
                    <CardTitle className="flex items-center gap-2 text-lg">
                      <KeyRound
                        className="h-5 w-5 text-brand-strong dark:text-sky-400"
                        strokeWidth={1.75}
                      />
                      Mã liên kết Phụ huynh
                    </CardTitle>
                    {linkData?.status === 'ACTIVE' ? (
                      <Badge variant="success">
                        <CheckCircle2 className="h-3.5 w-3.5" strokeWidth={1.75} />
                        Đã kết nối
                      </Badge>
                    ) : (
                      <Badge variant="brand">Dùng 1 lần · 24 giờ</Badge>
                    )}
                  </div>
                </CardHeader>

                <CardContent className="space-y-5">
                  <div className="rounded-lg border border-brand/25 bg-brand-soft/60 p-5 text-center dark:bg-sky-950/40">
                    <p className="mb-1.5 text-xs font-medium text-brand-strong dark:text-sky-300">
                      Đưa mã này cho bố mẹ nhập vào tài khoản Phụ huynh
                    </p>
                    <p className="font-mono text-3xl font-bold tracking-[0.22em] text-brand-strong dark:text-sky-200">
                      {linkData?.linkCode ?? '--------'}
                    </p>
                  </div>

                  {linkData?.linkedUserName ? (
                    <div className="flex items-center gap-2 rounded-md border border-emerald-200 bg-emerald-50 px-3.5 py-2.5 text-sm text-emerald-800 dark:border-emerald-900 dark:bg-emerald-950/50 dark:text-emerald-300">
                      <ShieldCheck className="h-4 w-4 shrink-0" strokeWidth={1.75} />
                      <span>
                        Đang liên kết với Phụ huynh:{' '}
                        <strong>{linkData.linkedUserName}</strong>
                      </span>
                    </div>
                  ) : null}

                  <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-2">
                    <Button
                      type="button"
                      variant="outline"
                      className="h-11 gap-2"
                      disabled={!linkData?.linkCode}
                      onClick={() =>
                        linkData?.linkCode && void handleCopy(linkData.linkCode)
                      }
                    >
                      {copied ? (
                        <>
                          <Check className="h-4 w-4 text-emerald-600" strokeWidth={1.75} />
                          Đã sao chép
                        </>
                      ) : (
                        <>
                          <Copy className="h-4 w-4" strokeWidth={1.75} />
                          Sao chép mã
                        </>
                      )}
                    </Button>

                    <Button
                      type="button"
                      className="h-11 gap-2"
                      disabled={generateMutation.isPending}
                      onClick={() => generateMutation.mutate()}
                    >
                      {generateMutation.isPending ? (
                        <Loader2 className="h-4 w-4 animate-spin" strokeWidth={1.75} />
                      ) : (
                        <RefreshCw className="h-4 w-4" strokeWidth={1.75} />
                      )}
                      Tạo mã mới
                    </Button>
                  </div>
                </CardContent>
              </Card>
            ) : (
              <Card className="border-brand/25">
                <CardHeader className="pb-4">
                  <div className="flex items-center justify-between gap-2">
                    <CardTitle className="flex items-center gap-2 text-lg">
                      <Link2
                        className="h-5 w-5 text-brand-strong dark:text-sky-400"
                        strokeWidth={1.75}
                      />
                      Kết nối với Học sinh
                    </CardTitle>
                    {linkData?.status === 'ACTIVE' ? (
                      <Badge variant="success">
                        <CheckCircle2 className="h-3.5 w-3.5" strokeWidth={1.75} />
                        Đã liên kết
                      </Badge>
                    ) : (
                      <Badge variant="secondary">Chưa liên kết</Badge>
                    )}
                  </div>
                </CardHeader>

                <CardContent className="space-y-5">
                  {linkData?.status === 'ACTIVE' && linkData.linkedUserName ? (
                    <div className="space-y-3 rounded-lg border border-emerald-200 bg-emerald-50 p-4 dark:border-emerald-900 dark:bg-emerald-950/50">
                      <div className="flex items-center gap-2 text-sm font-semibold text-emerald-800 dark:text-emerald-300">
                        <ShieldCheck className="h-4 w-4 shrink-0" strokeWidth={1.75} />
                        <span>
                          Học sinh đang theo dõi: {linkData.linkedUserName}
                        </span>
                      </div>
                      <Button asChild className="h-11 w-full">
                        <Link href="/guardian/dashboard">
                          Mở Bảng theo dõi Phụ huynh
                        </Link>
                      </Button>
                    </div>
                  ) : null}

                  <form onSubmit={handleAcceptSubmit} className="space-y-3">
                    <div className="space-y-1.5">
                      <Label htmlFor="guardian-link-code">
                        Nhập mã 8 ký tự từ tài khoản Học sinh
                      </Label>
                      <Input
                        id="guardian-link-code"
                        value={codeInput}
                        onChange={(e) =>
                          setCodeInput(e.target.value.toUpperCase().slice(0, 8))
                        }
                        maxLength={8}
                        placeholder="VD: KHOI2026"
                        className="h-12 text-center font-mono text-lg font-bold tracking-[0.2em] uppercase"
                      />
                      {submitError ? (
                        <p className="text-xs text-rose-600 dark:text-rose-400">
                          {submitError}
                        </p>
                      ) : null}
                    </div>

                    <Button
                      type="submit"
                      className="h-11 w-full gap-2"
                      disabled={acceptMutation.isPending}
                    >
                      {acceptMutation.isPending ? (
                        <Loader2 className="h-4 w-4 animate-spin" strokeWidth={1.75} />
                      ) : (
                        <Link2 className="h-4 w-4" strokeWidth={1.75} />
                      )}
                      Xác nhận liên kết
                    </Button>
                  </form>

                  {IS_MOCK_MODE ? (
                    <p className="rounded-md border bg-muted/40 px-3 py-2 font-mono text-[11px] text-muted-foreground">
                      Mẫu hợp lệ: <strong>KHOI2026</strong> · Thử lỗi:{' '}
                      <strong>EXPIRE00</strong>, <strong>INVALID0</strong>
                    </p>
                  ) : null}
                </CardContent>
              </Card>
            )
          }
        </QueryStateBoundary>
        )}
      </main>
    </div>
  );
}
