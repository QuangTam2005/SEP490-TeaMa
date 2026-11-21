import { ApiClientError } from '@/lib/api-client';
import { setAccessToken } from '@/lib/auth-token';
import {
  SEED_INVITATIONS,
  SEED_PROJECTS,
  SEED_USERS,
} from '@/mocks/fixtures/seed-data';
import {
  getMockRuntimeState,
  registerMockResetHook,
  runMockOperation,
} from '@/mocks/mock-runtime';
import type { AuthService } from '@/services/auth.service';
import type {
  AuthResponse,
  InvitationVerifyResponse,
  LoginRequest,
  RegisterRequest,
  UserResponse,
} from '@/types';

let usersDb: UserResponse[] = structuredClone(SEED_USERS);
let activeUserEmail = SEED_USERS[0].email;
let nextUserId = 100;

registerMockResetHook(() => {
  usersDb = structuredClone(SEED_USERS);
  activeUserEmail = SEED_USERS[0].email;
  nextUserId = 100;
});

function buildMockAuthResponse(user: UserResponse): AuthResponse {
  const token = `mock-jwt-token-${user.id}`;
  setAccessToken(token);
  activeUserEmail = user.email;
  return {
    accessToken: token,
    tokenType: 'Bearer',
    expiresInMs: 86_400_000,
    user,
  };
}

export const authMockService: AuthService = {
  async login(request: LoginRequest): Promise<AuthResponse> {
    return runMockOperation(() => {
      const normalized = request.email.trim().toLowerCase();
      if (!normalized || !request.password) {
        throw new ApiClientError({
          status: 400,
          errorCode: 'VAL_001',
          message: 'Vui lòng nhập đầy đủ email và mật khẩu.',
          fieldErrors: {
            ...(normalized ? {} : { email: 'Email không được để trống' }),
            ...(request.password ? {} : { password: 'Mật khẩu không được để trống' }),
          },
        });
      }

      if (request.password === 'wrongpass') {
        throw new ApiClientError({
          status: 401,
          errorCode: 'AUTH_002',
          message: 'Email hoặc mật khẩu không chính xác.',
        });
      }

      const existing = usersDb.find(
        (u) => u.email.toLowerCase() === normalized
      );
      if (existing) {
        return buildMockAuthResponse(existing);
      }

      // In mock mode, allow logging in with any valid non-blocked email for quick UI testing
      const created: UserResponse = {
        id: nextUserId++,
        email: normalized,
        fullName: normalized.split('@')[0],
        studentCode: null,
        phone: null,
        avatarUrl: null,
        createdAt: new Date().toISOString(),
      };
      usersDb.push(created);
      return buildMockAuthResponse(created);
    });
  },

  async register(request: RegisterRequest): Promise<AuthResponse> {
    return runMockOperation(() => {
      const normalized = request.email.trim().toLowerCase();
      const duplicate = usersDb.some(
        (u) => u.email.toLowerCase() === normalized
      );
      if (duplicate) {
        throw new ApiClientError({
          status: 409,
          errorCode: 'AUTH_005',
          message: 'Địa chỉ email này đã được đăng ký tài khoản.',
        });
      }

      const newUser: UserResponse = {
        id: nextUserId++,
        email: normalized,
        fullName: request.fullName.trim(),
        studentCode: request.studentCode?.trim() || null,
        phone: request.phone?.trim() || null,
        avatarUrl: null,
        createdAt: new Date().toISOString(),
      };
      usersDb.push(newUser);
      return buildMockAuthResponse(newUser);
    });
  },

  async getCurrentUser(): Promise<UserResponse> {
    return runMockOperation(() => {
      const { simulatedRole } = getMockRuntimeState();
      if (simulatedRole === 'MEMBER') {
        return usersDb[1] ?? usersDb[0];
      }
      return (
        usersDb.find(
          (u) => u.email.toLowerCase() === activeUserEmail.toLowerCase()
        ) ?? usersDb[0]
      );
    });
  },

  async verifyInvitation(token: string): Promise<InvitationVerifyResponse> {
    return runMockOperation(() => {
      const invite = SEED_INVITATIONS.find((item) => item.token === token);
      if (!invite || invite.status !== 'PENDING') {
        throw new ApiClientError({
          status: 410,
          errorCode: 'INV_001',
          message: 'Liên kết lời mời không hợp lệ, đã hết hạn hoặc đã bị thu hồi.',
        });
      }

      const project =
        SEED_PROJECTS.find((p) => p.id === invite.projectId) ?? SEED_PROJECTS[0];
      const accountExists = usersDb.some(
        (u) => u.email.toLowerCase() === invite.invitedEmail.toLowerCase()
      );

      return {
        token: invite.token,
        projectId: project.id,
        projectCode: project.projectCode,
        projectName: project.projectName,
        groupName: project.groupName,
        invitedEmail: invite.invitedEmail,
        role: invite.role,
        invitedByName: invite.invitedByName,
        expiresAt: invite.expiresAt,
        accountExists,
      };
    });
  },

  async acceptInvitation(token: string): Promise<{ projectId: number }> {
    return runMockOperation(() => {
      const invite = SEED_INVITATIONS.find((item) => item.token === token);
      if (!invite || invite.status !== 'PENDING') {
        throw new ApiClientError({
          status: 410,
          errorCode: 'INV_001',
          message: 'Liên kết lời mời không hợp lệ, đã hết hạn hoặc đã bị thu hồi.',
        });
      }

      if (
        token === 'mismatch' ||
        invite.invitedEmail.toLowerCase() !== activeUserEmail.toLowerCase()
      ) {
        throw new ApiClientError({
          status: 403,
          errorCode: 'INV_003',
          message: `Email tài khoản hiện tại (${activeUserEmail}) không khớp với email được mời (${invite.invitedEmail}).`,
        });
      }

      return { projectId: invite.projectId };
    });
  },
};
