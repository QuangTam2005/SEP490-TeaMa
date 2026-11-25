'use client';

import { useEffect } from 'react';
import { getAccessToken } from '@/lib/auth-token';
import { authService } from '@/services';
import { useAuthStore } from '@/store/auth-store';

/**
 * Hydrates Zustand auth session from GET /api/auth/me when a stored JWT exists after page reload.
 */
export function AuthHydrator() {
  const user = useAuthStore((state) => state.user);
  const syncUser = useAuthStore((state) => state.syncUser);
  const clearSession = useAuthStore((state) => state.clearSession);

  useEffect(() => {
    if (user || !getAccessToken()) {
      return;
    }

    let cancelled = false;
    void authService
      .getCurrentUser()
      .then((me) => {
        if (!cancelled) {
          syncUser(me);
        }
      })
      .catch(() => {
        if (!cancelled) {
          clearSession();
        }
      });

    return () => {
      cancelled = true;
    };
  }, [user, syncUser, clearSession]);

  return null;
}
