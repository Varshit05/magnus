import { useContext } from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { AuthContext } from '@/context/AuthContext';

interface ProtectedRouteProps {
  requiredRoles?: string[]; // e.g. ['ROLE_ADMIN']
}

export const ProtectedRoute = ({ requiredRoles }: ProtectedRouteProps) => {
  const auth = useContext(AuthContext);
  if (!auth?.token) {
    // Not logged in – redirect to login page
    return <Navigate to="/login" replace />;
  }
  if (requiredRoles && requiredRoles.length > 0) {
    const hasRole = requiredRoles.some((role) => auth.roles.includes(role));
    if (!hasRole) {
      // Logged in but missing role – show 403 page or redirect
      return <Navigate to="/403" replace />;
    }
  }
  // Authorized – render the child route(s)
  return <Outlet />;
};
