import { apiGet, apiPost, apiPut } from '@/lib/api-client';
import type { AuthService } from '@/services/auth.service';
import type {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  UpdateStudentProfileRequest,
  UserResponse,
} from '@/types';

export const authHttpService: AuthService = {
  login: (request: LoginRequest) =>
    apiPost<AuthResponse>('/api/auth/login', request),

  register: (request: RegisterRequest) =>
    apiPost<AuthResponse>('/api/auth/register', request),

  getCurrentUser: () => apiGet<UserResponse>('/api/auth/me'),

  updateStudentProfile: (request: UpdateStudentProfileRequest) =>
    apiPut<UserResponse>('/api/student/profile', request),
};
