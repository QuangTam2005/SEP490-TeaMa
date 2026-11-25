import { apiGet, apiPost } from '@/lib/api-client';
import type { LinkService } from '@/services/link.service';
import type { LinkCodeResponse, LinkedChildResponse } from '@/types';

export const linkHttpService: LinkService = {
  getCurrentLink: () => apiGet<LinkCodeResponse | null>('/api/links/current'),
  generateLinkCode: () => apiPost<LinkCodeResponse>('/api/links/code', {}),
  acceptLinkCode: (linkCode: string) =>
    apiPost<LinkCodeResponse>('/api/links/accept', { linkCode }),
  getLinkedChildren: () => apiGet<LinkedChildResponse[]>('/api/links/children'),
  unlink: async (studentId?: number) => {
    await apiPost<null>(
      '/api/links/unlink',
      studentId ? { studentId } : {}
    );
  },
};
