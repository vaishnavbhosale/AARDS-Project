import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';

// Top bar: page title on the left, user info + logout on the right.
export default function Navbar({ title, onMenuClick }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/login');
  }

  return (
    <header className="bg-white border-b px-4 py-3 flex items-center justify-between">
      <div className="flex items-center gap-3">
        <button
          className="md:hidden text-slate-600 text-xl px-2"
          onClick={onMenuClick}
          aria-label="Open menu"
        >
          ☰
        </button>
        <h1 className="text-lg font-semibold text-slate-900">{title}</h1>
      </div>
      <div className="flex items-center gap-3">
        <div className="text-right">
          <p className="text-sm font-medium text-slate-900">
            {user?.fullName || user?.username}
          </p>
          <span className="inline-block text-xs bg-primary-light text-primary-dark rounded px-2 py-0.5">
            {user?.role}
          </span>
        </div>
        <button
          onClick={handleLogout}
          className="text-sm text-slate-500 hover:text-slate-900 border rounded-lg px-3 py-1.5"
        >
          Logout
        </button>
      </div>
    </header>
  );
}
