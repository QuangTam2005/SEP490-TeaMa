export type ProjectRole = 'LEADER' | 'MEMBER';

export type SimulatedUserRole = ProjectRole | 'SUPERVISOR';

export interface UserResponse {
  id: number;
  email: string;
  fullName: string;
  studentCode: string | null;
  phone: string | null;
  avatarUrl: string | null;
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
  studentCode?: string | null;
  phone?: string | null;
}
