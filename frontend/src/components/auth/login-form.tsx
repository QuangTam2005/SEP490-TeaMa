'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { AlertCircle, ArrowRight, Loader2, Mail } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  applyApiFieldErrors,
  extractApiErrorMessage,
} from '@/components/shared/form-error-utils';
import { authService, IS_MOCK_MODE } from '@/services';
import { useAuthStore } from '@/store/auth-store';

const loginSchema = z.object({
  email: z
    .string()
    .trim()
    .min(1, 'Email không được để trống')
    .email('Email không đúng định dạng'),
  password: z.string().min(1, 'Mật khẩu không được để trống'),
});

type LoginFormValues = z.infer<typeof loginSchema>;

export function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const inviteToken = searchParams.get('inviteToken');
  const invitedEmail = searchParams.get('email') ?? '';

  const setSession = useAuthStore((state) => state.setSession);
  const [formError, setFormError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: invitedEmail,
      password: '',
    },
  });

  const onSubmit = async (values: LoginFormValues) => {
    setFormError(null);
    try {
      const auth = await authService.login({
        email: values.email.trim(),
        password: values.password,
      });
      setSession(auth);

      if (inviteToken) {
        try {
          const accepted = await authService.acceptInvitation(inviteToken);
          toast.success('Đăng nhập và tham gia dự án thành công');
          router.push(`/projects/${accepted.projectId}/overview`);
          return;
        } catch (inviteErr) {
          const inviteMessage = extractApiErrorMessage(inviteErr);
          setFormError(inviteMessage);
          toast.error(inviteMessage);
          return;
        }
      }

      toast.success(`Chào mừng ${auth.user.fullName}`);
      router.push('/projects');
    } catch (err) {
      const mapped = applyApiFieldErrors(
        err,
        setError,
        new Set(['email', 'password'])
      );
      const message = extractApiErrorMessage(err);
      if (!mapped) {
        setFormError(message);
      }
      toast.error(message);
    }
  };

  const registerHref = inviteToken
    ? `/register?inviteToken=${encodeURIComponent(inviteToken)}&email=${encodeURIComponent(invitedEmail)}`
    : '/register';

  return (
    <div className="w-full max-w-[420px] rounded-lg border bg-card p-7">
      <div className="mb-6">
        <h1 className="text-2xl font-bold tracking-tight text-foreground">
          Đăng nhập
        </h1>
      </div>

      {inviteToken ? (
        <div className="mb-5 flex items-center gap-2 rounded-md border bg-brand-soft px-3.5 py-2.5 text-xs text-foreground">
          <Mail className="h-4 w-4 shrink-0 text-brand" strokeWidth={1.75} />
          <span className="font-mono">
            {invitedEmail || 'Xác thực lời mời dự án'}
          </span>
        </div>
      ) : null}

      {formError ? (
        <div
          role="alert"
          className="mb-5 flex items-start gap-2.5 rounded-md border border-rose-200 bg-rose-50 px-3.5 py-3 text-xs text-rose-700 dark:border-rose-900 dark:bg-rose-950/60 dark:text-rose-300"
        >
          <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" strokeWidth={1.75} />
          <span>{formError}</span>
        </div>
      ) : null}

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        <div className="space-y-1.5">
          <Label htmlFor="login-email">Email</Label>
          <Input
            id="login-email"
            type="email"
            autoComplete="email"
            placeholder="name@example.com"
            aria-invalid={Boolean(errors.email)}
            {...register('email')}
          />
          {errors.email ? (
            <p className="text-xs text-rose-600 dark:text-rose-400">
              {errors.email.message}
            </p>
          ) : null}
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="login-password">Mật khẩu</Label>
          <Input
            id="login-password"
            type="password"
            autoComplete="current-password"
            placeholder="Nhập mật khẩu"
            aria-invalid={Boolean(errors.password)}
            {...register('password')}
          />
          {errors.password ? (
            <p className="text-xs text-rose-600 dark:text-rose-400">
              {errors.password.message}
            </p>
          ) : null}
        </div>

        <Button
          type="submit"
          className="h-10 w-full"
          disabled={isSubmitting}
        >
          {isSubmitting ? (
            <>
              <Loader2 className="h-4 w-4 animate-spin" strokeWidth={1.75} />
              Đang xác thực...
            </>
          ) : (
            <>
              Đăng nhập
              <ArrowRight className="h-4 w-4" strokeWidth={1.75} />
            </>
          )}
        </Button>
      </form>

      {IS_MOCK_MODE ? (
        <p className="mt-4 rounded-md border bg-muted/40 px-3 py-2 font-mono text-[11px] text-muted-foreground">
          Mock hint: dùng bất kỳ email hợp lệ nào; nhập mật khẩu{' '}
          <span className="font-semibold text-foreground">wrongpass</span> để
          xem lỗi 401 AUTH_002.
        </p>
      ) : null}

      <p className="mt-6 text-center text-sm text-muted-foreground">
        Chưa có tài khoản?{' '}
        <Link
          href={registerHref}
          className="font-medium text-brand hover:underline"
        >
          Đăng ký ngay
        </Link>
      </p>
    </div>
  );
}
