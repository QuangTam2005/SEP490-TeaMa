import { IS_MOCK_MODE } from './config';
import type { AuthService } from './auth.service';
import type { ProjectService } from './project.service';
import { authHttpService } from './http/auth.http-service';
import { projectHttpService } from './http/project.http-service';
import { authMockService } from './mock/auth.mock-service';
import { projectMockService } from './mock/project.mock-service';

export const authService: AuthService = IS_MOCK_MODE
  ? authMockService
  : authHttpService;

export const projectService: ProjectService = IS_MOCK_MODE
  ? projectMockService
  : projectHttpService;

export const services = {
  auth: authService,
  project: projectService,
};

export { DATA_MODE, IS_MOCK_MODE } from './config';
