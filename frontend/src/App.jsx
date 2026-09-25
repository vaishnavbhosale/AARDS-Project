import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import ProtectedRoute from './components/ProtectedRoute';
import AppLayout from './layouts/AppLayout';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import UsersPage from './pages/admin/UsersPage';
import DepartmentsPage from './pages/admin/DepartmentsPage';
import SubjectsPage from './pages/admin/SubjectsPage';
import NotFound from './pages/NotFound';

// Upload + Validation pages come in Steps 5 and 6. Placeholder shown till then.
function ComingSoon({ text }) {
  return (
    <div className="bg-white rounded-xl shadow p-8 text-center">
      <p className="text-lg font-medium text-slate-900">{text}</p>
      <p className="text-sm text-slate-500 mt-1">This page is coming soon.</p>
    </div>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/" element={<Navigate to="/dashboard" replace />} />

        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            <Route path="/dashboard" element={<Dashboard />} />
          </Route>
        </Route>

        <Route element={<ProtectedRoute roles={['FACULTY', 'ADMIN']} />}>
          <Route element={<AppLayout />}>
            <Route path="/upload" element={<ComingSoon text="Upload Result PDF" />} />
            <Route
              path="/validation/:batchId"
              element={<ComingSoon text="Validation Review" />}
            />
          </Route>
        </Route>

        <Route element={<ProtectedRoute roles={['ADMIN']} />}>
          <Route element={<AppLayout />}>
            <Route path="/admin/users" element={<UsersPage />} />
            <Route path="/admin/departments" element={<DepartmentsPage />} />
            <Route path="/admin/subjects" element={<SubjectsPage />} />
          </Route>
        </Route>

        <Route path="*" element={<NotFound />} />
      </Routes>
    </BrowserRouter>
  );
}
