import { ApiClientError } from '@/lib/api-client';
import type { ValidationErrorMap } from '@/types';

export type FieldErrorSetter<TFieldName extends string = string> = (
  name: TFieldName,
  error: { type: string; message: string }
) => void;

/**
 * Extracts a safe, user-facing Vietnamese error message from unknown errors.
 */
export function extractApiErrorMessage(
  error: unknown,
  fallback = 'Đã xảy ra lỗi không mong muốn. Vui lòng thử lại.'
): string {
  if (error instanceof ApiClientError) {
    return error.message || fallback;
  }
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return fallback;
}

/**
 * Extracts backend field validation map when errorCode === 'VAL_001'.
 */
export function extractApiFieldErrors(
  error: unknown
): ValidationErrorMap | null {
  if (error instanceof ApiClientError && error.fieldErrors) {
    return error.fieldErrors;
  }
  return null;
}

/**
 * Applies backend VAL_001 field errors directly onto a form state setter
 * (compatible with React Hook Form's setError without hard-coupling).
 * Returns true if at least one field error was mapped.
 */
export function applyApiFieldErrors<TFieldName extends string>(
  error: unknown,
  setError: FieldErrorSetter<TFieldName>,
  allowedFields?: ReadonlySet<string>
): boolean {
  const fieldErrors = extractApiFieldErrors(error);
  if (!fieldErrors) {
    return false;
  }

  let mapped = false;
  for (const [field, message] of Object.entries(fieldErrors)) {
    if (!allowedFields || allowedFields.has(field)) {
      setError(field as TFieldName, { type: 'server', message });
      mapped = true;
    }
  }
  return mapped;
}
