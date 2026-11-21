'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import {
  AlertTriangle,
  ArrowRight,
  CheckCircle2,
  Layers,
  Loader2,
  LogOut,
  Mail,
} from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { ThemeToggle } from '@/components/shared/theme-toggle';
import { StatusBadge } from '@/components/shared/status-badge';
import { QueryStateBoundary } from '@/components/shared/query-state-boundary';
import { extractApiErrorMessage } from '@/components/shared/form-error-utils';
import { ApiClientError } from '@/lib/api-client';
import { authService, IS_MOCK_MODE } from '@/services';
import { useAuthStore } from '@/store/auth-store';

const MOCK_TOKEN_SHORTCUTS = [
  { token: 'valid-new', label: 'valid-new (Chưa có TK)' },
  { token: 'valid-existing', label: 'valid-existing (Đã có TK)' },
  { token: 'mismatch', label: 'mismatch (Lệch email)' },
  { token: 'expired', label: 'expired (Hết hạn)' },
];

export function InviteAcceptView() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const token = searchParams.get('token')?.trim() ?? '';

  const user = useAuthStore((state) => state.user);
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const clearSession = useAuthStore((state) => state.clearSession);

  const [accepting, setAccepting] = useState(false);
  const [acceptError, setAcceptError] = useState<string | null>(null);

  const query = useQuery({
    queryKey: ['invitation-verify', token],
    queryFn: async () => {
      if (!token) {
        throw new ApiClientError({
          status: 410,
          errorCode: 'INV_001',
          message:
            'Thiếu mã token lời mời. Vui lòng mở đúng đường dẫn từ email được gửi.',
        });
      }
      return authService.verifyInvitation(token);
    },
    retry: false,
  });

  const handleAcceptNow = async () => {
    setAcceptError(null);
    setAccepting(true);
    try {
      const result = await authService.acceptInvitation(token);
      toast.success('Bạn đã tham gia dự án thành công');
      router.push(`/projects/${result.projectId}/overview`);
    } catch (err) {
      const message = extractApiErrorMessage(err);
      setAcceptError(message);
      toast.error(message);
    } finally {
      setAccepting(false);
    }
  };

  const handleLogoutSwitch = (invitedEmail: string) => {
    clearSession();
    toast.info('Đã đăng xuất. Vui lòng đăng nhập bằng đúng email được mời.');
    router.push(
      `/login?inviteToken=${encodeURIComponent(token)}&email=${encodeURIComponent(invitedEmail)}`
    );
  };

  return (
    <div className="min-h-screen bg-canvas">
      <header className="h-16 border-b bg-card">
        <div className="mx-auto flex h-full max-w-[1280px] items-center justify-between px-4 sm:px-6 lg:px-10">
          <Link
            href="/login"
            className="inline-flex items-center gap-2.5 rounded-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
          >
            <span className="flex h-8 w-8 items-center justify-center rounded-md bg-brand text-white">
              <Layers className="h-4 w-4" strokeWidth={1.75} />
            </span>
            <span className="text-base font-semibold tracking-tight text-foreground">
              TeaMa
            </span>
          </Link>
          <ThemeToggle />
        </div>
      </header>

      <main className="mx-auto flex min-h-[calc(100svh-4rem)] max-w-lg flex-col items-center justify-center px-4 py-12">
        <div className="w-full">
          <QueryStateBoundary
            data={query.data}
            isLoading={query.isLoading}
            isError={query.isError}
            error={query.error}
            errorTitle="Liên kết lời mời không khả dụng"
            onRetry={() => void query.refetch()}
          >
            {(invite) => {
              const isEmailMismatch =
                isAuthenticated &&
                user !== null &&
                user.email.toLowerCase() !== invite.invitedEmail.toLowerCase();

              const registerHref = `/register?inviteToken=${encodeURIComponent(invite.token)}&email=${encodeURIComponent(invite.invitedEmail)}`;
              const loginHref = `/login?inviteToken=${encodeURIComponent(invite.token)}&email=${encodeURIComponent(invite.invitedEmail)}`;

              return (
                <div className="rounded-lg border bg-card p-7">
                  <div className="mb-5 flex items-center justify-between gap-2">
                    <span className="inline-flex h-10 w-10 items-center justify-center rounded-md bg-brand-soft text-brand">
                      <Mail className="h-5 w-5" strokeWidth={1.75} />
                    </span>
                    <StatusBadge status={invite.role} />
                  </div>

                  <p className="font-mono text-xs font-medium text-brand">
                    {invite.projectCode} • {invite.groupName}
                  </p>
                  <h1 className="mt-1 text-xl font-bold tracking-tight text-foreground">
                    {invite.projectName}
                  </h1>

                  <dl className="mt-5 space-y-2 rounded-md border bg-canvas p-4 text-xs">
                    <div className="flex items-center justify-between gap-2">
                      <dt className="text-muted-foreground">Email được mời</dt>
                      <dd className="font-mono font-medium text-foreground">
                        {invite.invitedEmail}
                      </dd>
                    </div>
                    <div className="flex items-center justify-between gap-2">
                      <dt className="text-muted-foreground">Vai trò dự kiến</dt>
                      <dd className="font-mono font-medium text-foreground">
                        {invite.role}
                      </dd>
                    </div>
                    <div className="flex items-center justify-between gap-2">
                      <dt className="text-muted-foreground">Trạng thái tài khoản</dt>
                      <dd className="font-medium text-foreground">
                        {invite.accountExists
                          ? 'Đã đăng ký trên TeaMa'
                          : 'Chưa có tài khoản'}
                      </dd>
                    </div>
                  </dl>

                  {acceptError ? (
                    <div
                      role="alert"
                      className="mt-4 flex items-start gap-2 rounded-md border border-rose-200 bg-rose-50 px-3.5 py-3 text-xs text-rose-700 dark:border-rose-900 dark:bg-rose-950/60 dark:text-rose-300"
                    >
                      <AlertTriangle
                        className="mt-0.5 h-4 w-4 shrink-0"
                        strokeWidth={1.75}
                      />
                      <span>{acceptError}</span>
                    </div>
                  ) : null}

                  {isEmailMismatch ? (
                    <div className="mt-5 space-y-3 rounded-md border border-amber-200 bg-amber-50 p-4 text-xs text-amber-900 dark:border-amber-900 dark:bg-amber-950/50 dark:text-amber-200">
                      <div className="flex items-start gap-2">
                        <AlertTriangle
                          className="mt-0.5 h-4 w-4 shrink-0"
                          strokeWidth={1.75}
                        />
                        <p>
                          Bạn đang đăng nhập bằng{' '}
                          <span className="font-mono font-semibold">
                            {user?.email}
                          </span>
                          , không khớp với email được mời (
                          <span className="font-mono font-semibold">
                            {invite.invitedEmail}
                          </span>
                          ).
                        </p>
                      </div>
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        className="w-full bg-card"
                        onClick={() => handleLogoutSwitch(invite.invitedEmail)}
                      >
                        <LogOut className="h-4 w-4" strokeWidth={1.75} />
                        Đăng xuất &amp; đổi tài khoản
                      </Button>
                    </div>
                  ) : isAuthenticated ? (
                    <div className="mt-6 space-y-2.5">
                      <Button
                        type="button"
                        className="h-10 w-full"
                        disabled={accepting}
                        onClick={() => void handleAcceptNow()}
                      >
                        {accepting ? (
                          <>
                            <Loader2
                              className="h-4 w-4 animate-spin"
                              strokeWidth={1.75}
                            />
                            Đang xác nhận...
                          </>
                        ) : (
                          <>
                            <CheckCircle2
                              className="h-4 w-4"
                              strokeWidth={1.75}
                            />
                            Chấp nhận &amp; Vào dự án
                          </>
                        )}
                      </Button>
                    </div>
                  ) : invite.accountExists ? (
                    <div className="mt-6 flex flex-col gap-2.5">
                      <Button asChild className="h-10 w-full">
                        <Link href={loginHref}>
                          Đăng nhập để tham gia dự án
                          <ArrowRight className="h-4 w-4" strokeWidth={1.75} />
                        </Link>
                      </Button>
                      <Button variant="outline" asChild className="h-10 w-full">
                        <Link href={registerHref}>Tạo tài khoản mới</Link>
                      </Button>
                    </div>
                  ) : (
                    <div className="mt-6 flex flex-col gap-2.5">
                      <Button asChild className="h-10 w-full">
                        <Link href={registerHref}>
                          Đăng ký bằng {invite.invitedEmail}
                          <ArrowRight className="h-4 w-4" strokeWidth={1.75} />
                        </Link>
                      </Button>
                      <Button variant="outline" asChild className="h-10 w-full">
                        <Link href={loginHref}>Đã có tài khoản? Đăng nhập</Link>
                      </Button>
                    </div>
                  )}
                </div>
              );
            }}
          </QueryStateBoundary>

          {IS_MOCK_MODE ? (
            <div className="mt-5 rounded-lg border bg-card p-3.5">
              <p className="mb-2 font-mono text-[11px] font-medium text-muted-foreground">
                Mock Quick-Test Tokens (4 kịch bản /invite):
              </p>
              <div className="flex flex-wrap gap-1.5">
                {MOCK_TOKEN_SHORTCUTS.map((item) => (
                  <Link
                    key={item.token}
                    href={`/invite?token=${item.token}`}
                    className={`rounded border px-2 py-1 font-mono text-[11px] transition-colors ${
                      token === item.token
                        ? 'border-brand bg-brand-soft font-semibold text-brand'
                        : 'bg-canvas text-muted-foreground hover:text-foreground'
                    }`}
                  >
                    {item.label}
                  </Link>
                ))}
              </div>
            </div>
          ) : null}
        </div>
      </main>
    </div>
  );
}
