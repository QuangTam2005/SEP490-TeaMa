'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { AlertCircle, ArrowRight, Loader2, Lock, Mail } from 'lucide-react';
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
    password: z
      .string()
      .min(6, 'Mật khẩu phải có ít nhất 6 ký tự')
      .max(100, 'Mật khẩu tối đa 100 ký tự'),
    confirmPassword: z.string().min(1, 'Vui lòng xác nhận mật khẩu'),
    studentCode: z
      .string()
      .trim()
      .max(20, 'Mã sinh viên tối đa 20 ký tự')
      .optional(),
    phone: z
      .string()
      .trim()
      .max(20, 'Số điện thoại tối đa 20 ký tự')
      .optional(),
  })
  .refine((data) => data.password === data.confirmPassword, {
    path: ['confirmPassword'],
    message: 'Mật khẩu xác nhận không khớp',
  });

type RegisterFormValues = z.infer<typeof registerSchema>;

export function RegisterForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const inviteToken = searchParams.get('inviteToken');
  const invitedEmail = searchParams.get('email') ?? '';
  const lockInvitedEmail = Boolean(inviteToken && invitedEmail);

  const setSession = useAuthStore((state) => state.setSession);
  const [formError, setFormError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      fullName: '',
      email: invitedEmail,
      password: '',
      confirmPassword: '',
      studentCode: '',
      phone: '',
    },
  });

  const onSubmit = async (values: RegisterFormValues) => {
    setFormError(null);
    try {
      const auth = await authService.register({
        fullName: values.fullName.trim(),
        email: values.email.trim(),
        password: values.password,
        studentCode: values.studentCode?.trim() || null,
        phone: values.phone?.trim() || null,
      });
      setSession(auth);

      if (inviteToken) {
        try {
          const accepted = await authService.acceptInvitation(inviteToken);
          toast.success('Đăng ký và tham gia dự án thành công');
          router.push(`/projects/${accepted.projectId}/overview`);
          return;
        } catch (inviteErr) {
          const inviteMessage = extractApiErrorMessage(inviteErr);
          setFormError(inviteMessage);
          toast.error(inviteMessage);
          return;
        }
      }

      toast.success('Tạo tài khoản thành công');
      router.push('/projects');
    } catch (err) {
      const mapped = applyApiFieldErrors(
        err,
        setError,
        new Set(['fullName', 'email', 'password', 'studentCode', 'phone'])
      );
      const message = extractApiErrorMessage(err);
      if (!mapped) {
        setFormError(message);
      }
      toast.error(message);
    }
  };

  const loginHref = inviteToken
    ? `/login?inviteToken=${encodeURIComponent(inviteToken)}&email=${encodeURIComponent(invitedEmail)}`
    : '/login';

  return (
    <div className="w-full max-w-[420px] rounded-lg border bg-card p-7">
      <div className="mb-6">
        <h1 className="text-2xl font-bold tracking-tight text-foreground">
          Tạo tài khoản
        </h1>
        <p className="mt-1.5 text-sm text-muted-foreground">
          Sử dụng bất kỳ địa chỉ email hợp lệ nào để bắt đầu làm việc cùng nhóm.
        </p>
      </div>

      {inviteToken ? (
        <div className="mb-5 flex items-start gap-2.5 rounded-md border bg-brand-soft px-3.5 py-3 text-xs text-foreground">
          <Mail
            className="mt-0.5 h-4 w-4 shrink-0 text-brand"
            strokeWidth={1.75}
          />
          <div>
            <p className="font-semibold">Đăng ký theo lời mời dự án</p>
            <p className="mt-0.5 text-muted-foreground">
              Hoàn tất đăng ký với email được mời để hệ thống tự động đưa bạn
              vào nhóm.
            </p>
          </div>
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

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-3.5" noValidate>
        <div className="space-y-1.5">
          <Label htmlFor="reg-fullName">Họ và tên</Label>
          <Input
            id="reg-fullName"
            placeholder="Nguyễn Văn An"
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
          <div className="flex items-center justify-between">
            <Label htmlFor="reg-email">Email</Label>
            {lockInvitedEmail ? (
              <span className="inline-flex items-center gap-1 font-mono text-[11px] text-brand">
                <Lock className="h-3 w-3" strokeWidth={1.75} />
                Khớp lời mời
              </span>
            ) : null}
          </div>
          <Input
            id="reg-email"
            type="email"
            autoComplete="email"
            placeholder="name@example.com"
            readOnly={lockInvitedEmail}
            className={lockInvitedEmail ? 'bg-muted/60 text-muted-foreground' : undefined}
            aria-invalid={Boolean(errors.email)}
            {...register('email')}
          />
          {errors.email ? (
            <p className="text-xs text-rose-600 dark:text-rose-400">
              {errors.email.message}
            </p>
          ) : null}
        </div>

        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <div className="space-y-1.5">
            <Label htmlFor="reg-studentCode">Mã sinh viên (tuỳ chọn)</Label>
            <Input
              id="reg-studentCode"
              placeholder="SE180123"
              className="font-mono"
              {...register('studentCode')}
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="reg-phone">Số điện thoại (tuỳ chọn)</Label>
            <Input
              id="reg-phone"
              placeholder="0901234567"
              {...register('phone')}
            />
          </div>
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
              {inviteToken ? 'Đăng ký & Tham gia dự án' : 'Đăng ký tài khoản'}
              <ArrowRight className="h-4 w-4" strokeWidth={1.75} />
            </>
          )}
        </Button>
      </form>

      <p className="mt-6 text-center text-sm text-muted-foreground">
        Đã có tài khoản?{' '}
        <Link
          href={loginHref}
          className="font-medium text-brand hover:underline"
        >
          Đăng nhập
        </Link>
      </p>
    </div>
  );
}
