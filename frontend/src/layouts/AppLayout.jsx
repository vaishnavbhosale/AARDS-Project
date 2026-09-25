import { useState } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import Sidebar from '../components/Sidebar';
import Navbar from '../components/Navbar';

// Shared shell: sidebar + navbar + page content.
export default function AppLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const location = useLocation();

  const titles = {
    '/dashboard': 'Dashboard',
    '/upload': 'Upload Result PDF',
    '/admin/users': 'Manage Users',
    '/admin/departments': 'Manage Departments',
    '/admin/subjects': 'Manage Subjects',
  };
  const title = titles[location.pathname] || 'AARDS';

  return (
    <div className="min-h-screen bg-slate-50 flex">
      <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />
      <div className="flex-1 min-w-0 flex flex-col">
        <Navbar title={title} onMenuClick={() => setSidebarOpen(true)} />
        <main className="flex-1 p-4 md:p-6 max-w-6xl w-full mx-auto">
          {location.state?.denied && (
            <div className="mb-4 bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg px-4 py-2">
              You do not have access to that page.
            </div>
          )}
          <Outlet />
        </main>
      </div>
    </div>
  );
}
