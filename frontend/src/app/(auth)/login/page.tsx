import { Suspense } from 'react';
import type { Metadata } from 'next';
import { LoginForm } from '@/components/auth/login-form';

export const metadata: Metadata = {
  title: 'Đăng nhập | TeaMa SEP490',
  description: 'Đăng nhập vào không gian quản trị đồ án SEP490 Capstone Copilot.',
};

function AuthFormFallback() {
  return (
    <div className="w-full max-w-[420px] space-y-4 rounded-lg border bg-card p-7">
      <div className="h-7 w-1/3 animate-pulse rounded bg-muted" />
      <div className="h-10 w-full animate-pulse rounded bg-muted" />
      <div className="h-10 w-full animate-pulse rounded bg-muted" />
      <div className="h-10 w-full animate-pulse rounded bg-muted" />
    </div>
  );
}

export default function LoginPage() {
  return (
    <Suspense fallback={<AuthFormFallback />}>
      <LoginForm />
    </Suspense>
  );
}
