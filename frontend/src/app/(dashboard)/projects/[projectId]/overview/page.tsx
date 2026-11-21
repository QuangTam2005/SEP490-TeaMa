import type { Metadata } from 'next';
import { OverviewMilestonesView } from '@/components/workspace/overview-milestones-view';

export const metadata: Metadata = {
  title: 'Tổng quan & 10 Cột mốc OGA | TeaMa SEP490',
  description:
    'Theo dõi 10 cột mốc SEP490, trọng số OGA và nộp checklist tài liệu lũy tiến.',
};

interface OverviewPageProps {
  params: { projectId: string };
}

export default function ProjectOverviewPage({ params }: OverviewPageProps) {
  return <OverviewMilestonesView projectId={Number(params.projectId)} />;
}
