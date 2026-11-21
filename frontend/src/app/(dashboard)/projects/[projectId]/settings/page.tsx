import type { Metadata } from 'next';
import { ProjectSettingsView } from '@/components/workspace/project-settings-view';

export const metadata: Metadata = {
  title: 'Cài đặt nhóm & Thành viên | TeaMa SEP490',
  description:
    'Quản lý thành viên, gửi lời mời qua Email và tạo/thu hồi Magic Link cho Giảng viên.',
};

interface SettingsPageProps {
  params: { projectId: string };
}

export default function ProjectSettingsPage({ params }: SettingsPageProps) {
  return <ProjectSettingsView projectId={Number(params.projectId)} />;
}
