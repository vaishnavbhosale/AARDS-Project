import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

// Blocks pages for logged-out users. Optional roles prop blocks wrong roles too.
export default function ProtectedRoute({ roles }) {
  const { isAuthenticated, hasAnyRole } = useAuth();

  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }

  if (roles && !hasAnyRole(roles)) {
    return <Navigate to="/dashboard" replace state={{ denied: true }} />;
  }

  return <Outlet />;
}
