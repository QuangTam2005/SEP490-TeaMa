import { Suspense } from 'react';
import type { Metadata } from 'next';
import { InviteAcceptView } from '@/components/auth/invite-accept-view';

export const metadata: Metadata = {
  title: 'Chấp nhận lời mời dự án | TeaMa SEP490',
  description:
    'Xác thực liên kết lời mời qua email để tham gia nhóm đồ án SEP490.',
};

function InviteFallback() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-canvas px-4">
      <div className="w-full max-w-lg space-y-4 rounded-lg border bg-card p-7">
        <div className="h-6 w-1/3 animate-pulse rounded bg-muted" />
        <div className="h-24 w-full animate-pulse rounded bg-muted" />
        <div className="h-10 w-full animate-pulse rounded bg-muted" />
      </div>
    </div>
  );
}

export default function InvitePage() {
  return (
    <Suspense fallback={<InviteFallback />}>
      <InviteAcceptView />
    </Suspense>
  );
}
