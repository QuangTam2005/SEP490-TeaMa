import type { LinkCodeResponse, LinkedChildResponse } from '@/types';

export interface LinkService {
  getCurrentLink(): Promise<LinkCodeResponse | null>;
  generateLinkCode(): Promise<LinkCodeResponse>;
  acceptLinkCode(linkCode: string): Promise<LinkCodeResponse>;
  getLinkedChildren(): Promise<LinkedChildResponse[]>;
  unlink(studentId?: number): Promise<void>;
}
