import type { ProjectRole, UserResponse } from './auth';

export type MilestoneStatus = 'PENDING' | 'SUBMITTED' | 'APPROVED';
export type DeliverableStatus = 'PENDING' | 'SUBMITTED' | 'APPROVED';
export type InvitationStatus = 'PENDING' | 'ACCEPTED' | 'REVOKED' | 'EXPIRED';

export interface ProjectResponse {
  id: number;
  projectCode: string;
  projectName: string;
  groupName: string;
  softwareType: string | null;
  startDate: string;
  supervisorName: string | null;
  supervisorEmail: string | null;
  supervisorMagicToken: string | null;
  currentUserRole: ProjectRole;
  memberCount: number;
  repoCount: number;
  completedMilestones: number;
  totalMilestones: number;
  createdAt: string;
}

export interface CreateProjectRequest {
  projectCode: string;
  projectName: string;
  groupName: string;
  softwareType?: string;
  startDate: string;
  supervisorName?: string;
  supervisorEmail?: string;
}

export interface MilestoneDeliverableResponse {
  id: number;
  milestoneId: number;
  itemName: string;
  isRequired: boolean;
  filePath: string | null;
  fileName: string | null;
  fileSize: number | null;
  uploadedByName: string | null;
  uploadedAt: string | null;
  status: DeliverableStatus;
}

export interface MilestoneResponse {
  id: number;
  projectId: number;
  milestoneCode: string;
  name: string;
  weekNumber: number;
  dueDate: string | null;
  ogaWeightPercent: number;
  status: MilestoneStatus;
  deliverables: MilestoneDeliverableResponse[];
}

export interface ProjectMemberResponse {
  id: number;
  projectId: number;
  user: UserResponse;
  role: ProjectRole;
  joinedAt: string;
}

export interface ProjectInvitationResponse {
  id: number;
  projectId: number;
  invitedEmail: string;
  role: ProjectRole;
  token: string;
  status: InvitationStatus;
  invitedByName: string;
  expiresAt: string;
  acceptedAt: string | null;
  createdAt: string;
}

export interface SendInvitationRequest {
  email: string;
  role: ProjectRole;
}

export interface InvitationVerifyResponse {
  token: string;
  projectId: number;
  projectCode: string;
  projectName: string;
  groupName: string;
  invitedEmail: string;
  role: ProjectRole;
  invitedByName: string;
  expiresAt: string;
  accountExists: boolean;
}

export interface ProjectGitRepositoryResponse {
  id: number;
  projectId: number;
  repoLabel: string;
  repoFullName: string;
  githubRepoUrl: string;
  webhookUrl: string;
  webhookSecret: string | null;
  createdAt: string;
}

export interface CreateGitRepositoryRequest {
  repoLabel: string;
  repoFullName: string;
  githubRepoUrl: string;
}
