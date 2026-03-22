import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import type { LoginResponse, UserRole } from '@/api/types';
import apiClient from '@/api/client';

interface AuthState {
  token: string | null;
  userId: string | null;
  username: string | null;
  role: UserRole | null;
  projectIds: string[] | null;
}

interface AuthContextValue extends AuthState {
  isAuthenticated: boolean;
  isSuperAdmin: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function loadStoredAuth(): AuthState {
  try {
    const raw = localStorage.getItem('utem_user');
    if (raw) return JSON.parse(raw);
  } catch {
    // ignore
  }
  return { token: null, userId: null, username: null, role: null, projectIds: null };
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [auth, setAuth] = useState<AuthState>(loadStoredAuth);

  const logout = useCallback(() => {
    localStorage.removeItem('utem_token');
    localStorage.removeItem('utem_user');
    setAuth({ token: null, userId: null, username: null, role: null, projectIds: null });
  }, []);

  useEffect(() => {
    if (!auth.token) return;
    // Verify token is still valid on mount
    apiClient.get('/auth/me').catch(() => logout());
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const login = useCallback(async (username: string, password: string) => {
    const { data } = await apiClient.post<LoginResponse>('/auth/login', { username, password });
    const state: AuthState = {
      token: data.token,
      userId: data.userId,
      username: data.username,
      role: data.role,
      projectIds: data.projectIds,
    };
    localStorage.setItem('utem_token', data.token);
    localStorage.setItem('utem_user', JSON.stringify(state));
    setAuth(state);
  }, []);

  const value: AuthContextValue = {
    ...auth,
    isAuthenticated: !!auth.token,
    isSuperAdmin: auth.role === 'SUPER_ADMIN',
    login,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
