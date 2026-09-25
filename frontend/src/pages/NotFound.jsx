import { Link } from 'react-router-dom';

// Shown for unknown URLs.
export default function NotFound() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50 px-4">
      <div className="text-center">
        <p className="text-5xl font-bold text-slate-900">404</p>
        <p className="text-slate-500 mt-2">Page not found.</p>
        <Link
          to="/dashboard"
          className="inline-block mt-4 text-sm bg-primary text-white rounded-lg px-4 py-2"
        >
          Go to Dashboard
        </Link>
      </div>
    </div>
  );
}
