import type {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  UpdateStudentProfileRequest,
  UserResponse,
} from '@/types';

export interface AuthService {
  login(request: LoginRequest): Promise<AuthResponse>;
  register(request: RegisterRequest): Promise<AuthResponse>;
  getCurrentUser(): Promise<UserResponse>;
  updateStudentProfile(
    request: UpdateStudentProfileRequest
  ): Promise<UserResponse>;
}
