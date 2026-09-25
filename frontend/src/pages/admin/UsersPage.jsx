import { useEffect, useState } from 'react';
import api from '../../services/api';
import authService from '../../services/authService';
import Button from '../../components/Button';
import Input from '../../components/Input';
import Select from '../../components/Select';
import Modal from '../../components/Modal';
import Card from '../../components/Card';
import Table from '../../components/Table';
import Alert from '../../components/Alert';
import Loader from '../../components/Loader';

const EMPTY_FORM = {
  username: '',
  password: '',
  fullName: '',
  email: '',
  role: 'FACULTY',
  departmentId: '',
};

// Admin user list with create + activate/deactivate.
export default function UsersPage() {
  const [users, setUsers] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState('');

  async function loadUsers() {
    setLoading(true);
    setError('');
    try {
      const res = await api.get('/admin/users');
      setUsers(res.data.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load users.');
    } finally {
      setLoading(false);
    }
  }

  async function loadDepartments() {
    try {
      const res = await api.get('/departments');
      setDepartments(res.data.data);
    } catch {
      setDepartments([]);
    }
  }

  useEffect(() => {
    loadUsers();
    loadDepartments();
  }, []);

  function openModal() {
    setForm(EMPTY_FORM);
    setFormError('');
    setModalOpen(true);
  }

  async function onSubmit(e) {
    e.preventDefault();
    setFormError('');
    setSaving(true);
    try {
      const payload = {
        ...form,
        departmentId: form.departmentId ? Number(form.departmentId) : null,
      };
      await authService.registerUser(payload);
      setModalOpen(false);
      await loadUsers();
    } catch (err) {
      setFormError(err.response?.data?.message || 'Failed to create user.');
    } finally {
      setSaving(false);
    }
  }

  async function toggleActive(user) {
    setError('');
    try {
      const action = user.active ? 'deactivate' : 'activate';
      await api.put(`/admin/users/${user.id}/${action}`);
      await loadUsers();
    } catch (err) {
      setError(err.response?.data?.message || 'Action failed.');
    }
  }

  if (loading) return <Loader />;

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold text-slate-900">Users</h2>
        <Button onClick={openModal}>Create User</Button>
      </div>
      {error && <Alert type="error">{error}</Alert>}
      <Card>
        <Table
          headers={['ID', 'Username', 'Full Name', 'Email', 'Role', 'Active', 'Actions']}
        >
          {users.map((u) => (
            <tr key={u.id} className="border-b last:border-0">
              <td className="py-2 pr-4">{u.id}</td>
              <td className="py-2 pr-4">{u.username}</td>
              <td className="py-2 pr-4">{u.fullName}</td>
              <td className="py-2 pr-4">{u.email}</td>
              <td className="py-2 pr-4">{u.role}</td>
              <td className="py-2 pr-4">{u.active ? 'Yes' : 'No'}</td>
              <td className="py-2 pr-4">
                <button
                  onClick={() => toggleActive(u)}
                  className="text-xs text-primary-dark underline"
                >
                  {u.active ? 'Deactivate' : 'Activate'}
                </button>
              </td>
            </tr>
          ))}
        </Table>
        {users.length === 0 && (
          <p className="text-sm text-slate-500 mt-2">No users found.</p>
        )}
      </Card>

      <Modal open={modalOpen} title="Create User" onClose={() => setModalOpen(false)}>
        <form onSubmit={onSubmit} className="space-y-3">
          {formError && <Alert type="error">{formError}</Alert>}
          <Input
            label="Username"
            value={form.username}
            onChange={(e) => setForm({ ...form, username: e.target.value })}
            required
          />
          <Input
            label="Password"
            type="password"
            value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
            required
          />
          <Input
            label="Full Name"
            value={form.fullName}
            onChange={(e) => setForm({ ...form, fullName: e.target.value })}
            required
          />
          <Input
            label="Email"
            type="email"
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
          />
          <Select
            label="Role"
            value={form.role}
            onChange={(e) => setForm({ ...form, role: e.target.value })}
            options={['FACULTY', 'HOD', 'PRINCIPAL', 'ADMIN'].map((r) => ({
              value: r,
              label: r,
            }))}
          />
          <Select
            label="Department"
            value={form.departmentId}
            onChange={(e) => setForm({ ...form, departmentId: e.target.value })}
            options={[
              { value: '', label: 'None' },
              ...departments.map((d) => ({ value: d.id, label: `${d.name} (${d.code})` })),
            ]}
          />
          <Button type="submit" loading={saving} className="w-full">
            Create
          </Button>
        </form>
      </Modal>
    </div>
  );
}
