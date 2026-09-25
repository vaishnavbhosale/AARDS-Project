import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import uploadService from '../services/uploadService';
import Button from '../components/Button';
import Card from '../components/Card';
import Table from '../components/Table';
import Alert from '../components/Alert';
import Loader from '../components/Loader';
import SkeletonCard from '../components/SkeletonCard';

const MAX_SIZE = 20 * 1024 * 1024;

// Colored pill for batch status.
function StatusBadge({ status }) {
  const colors = {
    UPLOADED: 'bg-slate-100 text-slate-600',
    PARSING: 'bg-blue-100 text-blue-700',
    PARSED: 'bg-orange-100 text-orange-700',
    VALIDATED: 'bg-green-100 text-green-700',
    FAILED: 'bg-red-100 text-red-700',
  };
  return (
    <span
      className={`inline-block text-xs font-medium rounded px-2 py-0.5 ${
        colors[status] || 'bg-slate-100 text-slate-600'
      }`}
    >
      {status}
    </span>
  );
}

function formatSize(bytes) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function formatDate(value) {
  if (!value) return '—';
  return new Date(value).toLocaleString();
}

// Upload SPPU result PDF, then show recent uploads.
export default function Upload() {
  const navigate = useNavigate();
  const fileInput = useRef(null);
  const [file, setFile] = useState(null);
  const [dragActive, setDragActive] = useState(false);
  const [fileError, setFileError] = useState('');
  const [uploading, setUploading] = useState(false);
  const [result, setResult] = useState(null);
  const [uploads, setUploads] = useState([]);
  const [loadingList, setLoadingList] = useState(true);
  const [listError, setListError] = useState('');

  async function loadUploads() {
    setLoadingList(true);
    setListError('');
    try {
      const data = await uploadService.listUploads();
      setUploads(data);
    } catch (err) {
      setListError(err.response?.data?.message || 'Failed to load uploads.');
    } finally {
      setLoadingList(false);
    }
  }

  useEffect(() => {
    loadUploads();
  }, []);

  function pickFile(selected) {
    setResult(null);
    if (!selected) return;
    const isPdf =
      selected.name.toLowerCase().endsWith('.pdf') ||
      selected.type === 'application/pdf';
    if (!isPdf) {
      setFile(null);
      setFileError('Only PDF files are allowed.');
      return;
    }
    if (selected.size > MAX_SIZE) {
      setFile(null);
      setFileError('File is too large. Max 20MB.');
      return;
    }
    setFileError('');
    setFile(selected);
  }

  function onDrop(e) {
    e.preventDefault();
    setDragActive(false);
    pickFile(e.dataTransfer.files[0]);
  }

  async function onUpload() {
    if (!file) return;
    setUploading(true);
    setResult(null);
    try {
      const batch = await uploadService.uploadPdf(file);
      if (batch.status === 'VALIDATED') {
        setResult({
          type: 'success',
          message: `PDF parsed successfully. ${batch.totalRecords} records saved.`,
          batch,
        });
      } else if (batch.status === 'PARSED') {
        setResult({
          type: 'warning',
          message: `Validation errors found (${batch.errorRecords}). Please review.`,
          batch,
        });
      } else if (batch.status === 'FAILED') {
        setResult({ type: 'error', message: 'Upload failed on the server.', batch: null });
      } else {
        setResult({ type: 'success', message: `Upload status: ${batch.status}`, batch });
      }
      setFile(null);
      await loadUploads();
    } catch (err) {
      setResult({
        type: 'error',
        message: err.response?.data?.message || 'Upload failed. Please try again.',
        batch: null,
      });
    } finally {
      setUploading(false);
    }
  }

  return (
    <div className="space-y-4">
      <p className="text-xs text-slate-500">Dashboard / Upload</p>
      <div>
        <h2 className="text-xl font-semibold text-slate-900">Upload Result PDF</h2>
        <p className="text-sm text-slate-500">
          Upload the SPPU class result PDF to extract and validate student data.
        </p>
      </div>

      <Card>
        <div
          onDragOver={(e) => {
            e.preventDefault();
            setDragActive(true);
          }}
          onDragLeave={() => setDragActive(false)}
          onDrop={onDrop}
          onClick={() => fileInput.current?.click()}
          className={`border-2 border-dashed rounded-xl p-8 text-center cursor-pointer ${
            dragActive ? 'border-primary bg-primary-light' : 'border-slate-300'
          }`}
        >
          <input
            ref={fileInput}
            type="file"
            accept=".pdf"
            className="hidden"
            onChange={(e) => pickFile(e.target.files[0])}
          />
          <p className="text-sm text-slate-600">
            Drag and drop a PDF here, or <span className="text-primary-dark underline">browse files</span>
          </p>
          <p className="text-xs text-slate-400 mt-1">PDF only, max 20MB</p>
        </div>

        {fileError && (
          <div className="mt-3">
            <Alert type="error">{fileError}</Alert>
          </div>
        )}

        {file && (
          <div className="mt-3 flex items-center justify-between bg-slate-50 border rounded-lg px-4 py-2">
            <p className="text-sm text-slate-700">
              {file.name} <span className="text-slate-400">({formatSize(file.size)})</span>
            </p>
            <button
              onClick={() => setFile(null)}
              className="text-xs text-error underline"
            >
              Remove
            </button>
          </div>
        )}

        <div className="mt-4">
          <Button onClick={onUpload} loading={uploading} disabled={!file || uploading}>
            {uploading ? 'Uploading...' : 'Upload PDF'}
          </Button>
        </div>

        {uploading && <Loader />}

        {result && (
          <div className="mt-4 space-y-3">
            <Alert type={result.type}>{result.message}</Alert>
            {result.batch?.status === 'VALIDATED' && (
              <Button onClick={() => navigate('/dashboard')}>View Dashboard</Button>
            )}
            {result.batch?.status === 'PARSED' && (
              <Button onClick={() => navigate(`/validation/${result.batch.id}`)}>
                Review Errors
              </Button>
            )}
          </div>
        )}
      </Card>

      <Card>
        <h3 className="text-lg font-semibold text-slate-900 mb-3">Recent Uploads</h3>
        {loadingList ? (
          <div className="grid md:grid-cols-2 gap-3">
            <SkeletonCard />
            <SkeletonCard />
          </div>
        ) : listError ? (
          <Alert type="error">{listError}</Alert>
        ) : uploads.length === 0 ? (
          <p className="text-sm text-slate-500">
            No uploads yet. Upload your first PDF above.
          </p>
        ) : (
          <Table
            headers={[
              'File Name',
              'Status',
              'Total Records',
              'Errors',
              'Uploaded At',
              'Action',
            ]}
          >
            {uploads.map((u) => (
              <tr key={u.id} className="border-b last:border-0">
                <td className="py-2 pr-4">{u.fileName}</td>
                <td className="py-2 pr-4">
                  <StatusBadge status={u.status} />
                </td>
                <td className="py-2 pr-4">{u.totalRecords}</td>
                <td className="py-2 pr-4">{u.errorRecords}</td>
                <td className="py-2 pr-4 whitespace-nowrap">{formatDate(u.uploadedAt)}</td>
                <td className="py-2 pr-4">
                  {u.status === 'PARSED' && (
                    <button
                      onClick={() => navigate(`/validation/${u.id}`)}
                      className="text-xs text-primary-dark underline"
                    >
                      Review
                    </button>
                  )}
                  {u.status === 'VALIDATED' && (
                    <button
                      onClick={() => navigate('/dashboard')}
                      className="text-xs text-primary-dark underline"
                    >
                      View
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </Table>
        )}
      </Card>
    </div>
  );
}
