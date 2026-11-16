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

/**
 * Field-level validation errors returned under data when errorCode === 'VAL_001'.
 */
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
  | 'PROJ_001'
  | 'PROJ_002'
  | 'PROJ_003'
  | 'PROJ_004'
  | 'PROJ_005'
  | 'PROJ_006'
  | 'PROJ_007'
  | 'MILE_001'
  | 'MILE_002'
  | 'FILE_413'
  | 'FILE_001'
  | 'FILE_002'
  | 'FILE_003'
  | 'FILE_004'
  | 'FILE_005'
  | 'PLAN_001'
  | 'PLAN_002'
  | 'PLAN_003'
  | 'PLAN_004'
  | 'PLAN_005'
  | 'SRS_001'
  | 'SRS_002'
  | 'SRS_003'
  | 'SRS_004'
  | 'TEST_001'
  | 'TEST_002'
  | 'TRACK_001'
  | 'ANALYTIC_001'
  | 'EXPORT_001';
