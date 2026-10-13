import { createContext, useState, useEffect } from 'react';
import type { ReactNode } from 'react';
import { jwtDecode } from 'jwt-decode';

interface AuthContextProps {
  token: string | null;
  roles: string[];
  login: (token: string) => void;
  logout: () => void;
  getToken: () => string | null;
}

export const AuthContext = createContext<AuthContextProps | undefined>(undefined);

/**
 * Provides authentication state (JWT token and decoded roles) to the app.
 * Also exports a convenient `getToken` helper for non‑React modules (e.g., Axios interceptor).
 */
export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [token, setToken] = useState<string | null>(null);
  const [roles, setRoles] = useState<string[]>([]);

  // Load token from storage on mount
  useEffect(() => {
    const stored = localStorage.getItem('token');
    if (stored) {
      setToken(stored);
      const decoded: any = jwtDecode(stored);
      setRoles(decoded.roles ? decoded.roles.split(',') : []);
    }
  }, []);

  const login = (newToken: string) => {
    localStorage.setItem('token', newToken);
    setToken(newToken);
    const decoded: any = jwtDecode(newToken);
    setRoles(decoded.roles ? decoded.roles.split(',') : []);
  };

  const logout = () => {
    localStorage.removeItem('token');
    setToken(null);
    setRoles([]);
  };

  // Helper used by the Axios interceptor
  const getToken = () => token;

  return (
    <AuthContext.Provider value={{ token, roles, login, logout, getToken }}>
      {children}
    </AuthContext.Provider>
  );
};

// Export the helper for easy import outside React components
export const getToken = () => {
  return localStorage.getItem('token');
};

