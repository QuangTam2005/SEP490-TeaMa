'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  AlertCircle,
  ArrowRight,
  GraduationCap,
  Loader2,
  ShieldCheck,
} from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  applyApiFieldErrors,
  extractApiErrorMessage,
} from '@/components/shared/form-error-utils';
import { setSimulatedRole } from '@/mocks/mock-runtime';
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
      email: '',
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
      if (IS_MOCK_MODE) {
        setSimulatedRole(auth.user.role);
      }
      setSession(auth);
      toast.success(`Chào mừng ${auth.user.fullName}`);
      router.push(
        auth.user.role === 'GUARDIAN' ? '/guardian/dashboard' : '/student/today'
      );
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

  return (
    <div className="w-full max-w-[420px] rounded-lg border bg-card p-7">
      <div className="mb-6">
        <h1 className="text-2xl font-bold tracking-tight text-foreground">
          Đăng nhập
        </h1>
      </div>

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
            className="h-11"
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
            className="h-11"
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
          className="h-11 w-full text-sm font-semibold"
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
        <div className="mt-5 space-y-2 border-t pt-4">
          <p className="text-xs font-medium text-muted-foreground">
            Vào nhanh (Dữ liệu mẫu):
          </p>
          <div className="grid grid-cols-2 gap-2">
            <Button
              type="button"
              variant="outline"
              className="h-11 gap-1.5 border-brand/30 bg-brand-soft/40 text-xs font-semibold text-brand-strong hover:bg-brand-soft dark:text-sky-300"
              disabled={isSubmitting}
              onClick={() =>
                void onSubmit({
                  email: 'hocsinh.lop6@gmail.com',
                  password: 'password123',
                })
              }
            >
              <GraduationCap className="h-4 w-4" strokeWidth={1.75} />
              Học sinh
            </Button>
            <Button
              type="button"
              variant="outline"
              className="h-11 gap-1.5 text-xs font-semibold"
              disabled={isSubmitting}
              onClick={() =>
                void onSubmit({
                  email: 'phuhuynh@gmail.com',
                  password: 'password123',
                })
              }
            >
              <ShieldCheck className="h-4 w-4" strokeWidth={1.75} />
              Phụ huynh
            </Button>
          </div>
        </div>
      ) : null}

      <p className="mt-6 text-center text-sm text-muted-foreground">
        Chưa có tài khoản?{' '}
        <Link
          href="/register"
          className="font-semibold text-brand-strong hover:underline dark:text-sky-400"
        >
          Đăng ký ngay
        </Link>
      </p>
    </div>
  );
}
