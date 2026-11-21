import axios, {
  AxiosError,
  type AxiosProgressEvent,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from 'axios';
import {
  clearAccessToken,
  getAccessToken,
  resolveSupervisorTokenFromPath,
} from '@/lib/auth-token';
import type { ApiResponse, ValidationErrorMap } from '@/types/api';

const BASE_URL = (process.env.NEXT_PUBLIC_API_URL ?? '').replace(/\/$/, '');

export class ApiClientError extends Error {
  public readonly status: number;
  public readonly errorCode: string | null;
  public readonly fieldErrors: ValidationErrorMap | null;

  constructor(params: {
    message: string;
    status: number;
    errorCode?: string | null;
    fieldErrors?: ValidationErrorMap | null;
  }) {
    super(params.message);
    this.name = 'ApiClientError';
    this.status = params.status;
    this.errorCode = params.errorCode ?? null;
    this.fieldErrors = params.fieldErrors ?? null;
  }
}

export const apiClient = axios.create({
  baseURL: BASE_URL,
  timeout: 120_000,
  headers: {
    Accept: 'application/json',
  },
});

apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  // Multipart FormData: delete Content-Type so the browser generates the boundary
  if (typeof FormData !== 'undefined' && config.data instanceof FormData) {
    config.headers.delete('Content-Type');
  } else if (config.data !== undefined && !config.headers.has('Content-Type')) {
    config.headers.set('Content-Type', 'application/json');
  }

  // Dual Auth: prioritize Supervisor Magic Link context over Student JWT
  const explicitSupervisorToken = config.headers.get('X-Supervisor-Token');
  const routeSupervisorToken = resolveSupervisorTokenFromPath();
  const supervisorToken =
    typeof explicitSupervisorToken === 'string' && explicitSupervisorToken.length > 0
      ? explicitSupervisorToken
      : routeSupervisorToken;

  if (supervisorToken) {
    config.headers.set('X-Supervisor-Token', supervisorToken);
    config.headers.delete('Authorization');
  } else {
    const jwt = getAccessToken();
    if (jwt && !config.headers.has('Authorization')) {
      config.headers.set('Authorization', `Bearer ${jwt}`);
    }
  }

  return config;
});

function isValidationErrorMap(value: unknown): value is ValidationErrorMap {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) {
    return false;
  }
  return Object.values(value).every((item) => typeof item === 'string');
}

async function parseErrorPayload(
  rawData: unknown
): Promise<ApiResponse<ValidationErrorMap | null> | null> {
  if (typeof Blob !== 'undefined' && rawData instanceof Blob) {
    if (rawData.type.includes('application/json')) {
      try {
        const text = await rawData.text();
        return JSON.parse(text) as ApiResponse<ValidationErrorMap | null>;
      } catch {
        return null;
      }
    }
    return null;
  }

  if (typeof rawData === 'object' && rawData !== null && 'success' in rawData) {
    return rawData as ApiResponse<ValidationErrorMap | null>;
  }

  return null;
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<unknown>) => {
    const status = error.response?.status ?? 0;
    const payload = await parseErrorPayload(error.response?.data);

    const errorCode = payload?.errorCode ?? null;
    const message =
      payload?.message ??
      (status === 0
        ? 'Không thể kết nối tới máy chủ. Vui lòng kiểm tra mạng.'
        : 'Đã xảy ra lỗi khi xử lý yêu cầu.');

    const fieldErrors =
      errorCode === 'VAL_001' && isValidationErrorMap(payload?.data)
        ? payload.data
        : null;

    if (
      typeof window !== 'undefined' &&
      status === 401 &&
      (errorCode === 'AUTH_001' || errorCode === 'AUTH_003' || errorCode === null)
    ) {
      const pathname = window.location.pathname;
      const isPublicOrSupervisor =
        pathname === '/' ||
        pathname.startsWith('/login') ||
        pathname.startsWith('/register') ||
        pathname.startsWith('/invite') ||
        pathname.startsWith('/supervisor/');

      if (!isPublicOrSupervisor) {
        clearAccessToken();
        window.location.assign('/login');
      }
    }

    return Promise.reject(
      new ApiClientError({
        message,
        status,
        errorCode,
        fieldErrors,
      })
    );
  }
);

function unwrapResponse<T>(payload: ApiResponse<T>): T {
  if (!payload.success) {
    throw new ApiClientError({
      message: payload.message || 'Yêu cầu thất bại',
      status: 400,
      errorCode: payload.errorCode,
      fieldErrors: isValidationErrorMap(payload.data) ? payload.data : null,
    });
  }
  return payload.data as T;
}

export async function apiGet<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  const response = await apiClient.get<ApiResponse<T>>(url, config);
  return unwrapResponse(response.data);
}

export async function apiPost<T, B = unknown>(
  url: string,
  body?: B,
  config?: AxiosRequestConfig
): Promise<T> {
  const response = await apiClient.post<ApiResponse<T>>(url, body, config);
  return unwrapResponse(response.data);
}

export async function apiPut<T, B = unknown>(
  url: string,
  body?: B,
  config?: AxiosRequestConfig
): Promise<T> {
  const response = await apiClient.put<ApiResponse<T>>(url, body, config);
  return unwrapResponse(response.data);
}

export async function apiPatch<T, B = unknown>(
  url: string,
  body?: B,
  config?: AxiosRequestConfig
): Promise<T> {
  const response = await apiClient.patch<ApiResponse<T>>(url, body, config);
  return unwrapResponse(response.data);
}

export async function apiDelete<T = void>(
  url: string,
  config?: AxiosRequestConfig
): Promise<T> {
  const response = await apiClient.delete<ApiResponse<T>>(url, config);
  return unwrapResponse(response.data);
}

export async function apiUpload<T>(
  url: string,
  formData: FormData,
  onUploadProgress?: (percent: number) => void,
  config?: AxiosRequestConfig
): Promise<T> {
  const response = await apiClient.post<ApiResponse<T>>(url, formData, {
    ...config,
    onUploadProgress: (event: AxiosProgressEvent) => {
      if (!onUploadProgress || !event.total) {
        return;
      }
      const percent = Math.round((event.loaded * 100) / event.total);
      onUploadProgress(percent);
    },
  });
  return unwrapResponse(response.data);
}

export async function apiDownload(
  url: string,
  config?: AxiosRequestConfig
): Promise<Blob> {
  const response = await apiClient.get<Blob>(url, {
    ...config,
    responseType: 'blob',
  });
  return response.data;
}
