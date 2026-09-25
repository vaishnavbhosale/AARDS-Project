import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import ProtectedRoute from './components/ProtectedRoute';
import AppLayout from './layouts/AppLayout';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Upload from './pages/Upload';
import Validation from './pages/Validation';
import UsersPage from './pages/admin/UsersPage';
import DepartmentsPage from './pages/admin/DepartmentsPage';
import SubjectsPage from './pages/admin/SubjectsPage';
import NotFound from './pages/NotFound';

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
            <Route path="/upload" element={<Upload />} />
            <Route path="/validation/:batchId" element={<Validation />} />
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
