import { useEffect, useState } from 'react';
import api from '../../services/api';
import Button from '../../components/Button';
import Input from '../../components/Input';
import Select from '../../components/Select';
import Modal from '../../components/Modal';
import Card from '../../components/Card';
import Table from '../../components/Table';
import Alert from '../../components/Alert';
import Loader from '../../components/Loader';

const EMPTY_FORM = {
  code: '',
  name: '',
  departmentId: '',
  year: '2',
  semester: '3',
  credits: '4',
  maxMarks: '100',
  passingMarks: '40',
};

// Admin subject list with create modal.
export default function SubjectsPage() {
  const [subjects, setSubjects] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState('');

  async function loadSubjects() {
    setLoading(true);
    setError('');
    try {
      const res = await api.get('/subjects');
      setSubjects(res.data.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load subjects.');
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
    loadSubjects();
    loadDepartments();
  }, []);

  async function onSubmit(e) {
    e.preventDefault();
    setFormError('');
    setSaving(true);
    try {
      const payload = {
        code: form.code,
        name: form.name,
        departmentId: form.departmentId ? Number(form.departmentId) : null,
        year: Number(form.year),
        semester: Number(form.semester),
        credits: Number(form.credits),
        maxMarks: Number(form.maxMarks),
        passingMarks: Number(form.passingMarks),
      };
      await api.post('/subjects', payload);
      setModalOpen(false);
      setForm(EMPTY_FORM);
      await loadSubjects();
    } catch (err) {
      setFormError(err.response?.data?.message || 'Failed to create subject.');
    } finally {
      setSaving(false);
    }
  }

  if (loading) return <Loader />;

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold text-slate-900">Subjects</h2>
        <Button onClick={() => setModalOpen(true)}>Create Subject</Button>
      </div>
      {error && <Alert type="error">{error}</Alert>}
      <Card>
        <Table
          headers={[
            'Code',
            'Name',
            'Dept',
            'Year',
            'Sem',
            'Credits',
            'Max',
            'Passing',
          ]}
        >
          {subjects.map((s) => (
            <tr key={s.id} className="border-b last:border-0">
              <td className="py-2 pr-4">{s.code}</td>
              <td className="py-2 pr-4">{s.name}</td>
              <td className="py-2 pr-4">{s.departmentId}</td>
              <td className="py-2 pr-4">{s.year}</td>
              <td className="py-2 pr-4">{s.semester}</td>
              <td className="py-2 pr-4">{s.credits}</td>
              <td className="py-2 pr-4">{s.maxMarks}</td>
              <td className="py-2 pr-4">{s.passingMarks}</td>
            </tr>
          ))}
        </Table>
        {subjects.length === 0 && (
          <p className="text-sm text-slate-500 mt-2">No subjects found.</p>
        )}
      </Card>

      <Modal
        open={modalOpen}
        title="Create Subject"
        onClose={() => setModalOpen(false)}
      >
        <form onSubmit={onSubmit} className="space-y-3">
          {formError && <Alert type="error">{formError}</Alert>}
          <Input
            label="Code"
            placeholder="CS201"
            value={form.code}
            onChange={(e) => setForm({ ...form, code: e.target.value })}
            required
          />
          <Input
            label="Name"
            placeholder="Data Structures"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            required
          />
          <Select
            label="Department"
            value={form.departmentId}
            onChange={(e) => setForm({ ...form, departmentId: e.target.value })}
            options={[
              { value: '', label: 'None' },
              ...departments.map((d) => ({
                value: d.id,
                label: `${d.name} (${d.code})`,
              })),
            ]}
          />
          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Year"
              type="number"
              value={form.year}
              onChange={(e) => setForm({ ...form, year: e.target.value })}
              required
            />
            <Input
              label="Semester"
              type="number"
              value={form.semester}
              onChange={(e) => setForm({ ...form, semester: e.target.value })}
              required
            />
            <Input
              label="Credits"
              type="number"
              value={form.credits}
              onChange={(e) => setForm({ ...form, credits: e.target.value })}
            />
            <Input
              label="Max Marks"
              type="number"
              value={form.maxMarks}
              onChange={(e) => setForm({ ...form, maxMarks: e.target.value })}
            />
          </div>
          <Input
            label="Passing Marks"
            type="number"
            value={form.passingMarks}
            onChange={(e) => setForm({ ...form, passingMarks: e.target.value })}
          />
          <Button type="submit" loading={saving} className="w-full">
            Create
          </Button>
        </form>
      </Modal>
    </div>
  );
}
