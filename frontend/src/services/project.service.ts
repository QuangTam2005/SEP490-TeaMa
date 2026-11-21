import type {
  CreateGitRepositoryRequest,
  CreateProjectRequest,
  MilestoneDeliverableResponse,
  MilestoneResponse,
  ProjectGitRepositoryResponse,
  ProjectInvitationResponse,
  ProjectMemberResponse,
  ProjectResponse,
  ProjectRole,
  SendInvitationRequest,
} from '@/types';

export interface ProjectService {
  listProjects(): Promise<ProjectResponse[]>;
  getProject(projectId: number): Promise<ProjectResponse>;
  createProject(request: CreateProjectRequest): Promise<ProjectResponse>;
  getMilestones(projectId: number): Promise<MilestoneResponse[]>;
  uploadDeliverable(
    projectId: number,
    deliverableId: number,
    file: File,
    onProgress?: (percent: number) => void
  ): Promise<MilestoneDeliverableResponse>;
  listMembers(projectId: number): Promise<ProjectMemberResponse[]>;
  updateMemberRole(
    projectId: number,
    memberId: number,
    role: ProjectRole
  ): Promise<ProjectMemberResponse>;
  removeMember(projectId: number, memberId: number): Promise<void>;
  listInvitations(projectId: number): Promise<ProjectInvitationResponse[]>;
  sendInvitation(
    projectId: number,
    request: SendInvitationRequest
  ): Promise<ProjectInvitationResponse>;
  resendInvitation(
    projectId: number,
    invitationId: number
  ): Promise<ProjectInvitationResponse>;
  revokeInvitation(projectId: number, invitationId: number): Promise<void>;
  rotateMagicLink(projectId: number): Promise<{ supervisorMagicToken: string }>;
  listRepositories(projectId: number): Promise<ProjectGitRepositoryResponse[]>;
  createRepository(
    projectId: number,
    request: CreateGitRepositoryRequest
  ): Promise<ProjectGitRepositoryResponse>;
  deleteRepository(projectId: number, repoId: number): Promise<void>;
}
