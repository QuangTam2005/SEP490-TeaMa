import { IS_MOCK_MODE } from './config';
import type { AuthService } from './auth.service';
import { authHttpService } from './http/auth.http-service';
import { authMockService } from './mock/auth.mock-service';

export const authService: AuthService = IS_MOCK_MODE
  ? authMockService
  : authHttpService;

export const services = {
  auth: authService,
};

export { DATA_MODE, IS_MOCK_MODE } from './config';
