import { SubmissionDetailView } from '@/components/guardian/submission-detail-view';

interface GuardianSubmissionDetailPageProps {
  params: {
    submissionId: string;
  };
}

export default function GuardianSubmissionDetailPage({
  params,
}: GuardianSubmissionDetailPageProps) {
  return <SubmissionDetailView submissionId={Number(params.submissionId)} />;
}
