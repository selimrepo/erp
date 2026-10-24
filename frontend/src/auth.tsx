import { createContext, useContext, useMemo, useState, type ReactNode } from "react";
import type { Credentials } from "./api";

interface AuthContextValue {
  credentials: Credentials | null;
  login: (credentials: Credentials) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);
const STORAGE_KEY = "erplancers.erp.credentials";

export function AuthProvider({ children }: { children: ReactNode }) {
  const [credentials, setCredentials] = useState<Credentials | null>(() => {
    try {
      const raw = sessionStorage.getItem(STORAGE_KEY);
      return raw ? JSON.parse(raw) as Credentials : null;
    } catch {
      return null;
    }
  });

  const value = useMemo(() => ({
    credentials,
    login: (next: Credentials) => {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(next));
      setCredentials(next);
    },
    logout: () => {
      sessionStorage.removeItem(STORAGE_KEY);
      setCredentials(null);
    }
  }), [credentials]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside AuthProvider");
  return context;
}
