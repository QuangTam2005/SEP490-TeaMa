import type { IterationCode } from './planning';

export type TrackingLogType = 'ISSUE' | 'DEFECT' | 'QA' | 'WEEKLY_MATTER';
export type TrackingPriority = 'HIGH' | 'MEDIUM' | 'LOW';
export type TrackingStatus =
  | 'OPEN'
  | 'IN_PROGRESS'
  | 'RESOLVED'
  | 'CLOSED'
  | 'PENDING';

export interface TrackingLogResponse {
  id: number;
  projectId: number;
  logType: TrackingLogType;
  code: string;
  title: string;
  description: string | null;
  priority: TrackingPriority;
  potentialImpact: string | null;
  screenCode: string | null;
  relatedTestCaseCode: string | null;
  raisedBy: string;
  assigneeName: string | null;
  status: TrackingStatus;
  answerOrResolution: string | null;
  createdAt: string;
  closedAt: string | null;
}

export interface PeerReviewEntryResponse {
  id: number;
  iteration: IterationCode;
  revieweeId: number;
  revieweeName: string;
  attitudeScore: number;
  timelinessScore: number;
  qualityScore: number;
  averageScore: number;
  comment: string | null;
}

export interface MemberContributionResponse {
  userId: number;
  fullName: string;
  studentCode: string | null;
  role: 'LEADER' | 'MEMBER';
  tasksCompleted: number;
  actualMandays: number;
  mergedPrs: number;
  testCasesExecuted: number;
  peerReviewAvg: number;
  freeRiderWarning: boolean;
}

export interface DocumentChangeLogResponse {
  id: number;
  reportTarget: string;
  changeType: 'A' | 'M' | 'D';
  inChargeName: string;
  changeDescription: string;
  createdAt: string;
}
