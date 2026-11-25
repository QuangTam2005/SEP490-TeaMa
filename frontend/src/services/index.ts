import { IS_MOCK_MODE } from './config';
import type { AuthService } from './auth.service';
import type { GuardianService } from './guardian.service';
import type { LinkService } from './link.service';
import type { StudyService } from './study.service';
import type { TimetableService } from './timetable.service';
import { authHttpService } from './http/auth.http-service';
import { guardianHttpService } from './http/guardian.http-service';
import { linkHttpService } from './http/link.http-service';
import { studyHttpService } from './http/study.http-service';
import { timetableHttpService } from './http/timetable.http-service';
import { authMockService } from './mock/auth.mock-service';
import { guardianMockService } from './mock/guardian.mock-service';
import { linkMockService } from './mock/link.mock-service';
import { studyMockService } from './mock/study.mock-service';
import { timetableMockService } from './mock/timetable.mock-service';

export const authService: AuthService = IS_MOCK_MODE
  ? authMockService
  : authHttpService;

export const linkService: LinkService = IS_MOCK_MODE
  ? linkMockService
  : linkHttpService;

export const timetableService: TimetableService = IS_MOCK_MODE
  ? timetableMockService
  : timetableHttpService;

export const studyService: StudyService = IS_MOCK_MODE
  ? studyMockService
  : studyHttpService;

export const guardianService: GuardianService = IS_MOCK_MODE
  ? guardianMockService
  : guardianHttpService;

export const services = {
  auth: authService,
  link: linkService,
  timetable: timetableService,
  study: studyService,
  guardian: guardianService,
};

export { DATA_MODE, IS_MOCK_MODE } from './config';
