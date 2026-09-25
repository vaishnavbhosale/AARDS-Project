import { Link, Outlet, useNavigate } from 'react-router-dom';

export default function MainLayout() {
  const navigate = useNavigate();
  const logout = () => {
    localStorage.removeItem('aards_token');
    navigate('/login');
  };
  return (
    <div className="min-h-screen bg-slate-50">
      <header className="bg-white border-b px-6 py-3 flex items-center justify-between">
        <Link to="/dashboard" className="font-semibold">AARDS</Link>
        <button onClick={logout} className="text-sm text-slate-500 hover:text-slate-900">Logout</button>
      </header>
      <main className="max-w-6xl mx-auto">
        <Outlet />
      </main>
    </div>
  );
}
