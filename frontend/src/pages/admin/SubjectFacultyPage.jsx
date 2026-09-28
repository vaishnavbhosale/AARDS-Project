import { useEffect, useState } from 'react';
import api from '../../services/api';
import Button from '../../components/Button';
import Select from '../../components/Select';
import Modal from '../../components/Modal';
import Card from '../../components/Card';
import Table from '../../components/Table';
import Alert from '../../components/Alert';
import Loader from '../../components/Loader';

// Admin assigns one faculty per subject per session. Shown on the SE Combine report.
export default function SubjectFacultyPage() {
  const [assignments, setAssignments] = useState([]);
  const [subjects, setSubjects] = useState([]);
  const [faculties, setFaculties] = useState([]);
  const [sessions, setSessions] = useState([]);
  const [sessionId, setSessionId] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [form, setForm] = useState({ subjectId: '', facultyId: '' });
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState('');

  async function loadLookups() {
    try {
      const [subRes, userRes, filterRes] = await Promise.all([
        api.get('/subjects'),
        api.get('/admin/users'),
        api.get('/dashboard/filters'),
      ]);
      setSubjects(subRes.data.data || []);
      setFaculties(
        (userRes.data.data || []).filter((u) => u.role === 'FACULTY')
      );
      const sessionList = filterRes.data.data?.sessions || [];
      setSessions(sessionList);
      if (sessionList.length > 0 && !sessionId) {
        setSessionId(String(sessionList[0].id));
      }
    } catch {
      setSubjects([]);
      setFaculties([]);
      setSessions([]);
    }
  }

  async function loadAssignments(sid) {
    if (!sid) return;
    setLoading(true);
    setError('');
    try {
      const res = await api.get('/admin/subject-faculty', {
        params: { academicSessionId: sid },
      });
      setAssignments(res.data.data || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load assignments.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadLookups();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    loadAssignments(sessionId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessionId]);

  async function onSubmit(e) {
    e.preventDefault();
    setFormError('');
    setSaving(true);
    try {
      await api.post('/admin/subject-faculty', {
        subjectId: Number(form.subjectId),
        facultyId: Number(form.facultyId),
        academicSessionId: Number(sessionId),
      });
      setModalOpen(false);
      setForm({ subjectId: '', facultyId: '' });
      await loadAssignments(sessionId);
    } catch (err) {
      setFormError(err.response?.data?.message || 'Failed to assign faculty.');
    } finally {
      setSaving(false);
    }
  }

  async function onDelete(id) {
    try {
      await api.delete(`/admin/subject-faculty/${id}`);
      await loadAssignments(sessionId);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete assignment.');
    }
  }

  if (loading) return <Loader />;

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold text-slate-900">Subject-Faculty</h2>
        <Button onClick={() => setModalOpen(true)}>Assign Faculty</Button>
      </div>
      {error && <Alert type="error">{error}</Alert>}
      <Card>
        <div className="max-w-xs mb-3">
          <Select
            label="Academic Session"
            value={sessionId}
            onChange={(e) => setSessionId(e.target.value)}
            options={sessions.map((s) => ({
              value: String(s.id),
              label: s.name,
            }))}
          />
        </div>
        <Table headers={['Subject Code', 'Subject Name', 'Faculty', 'Session', 'Action']}>
          {assignments.map((a) => (
            <tr key={a.id} className="border-b last:border-0">
              <td className="py-2 pr-4">{a.subjectCode}</td>
              <td className="py-2 pr-4">{a.subjectName}</td>
              <td className="py-2 pr-4">{a.facultyFullName}</td>
              <td className="py-2 pr-4">{a.sessionName}</td>
              <td className="py-2 pr-4">
                <button
                  onClick={() => onDelete(a.id)}
                  className="text-xs text-error underline"
                >
                  Delete
                </button>
              </td>
            </tr>
          ))}
        </Table>
        {assignments.length === 0 && (
          <p className="text-sm text-slate-500 mt-2">No assignments found.</p>
        )}
      </Card>

      <Modal
        open={modalOpen}
        title="Assign Faculty"
        onClose={() => setModalOpen(false)}
      >
        <form onSubmit={onSubmit} className="space-y-3">
          {formError && <Alert type="error">{formError}</Alert>}
          <Select
            label="Subject"
            value={form.subjectId}
            onChange={(e) => setForm({ ...form, subjectId: e.target.value })}
            options={[
              { value: '', label: 'Select subject' },
              ...subjects.map((s) => ({
                value: s.id,
                label: `${s.code} - ${s.name}`,
              })),
            ]}
          />
          <Select
            label="Faculty"
            value={form.facultyId}
            onChange={(e) => setForm({ ...form, facultyId: e.target.value })}
            options={[
              { value: '', label: 'Select faculty' },
              ...faculties.map((f) => ({
                value: f.id,
                label: f.fullName || f.username,
              })),
            ]}
          />
          <Button type="submit" loading={saving} className="w-full">
            Assign
          </Button>
        </form>
      </Modal>
    </div>
  );
}
