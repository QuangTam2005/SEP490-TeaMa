import { apiGet, apiPost } from '@/lib/api-client';
import type { LinkService } from '@/services/link.service';
import type { LinkCodeResponse } from '@/types';

export const linkHttpService: LinkService = {
  getCurrentLink: () => apiGet<LinkCodeResponse | null>('/api/links/current'),
  generateLinkCode: () => apiPost<LinkCodeResponse>('/api/links/code', {}),
  acceptLinkCode: (linkCode: string) =>
    apiPost<LinkCodeResponse>('/api/links/accept', { linkCode }),
};
