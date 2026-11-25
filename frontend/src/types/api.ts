/**
 * Unified REST API response envelope matching Backend ApiResponse<T>.
 */
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T | null;
  errorCode: string | null;
  timestamp: string;
}

export type ValidationErrorMap = Record<string, string>;

/**
 * Granular domain-prefixed error codes matching Backend ErrorCode.java 1-to-1.
 */
export type ApiErrorCode =
  | 'SYS_500'
  | 'SYS_400'
  | 'SYS_404'
  | 'VAL_001'
  | 'VAL_002'
  | 'VAL_003'
  | 'AUTH_001'
  | 'AUTH_002'
  | 'AUTH_003'
  | 'AUTH_004'
  | 'AUTH_005'
  | 'AUTH_006'
  | 'AUTH_007'
  | 'AUTH_008'
  | 'SCH_001'
  | 'SCH_002'
  | 'SCH_003'
  | 'STUDY_001'
  | 'STUDY_002'
  | 'STUDY_003'
  | 'STUDY_004'
  | 'STUDY_005'
  | 'STUDY_006'
  | 'AI_001'
  | 'AI_002'
  | 'AI_003'
  | 'AI_004'
  | 'GRD_001';
