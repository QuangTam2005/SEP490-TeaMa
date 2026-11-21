import {
  apiDelete,
  apiGet,
  apiPatch,
  apiPost,
  apiUpload,
} from '@/lib/api-client';
import type { ProjectService } from '@/services/project.service';
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

export const projectHttpService: ProjectService = {
  listProjects: () => apiGet<ProjectResponse[]>('/api/projects'),

  getProject: (projectId: number) =>
    apiGet<ProjectResponse>(`/api/projects/${projectId}`),

  createProject: (request: CreateProjectRequest) =>
    apiPost<ProjectResponse>('/api/projects', request),

  getMilestones: (projectId: number) =>
    apiGet<MilestoneResponse[]>(`/api/projects/${projectId}/milestones`),

  uploadDeliverable: (
    projectId: number,
    deliverableId: number,
    file: File,
    onProgress?: (percent: number) => void
  ) => {
    const formData = new FormData();
    formData.append('file', file);
    return apiUpload<MilestoneDeliverableResponse>(
      `/api/projects/${projectId}/deliverables/${deliverableId}/upload`,
      formData,
      onProgress
    );
  },

  listMembers: (projectId: number) =>
    apiGet<ProjectMemberResponse[]>(`/api/projects/${projectId}/members`),

  updateMemberRole: (projectId: number, memberId: number, role: ProjectRole) =>
    apiPatch<ProjectMemberResponse>(
      `/api/projects/${projectId}/members/${memberId}/role`,
      { role }
    ),

  removeMember: (projectId: number, memberId: number) =>
    apiDelete<void>(`/api/projects/${projectId}/members/${memberId}`),

  listInvitations: (projectId: number) =>
    apiGet<ProjectInvitationResponse[]>(
      `/api/projects/${projectId}/invitations`
    ),

  sendInvitation: (projectId: number, request: SendInvitationRequest) =>
    apiPost<ProjectInvitationResponse>(
      `/api/projects/${projectId}/invitations`,
      request
    ),

  resendInvitation: (projectId: number, invitationId: number) =>
    apiPost<ProjectInvitationResponse>(
      `/api/projects/${projectId}/invitations/${invitationId}/resend`
    ),

  revokeInvitation: (projectId: number, invitationId: number) =>
    apiDelete<void>(`/api/projects/${projectId}/invitations/${invitationId}`),

  rotateMagicLink: (projectId: number) =>
    apiPost<{ supervisorMagicToken: string }>(
      `/api/projects/${projectId}/magic-link/rotate`
    ),

  listRepositories: (projectId: number) =>
    apiGet<ProjectGitRepositoryResponse[]>(
      `/api/projects/${projectId}/repositories`
    ),

  createRepository: (projectId: number, request: CreateGitRepositoryRequest) =>
    apiPost<ProjectGitRepositoryResponse>(
      `/api/projects/${projectId}/repositories`,
      request
    ),

  deleteRepository: (projectId: number, repoId: number) =>
    apiDelete<void>(`/api/projects/${projectId}/repositories/${repoId}`),
};
