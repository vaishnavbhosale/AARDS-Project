import { useEffect, useState } from 'react';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  ArcElement,
  Title,
  Tooltip,
  Legend,
} from 'chart.js';
import { Bar, Doughnut } from 'react-chartjs-2';
import dashboardService, { downloadBlob } from '../services/dashboardService';
import { useAuth } from '../context/AuthContext';
import Button from '../components/Button';
import Input from '../components/Input';
import Select from '../components/Select';
import Card from '../components/Card';
import Table from '../components/Table';
import Alert from '../components/Alert';
import SkeletonCard from '../components/SkeletonCard';

ChartJS.register(
  CategoryScale,
  LinearScale,
  BarElement,
  ArcElement,
  Title,
  Tooltip,
  Legend
);

const GRADE_ORDER = ['O', 'A+', 'A', 'B+', 'B', 'C', 'P', 'F'];

function formatPercent(value) {
  const num = Number(value);
  if (Number.isNaN(num)) return '0%';
  return `${num.toFixed(2)}%`;
}

function formatSgpa(value) {
  const num = Number(value);
  if (Number.isNaN(num)) return '0.00';
  return num.toFixed(2);
}

// Green when most students pass, orange in the middle, red when few pass.
function passBarColor(percent) {
  if (percent > 75) return 'bg-green-500';
  if (percent >= 40) return 'bg-orange-500';
  return 'bg-red-500';
}

// Small number card: gray label on top, big bold value below.
function StatCard({ label, value, color }) {
  const colors = {
    green: 'text-green-600',
    red: 'text-red-600',
    blue: 'text-blue-600',
  };
  return (
    <Card>
      <p className="text-xs text-slate-500">{label}</p>
      <p className={`text-2xl font-bold mt-1 ${colors[color] || colors.blue}`}>
        {value}
      </p>
    </Card>
  );
}

function getErrorMessage(err, fallback) {
  return err?.response?.data?.message || fallback;
}

