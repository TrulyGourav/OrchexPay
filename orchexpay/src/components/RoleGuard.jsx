import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export function RoleGuard({ children, allowedRoles }) {
  const { user, token } = useAuth();
  if (!token) return <Navigate to="/login" replace />;
  if (!user) return null;
  const hasRole = allowedRoles.some((r) => user.roles && user.roles.includes(r));
  if (!hasRole) return <Navigate to="/unauthorized" replace />;
  return children;
}
