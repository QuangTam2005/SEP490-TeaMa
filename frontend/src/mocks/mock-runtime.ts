import { useSyncExternalStore } from 'react';
import { ApiClientError } from '@/lib/api-client';
import type { UserRole, ValidationErrorMap } from '@/types';

export type MockScenario =
  | 'normal'
  | 'empty'
  | 'slow'
  | 'evaluating'
  | 'evaluation_failed'
  | 'insufficient_evidence'
  | 'error_400'
  | 'error_403'
  | 'error_404'
  | 'error_500'
  | 'offline';

export interface MockRuntimeState {
  scenario: MockScenario;
  simulatedRole: UserRole;
  resetCounter: number;
}

const STORAGE_KEY = 'asc_mock_runtime_v1';

const DEFAULT_STATE: MockRuntimeState = {
  scenario: 'normal',
  simulatedRole: 'STUDENT',
  resetCounter: 0,
};

let currentState: MockRuntimeState = loadInitialState();
const listeners = new Set<() => void>();
const resetHooks = new Set<() => void>();

function loadInitialState(): MockRuntimeState {
  if (typeof window === 'undefined') {
    return { ...DEFAULT_STATE };
  }
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return { ...DEFAULT_STATE };
    }
    const parsed = JSON.parse(raw) as Partial<MockRuntimeState>;
    const validRole: UserRole =
      parsed.simulatedRole === 'GUARDIAN' ? 'GUARDIAN' : 'STUDENT';
    return {
      scenario: parsed.scenario ?? DEFAULT_STATE.scenario,
      simulatedRole: validRole,
      resetCounter: 0,
    };
  } catch {
    return { ...DEFAULT_STATE };
  }
}

function persistAndNotify(): void {
  if (typeof window !== 'undefined') {
    try {
      window.localStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({
          scenario: currentState.scenario,
          simulatedRole: currentState.simulatedRole,
        })
      );
    } catch {
      // Ignore storage quota / privacy mode errors safely
    }
  }
  listeners.forEach((listener) => listener());
}

export function getMockRuntimeState(): MockRuntimeState {
  return currentState;
}

export function setMockScenario(scenario: MockScenario): void {
  currentState = { ...currentState, scenario };
  persistAndNotify();
}

export function setSimulatedRole(simulatedRole: UserRole): void {
  currentState = { ...currentState, simulatedRole };
  persistAndNotify();
}

export function registerMockResetHook(hook: () => void): () => void {
  resetHooks.add(hook);
  return () => {
    resetHooks.delete(hook);
  };
}

export function resetMockRuntime(): void {
  resetHooks.forEach((hook) => hook());
  currentState = {
    ...DEFAULT_STATE,
    resetCounter: currentState.resetCounter + 1,
  };
  persistAndNotify();
}

export function subscribeMockRuntime(listener: () => void): () => void {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

export function useMockRuntime(): MockRuntimeState {
  return useSyncExternalStore(
    subscribeMockRuntime,
    getMockRuntimeState,
    () => DEFAULT_STATE
  );
}

export interface MockOperationOptions<T> {
  requireRole?: UserRole;
  requireRoles?: UserRole[];
  emptyFallback?: T;
  validationErrors?: ValidationErrorMap;
  ignoreGlobalScenario?: boolean;
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

export async function runMockOperation<T>(
  executor: () => T | Promise<T>,
  options: MockOperationOptions<T> = {}
): Promise<T> {
  const { scenario, simulatedRole } = getMockRuntimeState();
  const delayMs = !options.ignoreGlobalScenario && scenario === 'slow' ? 1200 : 200;
  await sleep(delayMs);

  if (options.requireRole && simulatedRole !== options.requireRole) {
    throw new ApiClientError({
      status: 403,
      errorCode: 'AUTH_004',
      message: 'Vai trò hiện tại không có quyền thực hiện thao tác này.',
    });
  }

  if (options.requireRoles && !options.requireRoles.includes(simulatedRole)) {
    throw new ApiClientError({
      status: 403,
      errorCode: 'AUTH_004',
      message: 'Vai trò hiện tại không có quyền thực hiện thao tác này.',
    });
  }

  if (!options.ignoreGlobalScenario) {
    switch (scenario) {
      case 'empty':
        if (options.emptyFallback !== undefined) {
          return options.emptyFallback;
        }
        break;
      case 'error_400':
        throw new ApiClientError({
          status: 400,
          errorCode: 'VAL_001',
          message: 'Dữ liệu đầu vào không hợp lệ.',
          fieldErrors: options.validationErrors ?? {
            email: 'Định dạng email không hợp lệ',
          },
        });
      case 'error_403':
        throw new ApiClientError({
          status: 403,
          errorCode: 'GRD_001',
          message: 'Tài khoản Guardian chưa được liên kết với hồ sơ học sinh này.',
        });
      case 'error_404':
        throw new ApiClientError({
          status: 404,
          errorCode: 'STUDY_001',
          message: 'Không tìm thấy nhiệm vụ học tập.',
        });
      case 'error_500':
        throw new ApiClientError({
          status: 500,
          errorCode: 'SYS_500',
          message: 'Hệ thống đang gặp sự cố nội bộ. Vui lòng thử lại sau.',
        });
      case 'offline':
        throw new ApiClientError({
          status: 0,
          errorCode: null,
          message: 'Không thể kết nối tới máy chủ. Vui lòng kiểm tra kết nối mạng.',
        });
      case 'normal':
      case 'slow':
      case 'evaluating':
      case 'evaluation_failed':
      case 'insufficient_evidence':
        break;
    }
  }

  return executor();
}
