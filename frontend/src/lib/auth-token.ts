const ACCESS_TOKEN_KEY = 'sep490_access_token';
const SUPERVISOR_PATH_REGEX = /^\/supervisor\/([^/?#]+)/;

export function getAccessToken(): string | null {
  if (typeof window === 'undefined') {
    return null;
  }
  try {
    return window.localStorage.getItem(ACCESS_TOKEN_KEY);
  } catch {
    return null;
  }
}

export function setAccessToken(token: string): void {
  if (typeof window === 'undefined') {
    return;
  }
  try {
    window.localStorage.setItem(ACCESS_TOKEN_KEY, token);
  } catch {
    // Ignore storage quota or privacy-mode errors safely
  }
}

export function clearAccessToken(): void {
  if (typeof window === 'undefined') {
    return;
  }
  try {
    window.localStorage.removeItem(ACCESS_TOKEN_KEY);
  } catch {
    // Ignore storage access errors safely
  }
}

export function resolveSupervisorTokenFromPath(pathname?: string): string | null {
  const targetPath =
    pathname ?? (typeof window !== 'undefined' ? window.location.pathname : '');
  const match = SUPERVISOR_PATH_REGEX.exec(targetPath);
  return match && match[1] ? decodeURIComponent(match[1]) : null;
}
