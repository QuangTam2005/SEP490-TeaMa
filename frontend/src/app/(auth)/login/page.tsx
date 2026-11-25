import { Suspense } from 'react';
import type { Metadata } from 'next';
import { LoginForm } from '@/components/auth/login-form';

export const metadata: Metadata = {
  title: 'Đăng nhập | AI Study Companion — Lớp 6',
  description: 'Đăng nhập hệ thống tự học AI Study Companion lớp 6.',
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
