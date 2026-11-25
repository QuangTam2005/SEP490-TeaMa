'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { AlertCircle, ArrowRight, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  applyApiFieldErrors,
  extractApiErrorMessage,
} from '@/components/shared/form-error-utils';
import { authService } from '@/services';
import { useAuthStore } from '@/store/auth-store';
import { cn } from '@/lib/utils';

const registerSchema = z
  .object({
    fullName: z
      .string()
      .trim()
      .min(2, 'Họ và tên phải có ít nhất 2 ký tự')
      .max(150, 'Họ và tên tối đa 150 ký tự'),
    email: z
      .string()
      .trim()
      .min(1, 'Email không được để trống')
      .email('Email không đúng định dạng'),
    role: z.enum(['STUDENT', 'GUARDIAN']),
    password: z
      .string()
      .min(6, 'Mật khẩu phải có ít nhất 6 ký tự')
      .max(100, 'Mật khẩu tối đa 100 ký tự'),
    confirmPassword: z.string().min(1, 'Vui lòng xác nhận mật khẩu'),
  })
  .refine((data) => data.password === data.confirmPassword, {
    path: ['confirmPassword'],
    message: 'Mật khẩu xác nhận không khớp',
  });

type RegisterFormValues = z.infer<typeof registerSchema>;

export function RegisterForm() {
  const router = useRouter();
  const setSession = useAuthStore((state) => state.setSession);
  const [formError, setFormError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      fullName: '',
      email: '',
      role: 'STUDENT',
      password: '',
      confirmPassword: '',
    },
  });

  const selectedRole = watch('role');

  const onSubmit = async (values: RegisterFormValues) => {
    setFormError(null);
    try {
      const auth = await authService.register({
        fullName: values.fullName.trim(),
        email: values.email.trim(),
        role: values.role,
        password: values.password,
      });
      setSession(auth);
      toast.success('Tạo tài khoản thành công');
      router.push(
        auth.user.role === 'GUARDIAN' ? '/guardian/dashboard' : '/student/today'
      );
    } catch (err) {
      const mapped = applyApiFieldErrors(
        err,
        setError,
        new Set(['fullName', 'email', 'password', 'role'])
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
          Tạo tài khoản
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

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-3.5" noValidate>
        <div className="space-y-1.5">
          <Label>Vai trò</Label>
          <div className="grid grid-cols-2 gap-2">
            {(
              [
                { value: 'STUDENT', label: 'Học sinh lớp 6' },
                { value: 'GUARDIAN', label: 'Phụ huynh / Giám sát' },
              ] as const
            ).map((option) => (
              <button
                key={option.value}
                type="button"
                onClick={() => setValue('role', option.value)}
                className={cn(
                  'h-9 rounded-md border text-xs font-medium transition-colors',
                  selectedRole === option.value
                    ? 'border-brand bg-brand-soft text-brand'
                    : 'bg-background text-muted-foreground hover:text-foreground'
                )}
              >
                {option.label}
              </button>
            ))}
          </div>
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="reg-fullName">Họ và tên</Label>
          <Input
            id="reg-fullName"
            placeholder="Nguyễn Minh Khôi"
            aria-invalid={Boolean(errors.fullName)}
            {...register('fullName')}
          />
          {errors.fullName ? (
            <p className="text-xs text-rose-600 dark:text-rose-400">
              {errors.fullName.message}
            </p>
          ) : null}
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="reg-email">Email</Label>
          <Input
            id="reg-email"
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
          <Label htmlFor="reg-password">Mật khẩu</Label>
          <Input
            id="reg-password"
            type="password"
            autoComplete="new-password"
            placeholder="Tối thiểu 6 ký tự"
            aria-invalid={Boolean(errors.password)}
            {...register('password')}
          />
          {errors.password ? (
            <p className="text-xs text-rose-600 dark:text-rose-400">
              {errors.password.message}
            </p>
          ) : null}
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="reg-confirm">Xác nhận mật khẩu</Label>
          <Input
            id="reg-confirm"
            type="password"
            autoComplete="new-password"
            placeholder="Nhập lại mật khẩu"
            aria-invalid={Boolean(errors.confirmPassword)}
            {...register('confirmPassword')}
          />
          {errors.confirmPassword ? (
            <p className="text-xs text-rose-600 dark:text-rose-400">
              {errors.confirmPassword.message}
            </p>
          ) : null}
        </div>

        <Button
          type="submit"
          className="mt-1 h-10 w-full"
          disabled={isSubmitting}
        >
          {isSubmitting ? (
            <>
              <Loader2 className="h-4 w-4 animate-spin" strokeWidth={1.75} />
              Đang tạo tài khoản...
            </>
          ) : (
            <>
              Đăng ký tài khoản
              <ArrowRight className="h-4 w-4" strokeWidth={1.75} />
            </>
          )}
        </Button>
      </form>

      <p className="mt-6 text-center text-sm text-muted-foreground">
        Đã có tài khoản?{' '}
        <Link
          href="/login"
          className="font-medium text-brand hover:underline"
        >
          Đăng nhập
        </Link>
      </p>
    </div>
  );
}
