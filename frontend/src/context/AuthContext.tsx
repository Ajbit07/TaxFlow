import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { authApi } from "@/api/endpoints";
import { tokenStore } from "@/api/client";
import type { AuthResponse, UserProfile } from "@/types/api";

interface AuthUser {
  id: string;
  email: string;
  fullName: string;
  role: string;
  emailVerified: boolean;
}

interface AuthContextValue {
  user: AuthUser | null;
  loading: boolean;
  applyAuth: (auth: AuthResponse) => void;
  logout: () => Promise<void>;
  refreshProfile: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
  return ctx;
}

const USER_KEY = "taxflow.user";

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? (JSON.parse(raw) as AuthUser) : null;
  });
  const [loading, setLoading] = useState(true);

  const clear = useCallback(() => {
    tokenStore.clear();
    localStorage.removeItem(USER_KEY);
    localStorage.removeItem("taxflow.businessId");
    setUser(null);
  }, []);

  useEffect(() => {
    const onLogout = () => clear();
    window.addEventListener("taxflow:logout", onLogout);
    return () => window.removeEventListener("taxflow:logout", onLogout);
  }, [clear]);

  useEffect(() => {
    (async () => {
      if (tokenStore.access) {
        try {
          const profile: UserProfile = await authApi.me();
          const next: AuthUser = {
            id: profile.id,
            email: profile.email,
            fullName: profile.fullName,
            role: profile.role,
            emailVerified: profile.emailVerified,
          };
          setUser(next);
          localStorage.setItem(USER_KEY, JSON.stringify(next));
        } catch {
          clear();
        }
      }
      setLoading(false);
    })();
  }, [clear]);

  const applyAuth = useCallback((auth: AuthResponse) => {
    tokenStore.set(auth);
    const next: AuthUser = {
      id: auth.userId,
      email: auth.email,
      fullName: auth.fullName,
      role: auth.role,
      emailVerified: auth.emailVerified,
    };
    setUser(next);
    localStorage.setItem(USER_KEY, JSON.stringify(next));
  }, []);

  const logout = useCallback(async () => {
    const refresh = tokenStore.refresh;
    if (refresh) {
      try {
        await authApi.logout(refresh);
      } catch {
        // Session is cleared locally regardless of API result.
      }
    }
    clear();
  }, [clear]);

  const refreshProfile = useCallback(async () => {
    const profile = await authApi.me();
    const next: AuthUser = {
      id: profile.id,
      email: profile.email,
      fullName: profile.fullName,
      role: profile.role,
      emailVerified: profile.emailVerified,
    };
    setUser(next);
    localStorage.setItem(USER_KEY, JSON.stringify(next));
  }, []);

  const value = useMemo(
    () => ({ user, loading, applyAuth, logout, refreshProfile }),
    [user, loading, applyAuth, logout, refreshProfile],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
