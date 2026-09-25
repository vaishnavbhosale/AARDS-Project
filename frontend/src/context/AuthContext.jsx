import { createContext, useContext, useState } from 'react';
import authService from '../services/authService';

const AuthContext = createContext(null);

// Holds logged-in user + token. Any page can use useAuth() to read them.
export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('aards_token'));
  const [user, setUser] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem('aards_user'));
    } catch {
      return null;
    }
  });

  async function login(username, password) {
    const data = await authService.login(username, password);
    const loggedInUser = {
      username: data.username,
      fullName: data.fullName,
      role: data.role,
      departmentId: data.departmentId,
    };
    localStorage.setItem('aards_token', data.token);
    localStorage.setItem('aards_user', JSON.stringify(loggedInUser));
    setToken(data.token);
    setUser(loggedInUser);
    return loggedInUser;
  }

  function logout() {
    authService.logout();
    setToken(null);
    setUser(null);
  }

  function isAuthenticated() {
    return !!token;
  }

  function hasRole(role) {
    return user?.role === role;
  }

  function hasAnyRole(roles) {
    return roles.includes(user?.role);
  }

  return (
    <AuthContext.Provider
      value={{ user, token, login, logout, isAuthenticated, hasRole, hasAnyRole }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used inside AuthProvider');
  }
  return ctx;
}
