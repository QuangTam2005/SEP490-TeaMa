import type { ReactNode } from 'react';
import { StudentShell } from '@/components/student/student-shell';

interface StudentLayoutProps {
  children: ReactNode;
}

export default function StudentLayout({ children }: StudentLayoutProps) {
  return <StudentShell>{children}</StudentShell>;
}
