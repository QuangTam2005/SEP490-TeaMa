import { Suspense } from 'react';
import type { Metadata } from 'next';
import { RegisterForm } from '@/components/auth/register-form';

export const metadata: Metadata = {
  title: 'Đăng ký | TeaMa SEP490',
  description: 'Tạo tài khoản mới trên nền tảng SEP490 Capstone Copilot.',
};

function RegisterFormFallback() {
  return (
    <div className="w-full max-w-[420px] space-y-4 rounded-lg border bg-card p-7">
      <div className="h-7 w-1/3 animate-pulse rounded bg-muted" />
      <div className="h-10 w-full animate-pulse rounded bg-muted" />
      <div className="h-10 w-full animate-pulse rounded bg-muted" />
      <div className="h-10 w-full animate-pulse rounded bg-muted" />
    </div>
  );
}

export default function RegisterPage() {
  return (
    <Suspense fallback={<RegisterFormFallback />}>
      <RegisterForm />
    </Suspense>
  );
}
