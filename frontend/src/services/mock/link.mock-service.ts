import { ApiClientError } from '@/lib/api-client';
import {
  getMockRuntimeState,
  registerMockResetHook,
  runMockOperation,
} from '@/mocks/mock-runtime';
import type { LinkService } from '@/services/link.service';
import type { LinkCodeResponse, LinkedChildResponse } from '@/types';

const CODE_ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';

const INITIAL_STUDENT_LINK: LinkCodeResponse = {
  linkCode: 'KHOI2026',
  expiresAt: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
  status: 'PENDING',
  linkedUserName: null,
};

let studentLinkState: LinkCodeResponse | null = structuredClone(INITIAL_STUDENT_LINK);
let guardianLinkState: LinkCodeResponse | null = {
  linkCode: 'KHOI2026',
  expiresAt: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
  status: 'ACTIVE',
  linkedUserName: 'Nguyễn Minh Khôi',
};

registerMockResetHook(() => {
  studentLinkState = structuredClone(INITIAL_STUDENT_LINK);
  guardianLinkState = {
    linkCode: 'KHOI2026',
    expiresAt: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
    status: 'ACTIVE',
    linkedUserName: 'Nguyễn Minh Khôi',
  };
});

function randomEightCharCode(): string {
  let out = '';
  for (let i = 0; i < 8; i += 1) {
    const idx = Math.floor(Math.random() * CODE_ALPHABET.length);
    out += CODE_ALPHABET[idx];
  }
  return out;
}

export const linkMockService: LinkService = {
  async getCurrentLink(): Promise<LinkCodeResponse | null> {
    return runMockOperation(
      () => {
        const { simulatedRole } = getMockRuntimeState();
        return simulatedRole === 'GUARDIAN'
          ? guardianLinkState
            ? { ...guardianLinkState }
            : null
          : studentLinkState
            ? { ...studentLinkState }
            : null;
      },
      { emptyFallback: null }
    );
  },

  async generateLinkCode(): Promise<LinkCodeResponse> {
    return runMockOperation(
      () => {
        const next: LinkCodeResponse = {
          linkCode: randomEightCharCode(),
          expiresAt: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
          status: 'PENDING',
          linkedUserName: null,
        };
        studentLinkState = next;
        return { ...next };
      },
      { requireRole: 'STUDENT' }
    );
  },

  async acceptLinkCode(linkCode: string): Promise<LinkCodeResponse> {
    return runMockOperation(
      () => {
        const normalized = linkCode.trim().toUpperCase();
        if (normalized.length !== 8) {
          throw new ApiClientError({
            status: 400,
            errorCode: 'VAL_001',
            message: 'Mã liên kết phải gồm đúng 8 ký tự.',
            fieldErrors: {
              linkCode: 'Mã liên kết phải gồm đúng 8 ký tự.',
            },
          });
        }
        if (normalized === 'EXPIRE00' || normalized === 'USED0000') {
          throw new ApiClientError({
            status: 400,
            errorCode: 'AUTH_007',
            message: 'Mã liên kết đã được sử dụng hoặc đã hết hạn.',
            fieldErrors: {
              linkCode: 'Mã liên kết đã dùng hoặc hết hạn.',
            },
          });
        }
        if (normalized === 'INVALID0') {
          throw new ApiClientError({
            status: 404,
            errorCode: 'AUTH_008',
            message: 'Không tìm thấy mã liên kết phù hợp.',
            fieldErrors: {
              linkCode: 'Mã liên kết không tồn tại.',
            },
          });
        }

        const linked: LinkCodeResponse = {
          linkCode: normalized,
          expiresAt: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
          status: 'ACTIVE',
          linkedUserName: 'Nguyễn Minh Khôi',
        };
        guardianLinkState = linked;
        if (studentLinkState && studentLinkState.linkCode === normalized) {
          studentLinkState = {
            ...studentLinkState,
            status: 'ACTIVE',
            linkedUserName: 'Nguyễn Thu Hằng',
          };
        }
        return { ...linked };
      },
      { requireRole: 'GUARDIAN' }
    );
  },

  async getLinkedChildren(): Promise<LinkedChildResponse[]> {
    return runMockOperation(
      () =>
        guardianLinkState && guardianLinkState.status === 'ACTIVE'
          ? [
              {
                studentId: 10,
                studentUserId: 1,
                displayName: guardianLinkState.linkedUserName ?? 'Nguyễn Minh Khôi',
                grade: 6,
                linkedAt: new Date().toISOString(),
              },
            ]
          : [],
      { requireRole: 'GUARDIAN', emptyFallback: [] }
    );
  },

  async unlink(): Promise<void> {
    return runMockOperation(() => {
      const { simulatedRole } = getMockRuntimeState();
      if (simulatedRole === 'GUARDIAN') {
        guardianLinkState = null;
      } else {
        studentLinkState = null;
      }
    });
  },
};
