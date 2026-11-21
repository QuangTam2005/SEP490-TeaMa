import type { ReactNode } from 'react';
import { WorkspaceShell } from '@/components/workspace/workspace-shell';

interface ProjectLayoutProps {
  children: ReactNode;
  params: { projectId: string };
}

export default function ProjectWorkspaceLayout({
  children,
  params,
}: ProjectLayoutProps) {
  const numericProjectId = Number(params.projectId);
  return (
    <WorkspaceShell projectId={numericProjectId}>{children}</WorkspaceShell>
  );
}
