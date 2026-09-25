import { useState, useEffect } from 'react';
import api from '../services/api';

// Fetches dashboard data for the given filters. Real charts come in Step 6.
export function useDashboard(filters) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    async function fetchData() {
      setLoading(true);
      setError('');
      try {
        const res = await api.get('/dashboard', { params: filters });
        setData(res.data?.data);
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to load dashboard.');
        setData(null);
      } finally {
        setLoading(false);
      }
    }
    fetchData();
  }, [filters]);

  return { data, loading, error };
}
