import type { Metadata } from 'next';
import { ProjectsDirectoryView } from '@/components/projects/projects-directory-view';

export const metadata: Metadata = {
  title: 'Danh sách dự án | TeaMa SEP490',
  description:
    'Quản lý và khởi tạo không gian đồ án tốt nghiệp SEP490 của nhóm.',
};

export default function ProjectsPage() {
  return <ProjectsDirectoryView />;
}
