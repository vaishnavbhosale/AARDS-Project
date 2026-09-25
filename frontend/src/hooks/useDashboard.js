import { useState, useEffect } from 'react';
import api from '../services/api';

export function useDashboard(filters) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    api
      .get('/dashboard', { params: filters })
      .then((res) => setData(res.data?.data))
      .catch(() => setData(null))
      .finally(() => setLoading(false));
  }, [filters]);

  return { data, loading };
}
