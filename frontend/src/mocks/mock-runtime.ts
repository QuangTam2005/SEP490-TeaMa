import { useSyncExternalStore } from 'react';
import { ApiClientError } from '@/lib/api-client';
import type { SimulatedUserRole, ValidationErrorMap } from '@/types';

export type MockScenario =
  | 'normal'
  | 'empty'
  | 'slow'
  | 'error_400'
  | 'error_403'
  | 'error_404'
  | 'error_500'
  | 'offline';

export interface MockRuntimeState {
  scenario: MockScenario;
  simulatedRole: SimulatedUserRole;
  resetCounter: number;
}

const STORAGE_KEY = 'sep490_teama_mock_runtime_v1';

const DEFAULT_STATE: MockRuntimeState = {
  scenario: 'normal',
  simulatedRole: 'LEADER',
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
    return {
      scenario: parsed.scenario ?? DEFAULT_STATE.scenario,
      simulatedRole: parsed.simulatedRole ?? DEFAULT_STATE.simulatedRole,
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
      // Ignore storage quota / privacy mode errors in dev preview
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

export function setSimulatedRole(simulatedRole: SimulatedUserRole): void {
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
  requireLeader?: boolean;
  disallowSupervisor?: boolean;
  emptyFallback?: T;
  validationErrors?: ValidationErrorMap;
  ignoreGlobalScenario?: boolean;
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

/**
 * Executes a mock service operation with realistic latency, role enforcement,
 * and real ApiClientError simulation so UI error boundaries are exercised identically to production.
 */
export async function runMockOperation<T>(
  executor: () => T | Promise<T>,
  options: MockOperationOptions<T> = {}
): Promise<T> {
  const { scenario, simulatedRole } = getMockRuntimeState();
  const delayMs = !options.ignoreGlobalScenario && scenario === 'slow' ? 1200 : 220;
  await sleep(delayMs);

  if (options.disallowSupervisor && simulatedRole === 'SUPERVISOR') {
    throw new ApiClientError({
      status: 403,
      errorCode: 'AUTH_004',
      message: 'Chế độ Giảng viên (Chỉ đọc) không được phép thực hiện thao tác thay đổi dữ liệu.',
    });
  }

  if (options.requireLeader && simulatedRole !== 'LEADER') {
    throw new ApiClientError({
      status: 403,
      errorCode: 'PROJ_003',
      message: 'Chỉ Nhóm trưởng (Leader) mới có quyền thực hiện thao tác này.',
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
          message: 'Dữ liệu đầu vào không hợp lệ. Vui lòng kiểm tra các trường được đánh dấu.',
          fieldErrors: options.validationErrors ?? {
            email: 'Định dạng email không hợp lệ',
            projectCode: 'Mã dự án không được để trống',
          },
        });
      case 'error_403':
        throw new ApiClientError({
          status: 403,
          errorCode: 'PROJ_003',
          message: 'Chỉ Nhóm trưởng (Leader) mới có quyền thực hiện thao tác này.',
        });
      case 'error_404':
        throw new ApiClientError({
          status: 404,
          errorCode: 'PROJ_001',
          message: 'Không tìm thấy dự án hoặc dữ liệu đã bị xóa.',
        });
      case 'error_500':
        throw new ApiClientError({
          status: 500,
          errorCode: 'SYS_500',
          message: 'Hệ thống đang gặp sự cố nội bộ (Simulated 500). Vui lòng thử lại sau.',
        });
      case 'offline':
        throw new ApiClientError({
          status: 0,
          errorCode: null,
          message: 'Không thể kết nối tới máy chủ. Vui lòng kiểm tra kết nối mạng.',
        });
      case 'normal':
      case 'slow':
        break;
    }
  }

  return executor();
}
