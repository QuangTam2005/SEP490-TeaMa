export type UserRole = 'STUDENT' | 'GUARDIAN';

export interface UserResponse {
  id: number;
  email: string;
  fullName: string;
  role: UserRole;
  grade: number | null;
  linkedStudentName: string | null;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresInMs: number;
  user: UserResponse;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
  role: UserRole;
  grade?: number | null;
}

export interface UpdateStudentProfileRequest {
  grade: number;
  displayName?: string;
}

export interface UpdateProfileRequest {
  fullName: string;
  grade?: number | null;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}
