import { useState } from 'react';
import { useNavigate, Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Button from '../components/Button';
import Input from '../components/Input';
import Alert from '../components/Alert';

// Login screen. Saves token + user, then goes to dashboard.
export default function Login() {
  const navigate = useNavigate();
  const { login, isAuthenticated } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  if (isAuthenticated()) {
    return <Navigate to="/dashboard" replace />;
  }

  function errorMessage(err) {
    return (
      err.response?.data?.message || 'Login failed. Please check your details.'
    );
  }

  async function onSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await login(username, password);
      navigate('/dashboard');
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center px-4 bg-gradient-to-br from-primary-light via-slate-50 to-slate-100">
      <form
        onSubmit={onSubmit}
        className="w-full max-w-sm bg-white rounded-xl shadow p-6 space-y-4"
      >
        <div>
          <h1 className="text-2xl font-bold text-slate-900">AARDS</h1>
          <p className="text-sm text-slate-500">
            Academic Result Analytics & Decision Support
          </p>
        </div>
        {error && <Alert type="error">{error}</Alert>}
        <Input
          label="Username"
          placeholder="Enter username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          required
        />
        <div>
          <Input
            label="Password"
            type={showPassword ? 'text' : 'password'}
            placeholder="Enter password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
          <button
            type="button"
            onClick={() => setShowPassword(!showPassword)}
            className="text-xs text-primary-dark mt-1"
          >
            {showPassword ? 'Hide password' : 'Show password'}
          </button>
        </div>
        <Button type="submit" loading={loading} className="w-full">
          {loading ? 'Signing in...' : 'Sign in'}
        </Button>
        <p className="text-xs text-slate-500 text-center">
          Default: admin / admin123
        </p>
      </form>
    </div>
  );
}
