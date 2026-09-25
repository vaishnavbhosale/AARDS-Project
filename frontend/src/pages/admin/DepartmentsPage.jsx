import { useEffect, useState } from 'react';
import api from '../../services/api';
import Button from '../../components/Button';
import Input from '../../components/Input';
import Modal from '../../components/Modal';
import Card from '../../components/Card';
import Table from '../../components/Table';
import Alert from '../../components/Alert';
import Loader from '../../components/Loader';

// Admin department list with create modal.
export default function DepartmentsPage() {
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [form, setForm] = useState({ name: '', code: '' });
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState('');

  async function loadDepartments() {
    setLoading(true);
    setError('');
    try {
      const res = await api.get('/departments');
      setDepartments(res.data.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load departments.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadDepartments();
  }, []);

  async function onSubmit(e) {
    e.preventDefault();
    setFormError('');
    setSaving(true);
    try {
      await api.post('/departments', form);
      setModalOpen(false);
      setForm({ name: '', code: '' });
      await loadDepartments();
    } catch (err) {
      setFormError(err.response?.data?.message || 'Failed to create department.');
    } finally {
      setSaving(false);
    }
  }

  if (loading) return <Loader />;

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold text-slate-900">Departments</h2>
        <Button onClick={() => setModalOpen(true)}>Create Department</Button>
      </div>
      {error && <Alert type="error">{error}</Alert>}
      <Card>
        <Table headers={['ID', 'Name', 'Code']}>
          {departments.map((d) => (
            <tr key={d.id} className="border-b last:border-0">
              <td className="py-2 pr-4">{d.id}</td>
              <td className="py-2 pr-4">{d.name}</td>
              <td className="py-2 pr-4">{d.code}</td>
            </tr>
          ))}
        </Table>
        {departments.length === 0 && (
          <p className="text-sm text-slate-500 mt-2">No departments found.</p>
        )}
      </Card>

      <Modal
        open={modalOpen}
        title="Create Department"
        onClose={() => setModalOpen(false)}
      >
        <form onSubmit={onSubmit} className="space-y-3">
          {formError && <Alert type="error">{formError}</Alert>}
          <Input
            label="Name"
            placeholder="Computer Engineering"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            required
          />
          <Input
            label="Code"
            placeholder="COMP"
            value={form.code}
            onChange={(e) => setForm({ ...form, code: e.target.value })}
            required
          />
          <Button type="submit" loading={saving} className="w-full">
            Create
          </Button>
        </form>
      </Modal>
    </div>
  );
}
