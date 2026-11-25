import type { LinkCodeResponse } from '@/types';

export interface LinkService {
  getCurrentLink(): Promise<LinkCodeResponse | null>;
  generateLinkCode(): Promise<LinkCodeResponse>;
  acceptLinkCode(linkCode: string): Promise<LinkCodeResponse>;
}
