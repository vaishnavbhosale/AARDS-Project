import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../services/api';

export default function Login() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ username: '', password: '' });
  const [error, setError] = useState('');

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const res = await api.post('/auth/login', form);
      const token = res.data?.data?.token;
      if (token) localStorage.setItem('aards_token', token);
      navigate('/dashboard');
    } catch {
      // Skeleton fallback so UI flow works without backend running
      if (form.username && form.password) {
        localStorage.setItem('aards_token', 'skeleton-token');
        navigate('/dashboard');
      } else {
        setError('Enter username and password');
      }
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50 px-4">
      <form onSubmit={onSubmit} className="w-full max-w-sm bg-white rounded-xl shadow p-6 space-y-4">
        <div>
          <h1 className="text-2xl font-semibold">AARDS Login</h1>
          <p className="text-sm text-slate-500">SPPU Result Analytics & Decision Support</p>
        </div>
        {error && <p className="text-sm text-error">{error}</p>}
        <input
          name="username"
          placeholder="Username"
          value={form.username}
          onChange={onChange}
          className="w-full border rounded-lg px-3 py-2"
        />
        <input
          name="password"
          type="password"
          placeholder="Password"
          value={form.password}
          onChange={onChange}
          className="w-full border rounded-lg px-3 py-2"
        />
        <button type="submit" className="w-full bg-primary text-white rounded-lg py-2 hover:bg-primary-dark">
          Login
        </button>
        <p className="text-xs text-slate-500">
          Skeleton only. Backend: <code>/api/v1/auth/login</code>. <Link className="underline" to="/dashboard">Go to dashboard →</Link>
        </p>
      </form>
    </div>
  );
}
