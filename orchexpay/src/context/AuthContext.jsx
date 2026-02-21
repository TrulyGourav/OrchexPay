import React, { createContext, useContext, useState, useCallback, useEffect } from 'react';
import { authApi } from '../api/auth';
import { setWalletAuth, setPayoutAuth } from '../api/config';
import { decodeJwtPayload } from '../utils/jwt';

const AuthContext = createContext(null);

const TOKEN_KEY = 'ledgerx_token';
const USER_KEY = 'ledgerx_user';

function userFromToken(accessToken) {
  const payload = decodeJwtPayload(accessToken);
  if (!payload) return null;
  const roles = payload.roles || [];
  return {
    username: payload.sub,
    roles: Array.isArray(roles) ? roles : [],
    merchantId: payload.merchantId || null,
  };
}

export function AuthProvider({ children }) {
  const [token, setTokenState] = useState(() => sessionStorage.getItem(TOKEN_KEY));
  const [user, setUser] = useState(() => {
    try {
      const s = sessionStorage.getItem(USER_KEY);
      return s ? JSON.parse(s) : null;
    } catch {
      return null;
    }
  });
  const [loading, setLoading] = useState(true);

  const setToken = useCallback((newToken) => {
    if (newToken) {
      sessionStorage.setItem(TOKEN_KEY, newToken);
      setTokenState(newToken);
      const u = userFromToken(newToken);
      setUser(u);
      if (u) sessionStorage.setItem(USER_KEY, JSON.stringify(u));
    } else {
      sessionStorage.removeItem(TOKEN_KEY);
      sessionStorage.removeItem(USER_KEY);
      setTokenState(null);
      setUser(null);
    }
  }, []);

  const login = useCallback(async (username, password) => {
    const res = await authApi.login(username, password);
    const t = res.data.accessToken;
    setToken(t);
    setWalletAuth(t);
    setPayoutAuth(t);
    return userFromToken(t);
  }, [setToken]);

  const logout = useCallback(() => {
    setToken(null);
  }, [setToken]);

  const refreshUser = useCallback(() => {
    if (token) {
      const u = userFromToken(token);
      if (u) {
        setUser(u);
        sessionStorage.setItem(USER_KEY, JSON.stringify(u));
      } else {
        setToken(null);
      }
    }
  }, [token, setToken]);

  useEffect(() => {
    setWalletAuth(token);
    setPayoutAuth(token);
  }, [token]);

  useEffect(() => {
    if (token && !user) {
      const u = userFromToken(token);
      setUser(u);
      if (u) sessionStorage.setItem(USER_KEY, JSON.stringify(u));
    }
    setLoading(false);
  }, [token]);

  const value = {
    token,
    user,
    loading,
    login,
    logout,
    setToken,
    refreshUser,
    isAdmin: user?.roles?.includes('ADMIN'),
    isMerchant: user?.roles?.includes('MERCHANT'),
    isVendor: user?.roles?.includes('VENDOR'),
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
