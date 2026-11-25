import type {
  AuthResponse,
  ChangePasswordRequest,
  LoginRequest,
  RegisterRequest,
  UpdateProfileRequest,
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
  updateProfile(request: UpdateProfileRequest): Promise<UserResponse>;
  changePassword(request: ChangePasswordRequest): Promise<void>;
}
