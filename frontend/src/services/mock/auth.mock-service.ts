import { ApiClientError } from '@/lib/api-client';
import { setAccessToken } from '@/lib/auth-token';
import {
  getMockRuntimeState,
  registerMockResetHook,
  runMockOperation,
} from '@/mocks/mock-runtime';
import type { AuthService } from '@/services/auth.service';
import type {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  UserResponse,
} from '@/types';

const INITIAL_USERS: UserResponse[] = [
  {
    id: 1,
    email: 'hocsinh.lop6@gmail.com',
    fullName: 'Nguyễn Minh Khôi',
    role: 'STUDENT',
    grade: 6,
    linkedStudentName: null,
    createdAt: '2026-11-25T01:00:00Z',
  },
  {
    id: 2,
    email: 'phuhuynh@gmail.com',
    fullName: 'Nguyễn Thu Hằng',
    role: 'GUARDIAN',
    grade: null,
    linkedStudentName: 'Nguyễn Minh Khôi',
    createdAt: '2026-11-25T01:05:00Z',
  },
];

let usersDb: UserResponse[] = structuredClone(INITIAL_USERS);
let activeUserEmail = INITIAL_USERS[0].email;
let nextUserId = 100;

registerMockResetHook(() => {
  usersDb = structuredClone(INITIAL_USERS);
  activeUserEmail = INITIAL_USERS[0].email;
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

      const { simulatedRole } = getMockRuntimeState();
      const created: UserResponse = {
        id: nextUserId++,
        email: normalized,
        fullName: normalized.split('@')[0],
        role: simulatedRole,
        grade: simulatedRole === 'STUDENT' ? 6 : null,
        linkedStudentName: simulatedRole === 'GUARDIAN' ? 'Nguyễn Minh Khôi' : null,
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
        role: request.role,
        grade: request.role === 'STUDENT' ? 6 : null,
        linkedStudentName: null,
        createdAt: new Date().toISOString(),
      };
      usersDb.push(newUser);
      return buildMockAuthResponse(newUser);
    });
  },

  async getCurrentUser(): Promise<UserResponse> {
    return runMockOperation(() => {
      const { simulatedRole } = getMockRuntimeState();
      const byEmail = usersDb.find(
        (u) => u.email.toLowerCase() === activeUserEmail.toLowerCase()
      );
      if (byEmail && byEmail.role === simulatedRole) {
        return byEmail;
      }
      return usersDb.find((u) => u.role === simulatedRole) ?? usersDb[0];
    });
  },
};
