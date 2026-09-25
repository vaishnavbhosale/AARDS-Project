import { useEffect, useState } from 'react';
import { Bar, Doughnut } from 'react-chartjs-2';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  ArcElement,
  Tooltip,
  Legend,
} from 'chart.js';
import api from '../services/api';
import SkeletonCard from '../components/SkeletonCard';

ChartJS.register(CategoryScale, LinearScale, BarElement, ArcElement, Tooltip, Legend);

const FILTERS = {
  session: ['2023-24', '2024-25'],
  year: ['SE', 'TE', 'BE'],
  semester: ['Sem 1', 'Sem 2'],
  department: ['Computer', 'IT', 'E&TC', 'Mechanical'],
};

export default function Dashboard() {
  const [loading, setLoading] = useState(true);
  const [data, setData] = useState(null);
  const [filters, setFilters] = useState({ session: '2024-25', year: 'TE', semester: 'Sem 1', department: 'Computer' });

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    api
      .get('/dashboard', { params: filters })
      .then((res) => mounted && setData(res.data?.data))
      .catch(() =>
        mounted &&
        setData({ totalStudents: 0, passed: 0, failed: 0, overallPassPct: 0, averageSgpa: 0 })
      )
      .finally(() => mounted && setLoading(false));
    return () => {
      mounted = false;
    };
  }, [filters]);

  const cards = [
    { label: 'Total Students', value: data?.totalStudents ?? '—' },
    { label: 'Passed', value: data?.passed ?? '—' },
    { label: 'Failed', value: data?.failed ?? '—' },
    { label: 'Overall Pass %', value: data?.overallPassPct ?? '—' },
    { label: 'Average SGPA', value: data?.averageSgpa ?? '—' },
    { label: 'Highest SGPA', value: '—' },
    { label: 'Lowest SGPA', value: '—' },
    { label: '1 Backlog', value: '—' },
    { label: '2 Backlogs', value: '—' },
    { label: '3+ Backlogs', value: '—' },
    { label: 'Topper', value: '—' },
  ];

  return (
    <div className="p-6 space-y-6">
      <h1 className="text-2xl font-semibold">Academic Dashboard</h1>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        {Object.entries(FILTERS).map(([key, options]) => (
          <label key={key} className="text-sm">
            <span className="capitalize text-slate-500">{key}</span>
            <select
              value={filters[key]}
              onChange={(e) => setFilters({ ...filters, [key]: e.target.value })}
              className="mt-1 w-full border rounded-lg px-2 py-2 bg-white"
            >
              {options.map((o) => (
                <option key={o}>{o}</option>
              ))}
            </select>
          </label>
        ))}
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {loading
          ? Array.from({ length: 8 }).map((_, i) => <SkeletonCard key={i} />)
          : cards.map((c) => (
              <div key={c.label} className="bg-white rounded-xl shadow p-4">
                <p className="text-xs text-slate-500">{c.label}</p>
                <p className="text-xl font-semibold">{c.value}</p>
              </div>
            ))}
      </div>

      <div className="grid md:grid-cols-2 gap-4">
        <div className="bg-white rounded-xl shadow p-4">
          <h2 className="font-medium mb-2">Subject-wise Performance</h2>
          {loading ? (
            <div className="h-48 animate-pulse bg-slate-100 rounded" />
          ) : (
            <Bar
              data={{ labels: ['Sub1', 'Sub2', 'Sub3'], datasets: [{ label: 'Pass %', data: [80, 65, 90] }] }}
            />
          )}
        </div>
        <div className="bg-white rounded-xl shadow p-4">
          <h2 className="font-medium mb-2">Backlog Distribution</h2>
          {loading ? (
            <div className="h-48 animate-pulse bg-slate-100 rounded" />
          ) : (
            <Doughnut
              data={{
                labels: ['0', '1', '2', '3+'],
                datasets: [{ data: [60, 20, 12, 8] }],
              }}
            />
          )}
        </div>
      </div>

      <div className="bg-white rounded-xl shadow p-4">
        <h2 className="font-medium">Recommendation Panel</h2>
        <p className="text-sm text-slate-500">Problem / Reason / Recommendation / Priority (High red, Medium orange, Low green)</p>
      </div>
    </div>
  );
}
