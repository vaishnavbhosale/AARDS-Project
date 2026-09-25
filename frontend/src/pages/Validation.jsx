import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import validationService from '../services/validationService';
import Button from '../components/Button';
import Card from '../components/Card';
import Table from '../components/Table';
import Alert from '../components/Alert';
import Loader from '../components/Loader';
import Modal from '../components/Modal';

// Teacher reviews doubtful values, fixes them, then approves the batch.
export default function Validation() {
  const { batchId } = useParams();
  const navigate = useNavigate();
  const [errors, setErrors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [pageError, setPageError] = useState('');
  const [edited, setEdited] = useState({});
  const [rowError, setRowError] = useState({});
  const [savingId, setSavingId] = useState(null);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [approving, setApproving] = useState(false);
  const [notice, setNotice] = useState(null);

  async function loadErrors() {
    setLoading(true);
    setPageError('');
    try {
      const data = await validationService.getBatchErrors(batchId);
      setErrors(data);
      const initial = {};
      data.forEach((e) => {
        initial[e.id] = e.correctedValue || '';
      });
      setEdited(initial);
    } catch (err) {
      setPageError(err.response?.data?.message || 'Failed to load errors.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadErrors();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [batchId]);

  // No errors: show green message, then go to dashboard.
  useEffect(() => {
    if (!loading && !pageError && errors.length === 0) {
      const timer = setTimeout(() => navigate('/dashboard'), 2000);
      return () => clearTimeout(timer);
    }
  }, [loading, pageError, errors, navigate]);

  async function onSave(errorId) {
    const value = (edited[errorId] || '').trim();
    if (!value) {
      setRowError({ ...rowError, [errorId]: 'Corrected value is required.' });
      return;
    }
    setRowError({ ...rowError, [errorId]: '' });
    setSavingId(errorId);
    try {
      const updated = await validationService.updateError(errorId, value);
      setErrors(errors.map((e) => (e.id === errorId ? updated : e)));
      setNotice({ type: 'success', message: 'Saved. Row approved.' });
    } catch (err) {
      setNotice({
        type: 'error',
        message: err.response?.data?.message || 'Save failed.',
      });
    } finally {
      setSavingId(null);
    }
  }

  async function onApprove() {
    setConfirmOpen(false);
    setApproving(true);
    setNotice(null);
    try {
      await validationService.approveBatch(batchId);
      setNotice({ type: 'success', message: 'Batch approved. Opening dashboard...' });
      setTimeout(() => navigate('/dashboard'), 1500);
    } catch (err) {
      setNotice({
        type: 'error',
        message: err.response?.data?.message || 'Approve failed.',
      });
    } finally {
      setApproving(false);
    }
  }

  if (loading) return <Loader />;

  return (
    <div className="space-y-4">
      <p className="text-xs text-slate-500">Dashboard / Upload / Validation</p>
      <div>
        <h2 className="text-xl font-semibold text-slate-900">Validation Errors</h2>
        <p className="text-sm text-slate-500">
          Review and correct extracted data before analytics are generated.
        </p>
      </div>

      {pageError && <Alert type="error">{pageError}</Alert>}
      {notice && <Alert type={notice.type}>{notice.message}</Alert>}

      {!pageError && errors.length === 0 && (
        <Alert type="success">No errors found. Redirecting...</Alert>
      )}

      {errors.length > 0 && (
        <Card>
          <Table
            headers={[
              'Student PRN',
              'Student Name',
              'Field',
              'Extracted Value',
              'Corrected Value',
              'Status',
              'Action',
            ]}
          >
            {errors.map((e) => (
              <tr
                key={e.id}
                className={`border-b last:border-0 ${
                  e.status === 'APPROVED' ? 'bg-green-50' : 'bg-orange-50'
                }`}
              >
                <td className="py-2 pr-4">{e.studentPrn}</td>
                <td className="py-2 pr-4">{e.studentName}</td>
                <td className="py-2 pr-4">{e.fieldName}</td>
                <td className="py-2 pr-4">{e.extractedValue}</td>
                <td className="py-2 pr-4">
                  {e.status === 'APPROVED' ? (
                    <span>{e.correctedValue}</span>
                  ) : (
                    <div>
                      <input
                        value={edited[e.id] || ''}
                        onChange={(ev) =>
                          setEdited({ ...edited, [e.id]: ev.target.value })
                        }
                        className={`border rounded-lg px-2 py-1 text-sm w-40 bg-white ${
                          rowError[e.id] ? 'border-error' : 'border-slate-300'
                        }`}
                      />
                      {rowError[e.id] && (
                        <p className="text-xs text-error mt-1">{rowError[e.id]}</p>
                      )}
                    </div>
                  )}
                </td>
                <td className="py-2 pr-4">
                  <span
                    className={`inline-block text-xs font-medium rounded px-2 py-0.5 ${
                      e.status === 'APPROVED'
                        ? 'bg-green-100 text-green-700'
                        : 'bg-orange-100 text-orange-700'
                    }`}
                  >
                    {e.status}
                  </span>
                </td>
                <td className="py-2 pr-4">
                  {e.status !== 'APPROVED' && (
                    <button
                      onClick={() => onSave(e.id)}
                      disabled={savingId === e.id}
                      className="text-xs text-primary-dark underline disabled:opacity-50"
                    >
                      {savingId === e.id ? 'Saving...' : 'Save'}
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </Table>

          <div className="mt-4 flex flex-wrap gap-3">
            <Button onClick={() => setConfirmOpen(true)} loading={approving}>
              Approve All & Finalize
            </Button>
            <Button variant="secondary" onClick={() => navigate('/upload')}>
              Cancel
            </Button>
          </div>
        </Card>
      )}

      <Modal
        open={confirmOpen}
        title="Approve batch?"
        onClose={() => setConfirmOpen(false)}
      >
        <p className="text-sm text-slate-600">
          This will save all corrected data and generate analytics. Continue?
        </p>
        <div className="mt-4 flex gap-3 justify-end">
          <Button variant="secondary" onClick={() => setConfirmOpen(false)}>
            Back
          </Button>
          <Button onClick={onApprove}>Confirm</Button>
        </div>
      </Modal>
    </div>
  );
}
