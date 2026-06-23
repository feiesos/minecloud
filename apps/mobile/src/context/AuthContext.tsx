import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from 'react';
import { tokenStore } from '../api/tokenStore';
import { login as apiLogin, logout as apiLogout } from '../api/auth';

interface AuthState {
  isAuthenticated: boolean;
  userId: string | null;
  username: string | null;
  isLoading: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [userId, setUserId] = useState<string | null>(null);
  const [username, setUsername] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    setIsLoading(false);
  }, []);

  const login = useCallback(async (uname: string, pwd: string) => {
    const data = await apiLogin(uname, pwd);
    setUserId(data.userId);
    setUsername(data.username);
  }, []);

  const logout = useCallback(() => {
    apiLogout();
    setUserId(null);
    setUsername(null);
  }, []);

  return (
    <AuthContext.Provider
      value={{
        isAuthenticated: !!userId,
        userId,
        username,
        isLoading,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
