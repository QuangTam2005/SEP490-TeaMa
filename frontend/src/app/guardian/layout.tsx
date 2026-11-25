import type { ReactNode } from 'react';
import { GuardianShell } from '@/components/guardian/guardian-shell';

interface GuardianLayoutProps {
  children: ReactNode;
}

export default function GuardianLayout({ children }: GuardianLayoutProps) {
  return <GuardianShell>{children}</GuardianShell>;
}
