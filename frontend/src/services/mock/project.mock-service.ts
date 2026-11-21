import { ApiClientError } from '@/lib/api-client';
import {
  SEED_INVITATIONS,
  SEED_MILESTONES_101,
  SEED_PROJECTS,
  SEED_PROJECT_MEMBERS,
  SEED_REPOSITORIES,
} from '@/mocks/fixtures/seed-data';
import {
  getMockRuntimeState,
  registerMockResetHook,
  runMockOperation,
} from '@/mocks/mock-runtime';
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

const MAX_DELIVERABLE_BYTES = 50 * 1024 * 1024;
const ALLOWED_EXTENSIONS = [
  '.docx',
  '.pdf',
  '.xlsx',
  '.zip',
  '.pptx',
  '.png',
  '.jpg',
  '.webp',
];

let projectsDb: ProjectResponse[] = structuredClone(SEED_PROJECTS);
let milestonesDb: Record<number, MilestoneResponse[]> = {
  101: structuredClone(SEED_MILESTONES_101),
};
let membersDb: ProjectMemberResponse[] = structuredClone(SEED_PROJECT_MEMBERS);
let invitationsDb: ProjectInvitationResponse[] =
  structuredClone(SEED_INVITATIONS);
let reposDb: ProjectGitRepositoryResponse[] = structuredClone(SEED_REPOSITORIES);
let nextId = 5000;

registerMockResetHook(() => {
  projectsDb = structuredClone(SEED_PROJECTS);
  milestonesDb = {
    101: structuredClone(SEED_MILESTONES_101),
  };
  membersDb = structuredClone(SEED_PROJECT_MEMBERS);
  invitationsDb = structuredClone(SEED_INVITATIONS);
  reposDb = structuredClone(SEED_REPOSITORIES);
  nextId = 5000;
});

function applyRoleProjection(project: ProjectResponse): ProjectResponse {
  const { simulatedRole } = getMockRuntimeState();
  const effectiveRole: ProjectRole =
    simulatedRole === 'LEADER' ? 'LEADER' : 'MEMBER';
  return {
    ...project,
    currentUserRole: effectiveRole,
    supervisorMagicToken:
      effectiveRole === 'LEADER' ? project.supervisorMagicToken : null,
  };
}

