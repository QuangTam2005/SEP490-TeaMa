import { IS_MOCK_MODE } from './config';
import type { AuthService } from './auth.service';
import type { LinkService } from './link.service';
import { authHttpService } from './http/auth.http-service';
import { linkHttpService } from './http/link.http-service';
import { authMockService } from './mock/auth.mock-service';
import { linkMockService } from './mock/link.mock-service';

export const authService: AuthService = IS_MOCK_MODE
  ? authMockService
  : authHttpService;

export const linkService: LinkService = IS_MOCK_MODE
  ? linkMockService
  : linkHttpService;

export const services = {
  auth: authService,
  link: linkService,
};

export { DATA_MODE, IS_MOCK_MODE } from './config';