export default function Dashboard() {
  const { user } = useAuth();
  const isHod = user?.role === 'HOD';
  const isPrincipal = user?.role === 'PRINCIPAL';

  const [filters, setFilters] = useState(null);
  const [loadingFilters, setLoadingFilters] = useState(true);
  const [filtersError, setFiltersError] = useState('');

  const [selected, setSelected] = useState({
    sessionId: '',
    departmentId: '',
    year: '1',
    semester: '1',
  });

  const [dashboard, setDashboard] = useState(null);
  const [loadingDashboard, setLoadingDashboard] = useState(false);
  const [dashboardError, setDashboardError] = useState('');

  const [generatingReport, setGeneratingReport] = useState(false);
  const [generatingInstitute, setGeneratingInstitute] = useState(false);
  const [reportError, setReportError] = useState('');
  const [downloadingSubjectId, setDownloadingSubjectId] = useState(null);

  // HODs only get their own department back, so it is the first (only) one.
  const hodDepartment = filters?.departments?.[0];
  const hodDepartmentName = hodDepartment
    ? hodDepartment.code
      ? `${hodDepartment.code} - ${hodDepartment.name}`
      : hodDepartment.name
    : '';

  function currentFilters() {
    return {
      sessionId: selected.sessionId,
      departmentId: selected.departmentId,
      year: Number(selected.year),
      semester: Number(selected.semester),
    };
  }

  async function handleDownloadDepartment() {
    if (!selected.sessionId || !selected.departmentId || generatingReport) return;
    setGeneratingReport(true);
    setReportError('');
    try {
      const blob = await dashboardService.downloadDepartmentReport(currentFilters());
      downloadBlob(
        blob,
        `department-report-${selected.sessionId}-${selected.departmentId}-${selected.year}-${selected.semester}.pdf`
      );
    } catch (err) {
      setReportError(getErrorMessage(err, 'Failed to generate report.'));
    } finally {
      setGeneratingReport(false);
    }
  }

  async function handleDownloadInstitute() {
    if (!selected.sessionId || generatingInstitute) return;
    setGeneratingInstitute(true);
    setReportError('');
    try {
      const blob = await dashboardService.downloadInstituteReport({
        sessionId: selected.sessionId,
        year: Number(selected.year),
        semester: Number(selected.semester),
      });
      downloadBlob(
        blob,
        `institute-report-${selected.sessionId}-${selected.year}-${selected.semester}.pdf`
      );
    } catch (err) {
      setReportError(getErrorMessage(err, 'Failed to generate report.'));
    } finally {
      setGeneratingInstitute(false);
    }
  }

  async function handleDownloadSubject(subject) {
    if (downloadingSubjectId) return;
    setDownloadingSubjectId(subject.subjectId);
    setReportError('');
    try {
      const blob = await dashboardService.downloadSubjectReport(
        subject.subjectId,
        currentFilters()
      );
      downloadBlob(blob, `subject-report-${subject.subjectCode}.pdf`);
    } catch (err) {
      setReportError(getErrorMessage(err, 'Failed to generate report.'));
    } finally {
      setDownloadingSubjectId(null);
    }
  }

  async function loadDashboard(params) {
    if (!params.sessionId || !params.departmentId) return;
    setLoadingDashboard(true);
    setDashboardError('');
    try {
      const data = await dashboardService.getDashboard({
        sessionId: params.sessionId,
        departmentId: params.departmentId,
        year: Number(params.year),
        semester: Number(params.semester),
      });
      setDashboard(data);
    } catch (err) {
      setDashboardError(getErrorMessage(err, 'Failed to load dashboard.'));
    } finally {
      setLoadingDashboard(false);
    }
  }

  // Load filter dropdowns once, then auto-load the dashboard with defaults.
  useEffect(() => {
    async function loadFilters() {
      setLoadingFilters(true);
      setFiltersError('');
      try {
        const data = await dashboardService.getFilters();
        setFilters(data);
        const firstSession = data?.sessions?.[0]?.id ?? '';
        const firstDept = data?.departments?.[0]?.id ?? '';
        const firstYear = data?.years?.[0] ?? 1;
        const firstSemester = data?.semesters?.[0] ?? 1;
        const defaults = {
          sessionId: firstSession === '' ? '' : String(firstSession),
          departmentId: firstDept === '' ? '' : String(firstDept),
          year: String(firstYear),
          semester: String(firstSemester),
        };
        setSelected(defaults);
        await loadDashboard({
          sessionId: defaults.sessionId,
          departmentId: defaults.departmentId,
          year: defaults.year,
          semester: defaults.semester,
        });
      } catch (err) {
        setFiltersError(getErrorMessage(err, 'Failed to load filters.'));
      } finally {
        setLoadingFilters(false);
      }
    }
    loadFilters();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function handleApply() {
    loadDashboard(selected);
  }

  function handleReset() {
    const defaults = {
      sessionId: filters?.sessions?.[0]?.id
        ? String(filters.sessions[0].id)
        : '',
      departmentId: filters?.departments?.[0]?.id
        ? String(filters.departments[0].id)
        : '',
      year: String(filters?.years?.[0] ?? 1),
      semester: String(filters?.semesters?.[0] ?? 1),
    };
    setSelected(defaults);
    loadDashboard(defaults);
  }

  const cards = dashboard?.cards || {};
  const subjects = dashboard?.subjectPerformance || [];
  const backlogs = dashboard?.backlogDistribution || [];
  const grades = dashboard?.gradeDistribution || [];
  const totalStudents = Number(cards.totalStudents || 0);
  const isEmpty = !loadingDashboard && !dashboardError && dashboard && totalStudents === 0;

  const statItems = [
    { label: 'Total Students', value: cards.totalStudents ?? 0, color: 'blue' },
    { label: 'Passed', value: cards.passedStudents ?? 0, color: 'green' },
    { label: 'Failed', value: cards.failedStudents ?? 0, color: 'red' },
    {
      label: 'Overall Pass %',
      value: formatPercent(cards.overallPassPercentage ?? 0),
      color: 'blue',
    },
    {
      label: 'Average SGPA',
      value: formatSgpa(cards.averageSgpa ?? 0),
      color: 'blue',
    },
    {
      label: 'Highest SGPA',
      value: formatSgpa(cards.highestSgpa ?? 0),
      color: 'blue',
    },
    {
      label: 'Lowest SGPA',
      value: formatSgpa(cards.lowestSgpa ?? 0),
      color: 'blue',
    },
    {
      label: 'Students with 1 Backlog',
      value: cards.studentsWith1Backlog ?? 0,
      color: 'blue',
    },
    {
      label: 'Students with 2 Backlogs',
      value: cards.studentsWith2Backlogs ?? 0,
      color: 'blue',
    },
    {
      label: 'Students with 3+ Backlogs',
      value: cards.studentsWith3PlusBacklogs ?? 0,
      color: 'blue',
    },
  ];

  const subjectChartData = {
    labels: subjects.map((s) => s.subjectCode),
    datasets: [
      {
        label: 'Pass %',
        data: subjects.map((s) => Number(s.passPercentage || 0)),
        backgroundColor: 'rgb(59, 130, 246)',
      },
      {
        label: 'Fail %',
        data: subjects.map((s) => 100 - Number(s.passPercentage || 0)),
        backgroundColor: 'rgb(239, 68, 68)',
      },
    ],
  };

  const backlogChartData = {
    labels: backlogs.length > 0 ? backlogs.map((b) => b.label) : ['0', '1', '2', '3+'],
    datasets: [
      {
        data: backlogs.length > 0 ? backlogs.map((b) => b.count) : [0, 0, 0, 0],
        backgroundColor: [
          'rgb(34, 197, 94)',
          'rgb(59, 130, 246)',
          'rgb(249, 115, 22)',
          'rgb(239, 68, 68)',
        ],
      },
    ],
  };

  const orderedGrades = GRADE_ORDER.map((grade) => {
    const found = grades.find((g) => g.grade === grade);
    return { grade, count: found ? found.count : 0 };
  });

  const gradeChartData = {
    labels: orderedGrades.map((g) => g.grade),
    datasets: [
      {
        label: 'Students',
        data: orderedGrades.map((g) => g.count),
        backgroundColor: 'rgb(59, 130, 246)',
      },
    ],
  };

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { position: 'top' } },
  };

  const doughnutOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { position: 'top' } },
  };

  return (
    <div className="space-y-4">
      <p className="text-xs text-slate-500">Dashboard</p>
      <div>
        <h2 className="text-xl font-semibold text-slate-900">Dashboard</h2>
        <p className="text-sm text-slate-500">Academic performance overview</p>
      </div>

      {loadingFilters ? (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
          <SkeletonCard />
          <SkeletonCard />
          <SkeletonCard />
          <SkeletonCard />
        </div>
      ) : filtersError ? (
        <Alert type="error">{filtersError}</Alert>
      ) : (
        <Card>
          <div className={`grid grid-cols-2 gap-3 ${isHod ? 'md:grid-cols-3' : 'md:grid-cols-4'}`}>
            <Select
              label="Academic Session"
              value={selected.sessionId}
              onChange={(e) => setSelected({ ...selected, sessionId: e.target.value })}
              options={(filters?.sessions || []).map((s) => ({
                value: String(s.id),
                label: s.name,
              }))}
            />
            {isHod ? (
              <Input label="Department" value={hodDepartmentName} disabled readOnly />
            ) : (
              <Select
                label="Department"
                value={selected.departmentId}
                onChange={(e) =>
                  setSelected({ ...selected, departmentId: e.target.value })
                }
                options={(filters?.departments || []).map((d) => ({
                  value: String(d.id),
                  label: d.code ? `${d.code} - ${d.name}` : d.name,
                }))}
              />
            )}
            <Select
              label="Year"
              value={selected.year}
              onChange={(e) => setSelected({ ...selected, year: e.target.value })}
              options={(filters?.years?.length ? filters.years : [1, 2, 3, 4]).map(
                (y) => ({ value: String(y), label: `Year ${y}` })
              )}
            />
            <Select
              label="Semester"
              value={selected.semester}
              onChange={(e) => setSelected({ ...selected, semester: e.target.value })}
              options={(
                filters?.semesters?.length
                  ? filters.semesters
                  : [1, 2, 3, 4, 5, 6, 7, 8]
              ).map((s) => ({ value: String(s), label: `Sem ${s}` }))}
            />
          </div>
          <div className="flex gap-2 mt-3 justify-between flex-wrap">
            <div className="flex gap-2">
              <Button onClick={handleApply} loading={loadingDashboard}>
                Apply Filters
              </Button>
              <Button variant="secondary" onClick={handleReset}>
                Reset
              </Button>
            </div>
            <div className="flex gap-2">
              <Button
                variant="secondary"
                onClick={handleDownloadDepartment}
                loading={generatingReport}
                disabled={generatingReport}
              >
                {generatingReport ? 'Generating...' : 'Download Department Report'}
              </Button>
              {isPrincipal && (
                <Button
                  variant="secondary"
                  onClick={handleDownloadInstitute}
                  loading={generatingInstitute}
                  disabled={generatingInstitute}
                >
                  {generatingInstitute ? 'Generating...' : 'Download Institute Report'}
                </Button>
              )}
            </div>
          </div>
        </Card>
      )}

      {reportError && <Alert type="error">{reportError}</Alert>}

      {dashboardError && <Alert type="error">{dashboardError}</Alert>}

      {loadingDashboard ? (
        <>
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-3">
            {Array.from({ length: 10 }).map((_, i) => (
              <SkeletonCard key={i} />
            ))}
          </div>
          <div className="grid md:grid-cols-3 gap-3">
            <SkeletonCard />
            <SkeletonCard />
            <SkeletonCard />
          </div>
        </>
      ) : isEmpty ? (
        <Card>
          <p className="text-sm text-slate-500">
            No data available for the selected filters. Upload a result PDF or try
            different filters.
          </p>
        </Card>
      ) : (
        dashboard && (
          <>
            <Card>
              <p className="text-xs text-slate-500">Topper</p>
              <p className="text-2xl font-bold text-blue-600 mt-1">
                {dashboard.topperName || '—'}
              </p>
              <p className="text-sm text-slate-600">
                SGPA: {dashboard.topperSgpa != null ? formatSgpa(dashboard.topperSgpa) : '—'}
              </p>
            </Card>

            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-3">
              {statItems.map((item) => (
                <StatCard
                  key={item.label}
                  label={item.label}
                  value={item.value}
                  color={item.color}
                />
              ))}
            </div>

            <div className="grid md:grid-cols-3 gap-3">
              <Card>
                <h3 className="text-sm font-semibold text-slate-900 mb-2">
                  Subject-wise Performance
                </h3>
                <div className="h-[300px]">
                  <Bar data={subjectChartData} options={chartOptions} />
                </div>
              </Card>
              <Card>
                <h3 className="text-sm font-semibold text-slate-900 mb-2">
                  Backlog Distribution
                </h3>
                <div className="h-[300px]">
                  <Doughnut data={backlogChartData} options={doughnutOptions} />
                </div>
              </Card>
              <Card>
                <h3 className="text-sm font-semibold text-slate-900 mb-2">
                  Grade Distribution
                </h3>
                <div className="h-[300px]">
                  <Bar data={gradeChartData} options={chartOptions} />
                </div>
              </Card>
            </div>

            <Card>
              <h3 className="text-sm font-semibold text-slate-900 mb-2">
                Subject Performance
              </h3>
              {subjects.length === 0 ? (
                <p className="text-sm text-slate-500">No subject data available.</p>
              ) : (
                <Table
                  headers={[
                    'Subject Code',
                    'Subject Name',
                    'Total',
                    'Passed',
                    'Failed',
                    'Pass %',
                    'Action',
                  ]}
                >
                  {subjects.map((s) => {
                    const percent = Number(s.passPercentage || 0);
                    const downloading = downloadingSubjectId === s.subjectId;
                    return (
                      <tr key={s.subjectId || s.subjectCode} className="border-b last:border-0">
                        <td className="py-2 pr-4">{s.subjectCode}</td>
                        <td className="py-2 pr-4">{s.subjectName}</td>
                        <td className="py-2 pr-4">{s.totalStudents}</td>
                        <td className="py-2 pr-4">{s.passedStudents}</td>
                        <td className="py-2 pr-4">{s.failedStudents}</td>
                        <td className="py-2 pr-4">
                          <div className="flex items-center gap-2">
                            <div className="w-24 bg-slate-200 rounded-full h-2">
                              <div
                                className={`h-2 rounded-full ${passBarColor(percent)}`}
                                style={{ width: `${Math.min(100, Math.max(0, percent))}%` }}
                              />
                            </div>
                            <span className="text-xs text-slate-600">
                              {formatPercent(percent)}
                            </span>
                          </div>
                        </td>
                        <td className="py-2 pr-4">
                          <button
                            onClick={() => handleDownloadSubject(s)}
                            disabled={downloading}
                            className="text-xs text-primary-dark underline disabled:opacity-60"
                          >
                            {downloading ? 'Downloading...' : 'Download'}
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </Table>
              )}
            </Card>

            <Card>
              <div className="bg-slate-100 rounded-lg p-4 opacity-70">
                <h3 className="text-sm font-semibold text-slate-500">
                  AI Recommendations
                </h3>
                <p className="text-sm text-slate-400 mt-1">
                  Recommendations will appear here in the next update. Coming soon.
                </p>
              </div>
            </Card>
          </>
        )
      )}
    </div>
  );
}
