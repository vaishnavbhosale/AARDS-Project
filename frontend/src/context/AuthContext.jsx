import { createContext, useContext, useState } from 'react';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('aards_token'));
  const login = (t) => {
    localStorage.setItem('aards_token', t);
    setToken(t);
  };
  const logout = () => {
    localStorage.removeItem('aards_token');
    setToken(null);
  };
  return <AuthContext.Provider value={{ token, login, logout }}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
