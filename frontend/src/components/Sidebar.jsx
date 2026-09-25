import { NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

// Left menu. Links change based on the logged-in user's role.
export default function Sidebar({ open, onClose }) {
  const { hasAnyRole } = useAuth();

  const linkClass = ({ isActive }) =>
    `block px-4 py-2 rounded-lg text-sm font-medium ${
      isActive
        ? 'bg-primary-light text-primary-dark'
        : 'text-slate-600 hover:bg-slate-100'
    }`;

  return (
    <>
      {open && (
        <div
          className="fixed inset-0 bg-slate-900 bg-opacity-40 z-20 md:hidden"
          onClick={onClose}
        />
      )}
      <aside
        className={`fixed md:static z-30 w-60 shrink-0 h-screen bg-white border-r flex flex-col transition-transform ${
          open ? 'translate-x-0' : '-translate-x-full md:translate-x-0'
        }`}
      >
        <div className="px-5 py-4 border-b">
          <p className="text-lg font-bold text-slate-900">AARDS</p>
          <p className="text-xs text-slate-500">Result Analytics</p>
        </div>
        <nav className="flex-1 p-3 space-y-1 overflow-y-auto">
          <NavLink to="/dashboard" className={linkClass} onClick={onClose}>
            Dashboard
          </NavLink>
          {hasAnyRole(['FACULTY', 'HOD', 'ADMIN']) && (
            <NavLink to="/upload" className={linkClass} onClick={onClose}>
              Upload
            </NavLink>
          )}
          {hasAnyRole(['ADMIN']) && (
            <>
              <p className="px-4 pt-3 pb-1 text-xs font-semibold text-slate-400 uppercase">
                Admin
              </p>
              <NavLink to="/admin/users" className={linkClass} onClick={onClose}>
                Users
              </NavLink>
              <NavLink to="/admin/departments" className={linkClass} onClick={onClose}>
                Departments
              </NavLink>
              <NavLink to="/admin/subjects" className={linkClass} onClick={onClose}>
                Subjects
              </NavLink>
            </>
          )}
        </nav>
      </aside>
    </>
  );
}
