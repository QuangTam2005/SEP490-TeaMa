import { TaskWorkspaceView } from '@/components/student/task-workspace-view';

interface StudentTaskDetailPageProps {
  params: {
    taskId: string;
  };
}

export default function StudentTaskDetailPage({
  params,
}: StudentTaskDetailPageProps) {
  return <TaskWorkspaceView taskId={Number(params.taskId)} />;
}