export const projectMockService: ProjectService = {
  async listProjects(): Promise<ProjectResponse[]> {
    return runMockOperation(
      () => projectsDb.map(applyRoleProjection),
      { emptyFallback: [] }
    );
  },

  async getProject(projectId: number): Promise<ProjectResponse> {
    return runMockOperation(() => {
      const found = projectsDb.find((p) => p.id === projectId);
      if (!found) {
        throw new ApiClientError({
          status: 404,
          errorCode: 'PROJ_001',
          message: 'Không tìm thấy dự án.',
        });
      }
      return applyRoleProjection(found);
    });
  },

  async createProject(request: CreateProjectRequest): Promise<ProjectResponse> {
    return runMockOperation(
      () => {
        const id = nextId++;
        const created: ProjectResponse = {
          id,
          projectCode: request.projectCode.trim().toUpperCase(),
          projectName: request.projectName.trim(),
          groupName: request.groupName.trim(),
          softwareType: request.softwareType?.trim() || null,
          startDate: request.startDate,
          supervisorName: request.supervisorName?.trim() || null,
          supervisorEmail: request.supervisorEmail?.trim() || null,
          supervisorMagicToken: crypto.randomUUID(),
          currentUserRole: 'LEADER',
          memberCount: 1,
          repoCount: 0,
          completedMilestones: 0,
          totalMilestones: 10,
          createdAt: new Date().toISOString(),
        };
        projectsDb = [created, ...projectsDb];
        milestonesDb[id] = structuredClone(SEED_MILESTONES_101).map((m) => ({
          ...m,
          id: nextId++,
          projectId: id,
          status: 'PENDING',
          deliverables: m.deliverables.map((d) => ({
            ...d,
            id: nextId++,
            filePath: null,
            fileName: null,
            fileSize: null,
            uploadedByName: null,
            uploadedAt: null,
            status: 'PENDING',
          })),
        }));
        return created;
      },
      { disallowSupervisor: true }
    );
  },

  async getMilestones(projectId: number): Promise<MilestoneResponse[]> {
    return runMockOperation(
      () => structuredClone(milestonesDb[projectId] ?? SEED_MILESTONES_101),
      { emptyFallback: [] }
    );
  },

  async uploadDeliverable(
    projectId: number,
    deliverableId: number,
    file: File,
    onProgress?: (percent: number) => void
  ): Promise<MilestoneDeliverableResponse> {
    return runMockOperation(
      async () => {
        if (file.size > MAX_DELIVERABLE_BYTES) {
          throw new ApiClientError({
            status: 413,
            errorCode: 'FILE_413',
            message: 'Dung lượng tệp vượt quá giới hạn tối đa 50MB.',
          });
        }
        const lowerName = file.name.toLowerCase();
        const isAllowed = ALLOWED_EXTENSIONS.some((ext) =>
          lowerName.endsWith(ext)
        );
        if (!isAllowed) {
          throw new ApiClientError({
            status: 400,
            errorCode: 'FILE_001',
            message: `Định dạng tệp không được hỗ trợ. Chỉ chấp nhận: ${ALLOWED_EXTENSIONS.join(', ')}.`,
          });
        }

        onProgress?.(35);
        await new Promise((r) => setTimeout(r, 140));
        onProgress?.(75);
        await new Promise((r) => setTimeout(r, 140));
        onProgress?.(100);

        const list = milestonesDb[projectId] ?? [];
        for (const milestone of list) {
          const target = milestone.deliverables.find(
            (d) => d.id === deliverableId
          );
          if (target) {
            target.fileName = file.name;
            target.fileSize = file.size;
            target.filePath = `projects/${projectId}/${milestone.milestoneCode.toLowerCase()}/${file.name}`;
            target.uploadedByName = 'Nguyễn Quang Tâm';
            target.uploadedAt = new Date().toISOString();
            target.status = 'SUBMITTED';
            return structuredClone(target);
          }
        }

        throw new ApiClientError({
          status: 404,
          errorCode: 'MILE_002',
          message: 'Không tìm thấy đầu mục nộp bài.',
        });
      },
      { disallowSupervisor: true }
    );
  },

  async listMembers(projectId: number): Promise<ProjectMemberResponse[]> {
    return runMockOperation(
      () => membersDb.filter((m) => m.projectId === projectId),
      { emptyFallback: [] }
    );
  },

  async updateMemberRole(
    projectId: number,
    memberId: number,
    role: ProjectRole
  ): Promise<ProjectMemberResponse> {
    return runMockOperation(
      () => {
        const member = membersDb.find(
          (m) => m.projectId === projectId && m.id === memberId
        );
        if (!member) {
          throw new ApiClientError({
            status: 404,
            errorCode: 'PROJ_005',
            message: 'Không tìm thấy thành viên trong dự án.',
          });
        }
        member.role = role;
        return structuredClone(member);
      },
      { requireLeader: true, disallowSupervisor: true }
    );
  },

  async removeMember(projectId: number, memberId: number): Promise<void> {
    return runMockOperation(
      () => {
        membersDb = membersDb.filter(
          (m) => !(m.projectId === projectId && m.id === memberId)
        );
      },
      { requireLeader: true, disallowSupervisor: true }
    );
  },

  async listInvitations(
    projectId: number
  ): Promise<ProjectInvitationResponse[]> {
    return runMockOperation(
      () => invitationsDb.filter((inv) => inv.projectId === projectId),
      { emptyFallback: [] }
    );
  },

  async sendInvitation(
    projectId: number,
    request: SendInvitationRequest
  ): Promise<ProjectInvitationResponse> {
    return runMockOperation(
      () => {
        const normalized = request.email.trim().toLowerCase();
        const isAlreadyMember = membersDb.some(
          (m) =>
            m.projectId === projectId &&
            m.user.email.toLowerCase() === normalized
        );
        if (isAlreadyMember) {
          throw new ApiClientError({
            status: 409,
            errorCode: 'PROJ_004',
            message: 'Email này đã là thành viên của dự án.',
          });
        }

        const isPending = invitationsDb.some(
          (inv) =>
            inv.projectId === projectId &&
            inv.status === 'PENDING' &&
            inv.invitedEmail.toLowerCase() === normalized
        );
        if (isPending) {
          throw new ApiClientError({
            status: 409,
            errorCode: 'INV_002',
            message: 'Đã có lời mời đang chờ xác nhận cho email này.',
          });
        }

        const created: ProjectInvitationResponse = {
          id: nextId++,
          projectId,
          invitedEmail: normalized,
          role: request.role,
          token: crypto.randomUUID(),
          status: 'PENDING',
          invitedByName: 'Nguyễn Quang Tâm',
          expiresAt: new Date(Date.now() + 7 * 86_400_000).toISOString(),
          acceptedAt: null,
          createdAt: new Date().toISOString(),
        };
        invitationsDb = [created, ...invitationsDb];
        return structuredClone(created);
      },
      { requireLeader: true, disallowSupervisor: true }
    );
  },

  async resendInvitation(
    projectId: number,
    invitationId: number
  ): Promise<ProjectInvitationResponse> {
    return runMockOperation(
      () => {
        const target = invitationsDb.find(
          (inv) => inv.projectId === projectId && inv.id === invitationId
        );
        if (!target || target.status !== 'PENDING') {
          throw new ApiClientError({
            status: 400,
            errorCode: 'INV_001',
            message: 'Chỉ có thể gửi lại lời mời đang ở trạng thái chờ.',
          });
        }
        target.expiresAt = new Date(Date.now() + 7 * 86_400_000).toISOString();
        return structuredClone(target);
      },
      { requireLeader: true, disallowSupervisor: true }
    );
  },

  async revokeInvitation(
    projectId: number,
    invitationId: number
  ): Promise<void> {
    return runMockOperation(
      () => {
        const target = invitationsDb.find(
          (inv) => inv.projectId === projectId && inv.id === invitationId
        );
        if (target) {
          target.status = 'REVOKED';
        }
      },
      { requireLeader: true, disallowSupervisor: true }
    );
  },

  async rotateMagicLink(
    projectId: number
  ): Promise<{ supervisorMagicToken: string }> {
    return runMockOperation(
      () => {
        const project = projectsDb.find((p) => p.id === projectId);
        if (!project) {
          throw new ApiClientError({
            status: 404,
            errorCode: 'PROJ_001',
            message: 'Không tìm thấy dự án.',
          });
        }
        const nextToken = crypto.randomUUID();
        project.supervisorMagicToken = nextToken;
        return { supervisorMagicToken: nextToken };
      },
      { requireLeader: true, disallowSupervisor: true }
    );
  },

  async listRepositories(
    projectId: number
  ): Promise<ProjectGitRepositoryResponse[]> {
    return runMockOperation(
      () => reposDb.filter((r) => r.projectId === projectId),
      { emptyFallback: [] }
    );
  },

  async createRepository(
    projectId: number,
    request: CreateGitRepositoryRequest
  ): Promise<ProjectGitRepositoryResponse> {
    return runMockOperation(
      () => {
        const normalized = request.repoFullName.trim().toLowerCase();
        const duplicate = reposDb.some(
          (r) =>
            r.projectId === projectId &&
            r.repoFullName.toLowerCase() === normalized
        );
        if (duplicate) {
          throw new ApiClientError({
            status: 409,
            errorCode: 'PROJ_007',
            message: 'Repository này đã được liên kết với dự án.',
          });
        }
        const id = nextId++;
        const created: ProjectGitRepositoryResponse = {
          id,
          projectId,
          repoLabel: request.repoLabel.trim(),
          repoFullName: request.repoFullName.trim(),
          githubRepoUrl: request.githubRepoUrl.trim(),
          webhookUrl: `https://api.teama.example/api/webhooks/github/${id}`,
          webhookSecret: `whsec_${crypto.randomUUID().slice(0, 16)}`,
          createdAt: new Date().toISOString(),
        };
        reposDb = [...reposDb, created];
        return structuredClone(created);
      },
      { requireLeader: true, disallowSupervisor: true }
    );
  },

  async deleteRepository(projectId: number, repoId: number): Promise<void> {
    return runMockOperation(
      () => {
        reposDb = reposDb.filter(
          (r) => !(r.projectId === projectId && r.id === repoId)
        );
      },
      { requireLeader: true, disallowSupervisor: true }
    );
  },
};
