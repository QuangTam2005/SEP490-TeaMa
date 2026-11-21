import type {
  AuthResponse,
  InvitationVerifyResponse,
  LoginRequest,
  RegisterRequest,
  UserResponse,
} from '@/types';

export interface AuthService {
  login(request: LoginRequest): Promise<AuthResponse>;
  register(request: RegisterRequest): Promise<AuthResponse>;
  getCurrentUser(): Promise<UserResponse>;
  verifyInvitation(token: string): Promise<InvitationVerifyResponse>;
  acceptInvitation(token: string): Promise<{ projectId: number }>;
}
