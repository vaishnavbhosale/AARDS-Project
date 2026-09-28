import { useEffect, useState } from 'react';
import dashboardService, { downloadBlob } from '../services/dashboardService';
import reportService from '../services/reportService';
import Button from '../components/Button';
import Select from '../components/Select';
import Card from '../components/Card';
import Table from '../components/Table';
import Alert from '../components/Alert';
import SkeletonCard from '../components/SkeletonCard';

function getErrorMessage(err, fallback) {
  return err?.response?.data?.message || fallback;
}

// Small stat box: label on top, big value below.
function StatBox({ label, value, sub }) {
  return (
    <Card>
      <p className="text-xs text-slate-500">{label}</p>
      <p className="text-2xl font-bold mt-1 text-blue-600">{value}</p>
      {sub && <p className="text-xs text-slate-500 mt-1">{sub}</p>}
    </Card>
  );
}

// SE Combine preview + PDF download, mirroring the Excel sheet.
export default function SECombineReport() {
  const [filters, setFilters] = useState(null);
  const [loadingFilters, setLoadingFilters] = useState(true);
  const [filtersError, setFiltersError] = useState('');

  const [selected, setSelected] = useState({
    sessionId: '',
    departmentId: '',
    year: '2',
    semester: '3',
  });

  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [downloading, setDownloading] = useState(false);
  const [downloadError, setDownloadError] = useState('');

  async function loadReport(params) {
    if (!params.sessionId || !params.departmentId) return;
    setLoading(true);
    setError('');
    try {
      const data = await reportService.fetchSECombineReport({
        sessionId: params.sessionId,
        departmentId: params.departmentId,
        year: Number(params.year),
        semester: Number(params.semester),
      });
      setReport(data);
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to load SE Combine report.'));
    } finally {
      setLoading(false);
    }
  }

  // Load dropdowns once, then auto-load the preview with defaults.
  useEffect(() => {
    async function loadFilters() {
      setLoadingFilters(true);
      setFiltersError('');
      try {
        const data = await dashboardService.getFilters();
        setFilters(data);
        const defaults = {
          sessionId: data?.sessions?.[0]?.id ? String(data.sessions[0].id) : '',
          departmentId: data?.departments?.[0]?.id
            ? String(data.departments[0].id)
            : '',
          year: String(data?.years?.[1] ?? 2),
          semester: String(data?.semesters?.[2] ?? 3),
        };
        setSelected(defaults);
        await loadReport(defaults);
      } catch (err) {
        setFiltersError(getErrorMessage(err, 'Failed to load filters.'));
      } finally {
        setLoadingFilters(false);
      }
    }
    loadFilters();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function handleRefresh() {
    loadReport(selected);
  }

  async function handleDownload() {
    if (!selected.sessionId || !selected.departmentId || downloading) return;
    setDownloading(true);
    setDownloadError('');
    try {
      const blob = await reportService.exportSECombineReport({
        sessionId: selected.sessionId,
        departmentId: selected.departmentId,
        year: Number(selected.year),
        semester: Number(selected.semester),
      });
      downloadBlob(
        blob,
        `SE-Combine-Report-${selected.sessionId}-${selected.departmentId}.pdf`
      );
    } catch (err) {
      setDownloadError(getErrorMessage(err, 'Failed to generate report.'));
    } finally {
      setDownloading(false);
    }
  }

  const dist = report?.distribution;
  const backlog = report?.backlog;
  const overall = report?.overall;
  const isEmpty = !loading && !error && report && overall?.totalAppeared === 0;

  const distCards = dist
    ? [
        { label: 'Distinction', ...dist.distinction },
        { label: 'First Class', ...dist.firstClass },
        { label: 'Higher Second', ...dist.higherSecond },
        { label: 'Second Class', ...dist.secondClass },
        { label: 'Pass Class', ...dist.passClass },
      ]
    : [];

  const backlogCards = backlog
    ? [
        { label: 'Failed in 1', value: backlog.failedInOne },
        { label: 'Failed in 2', value: backlog.failedInTwo },
        { label: 'Failed in 3', value: backlog.failedInThree },
        { label: 'Failed in 4', value: backlog.failedInFour },
        { label: 'Failed in 5+', value: backlog.failedInFiveOrMore },
      ]
    : [];

  const overallRows = overall
    ? [
        ['Total Appeared', `${overall.totalAppeared}`],
        ['All Clear', `${overall.allClear} (${overall.allClearPct}%)`],
        ['Quality (>= 6.75)', `${overall.quality} (${overall.qualityPct}%)`],
        ['With ATKT', `${overall.withAtkt} (${overall.withAtktPct}%)`],
        ['Fail', `${overall.fail} (${overall.failPct}%)`],
        ['Absent', `${overall.absent}`],
      ]
    : [];

  return (
    <div className="space-y-4">
      <p className="text-xs text-slate-500">Reports / SE Combine</p>
      <div>
        <h2 className="text-xl font-semibold text-slate-900">SE Combine Report</h2>
        <p className="text-sm text-slate-500">
          Distribution, backlog, subject tables and toppers
        </p>
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
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
            <Select
              label="Academic Session"
              value={selected.sessionId}
              onChange={(e) => setSelected({ ...selected, sessionId: e.target.value })}
              options={(filters?.sessions || []).map((s) => ({
                value: String(s.id),
                label: s.name,
              }))}
            />
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
            <Button onClick={handleRefresh} loading={loading}>
              Refresh
            </Button>
            <Button
              variant="secondary"
              onClick={handleDownload}
              loading={downloading}
              disabled={downloading}
            >
              {downloading ? 'Generating...' : 'Download PDF Report'}
            </Button>
          </div>
        </Card>
      )}

      {error && <Alert type="error">{error}</Alert>}
      {downloadError && <Alert type="error">{downloadError}</Alert>}

      {loading ? (
        <>
          <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
            {Array.from({ length: 5 }).map((_, i) => (
              <SkeletonCard key={i} />
            ))}
          </div>
          <SkeletonCard />
          <SkeletonCard />
        </>
      ) : isEmpty ? (
        <Card>
          <p className="text-sm text-slate-500">
            No data available for the selected filters. Upload a result PDF or try
            different filters.
          </p>
        </Card>
      ) : (
        report && (
          <>
            <Card>
              <p className="text-lg font-semibold text-slate-900">
                {report.header?.collegeName}
              </p>
              <p className="text-sm text-slate-600">
                {report.header?.departmentName} | Session {report.header?.sessionName} |{' '}
                {report.header?.yearLabel} | Generated on {report.header?.generatedOn}
              </p>
              <p className="text-sm text-slate-600">
                Total Appeared: {report.header?.totalAppearedHeader}
              </p>
            </Card>

            <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
              {distCards.map((c) => (
                <StatBox
                  key={c.label}
                  label={c.label}
                  value={c.count}
                  sub={`${c.percentage}%`}
                />
              ))}
            </div>

            <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
              {backlogCards.map((c) => (
                <StatBox key={c.label} label={c.label} value={c.value} />
              ))}
            </div>

            <Card>
              <h3 className="text-sm font-semibold text-slate-900 mb-2">
                Overall Result
              </h3>
              <Table headers={['Metric', 'Value']}>
                {overallRows.map(([label, value]) => (
                  <tr key={label} className="border-b last:border-0">
                    <td className="py-2 pr-4">{label}</td>
                    <td className="py-2 pr-4">{value}</td>
                  </tr>
                ))}
              </Table>
            </Card>

            {(report.semesters || []).map((block) => (
              <Card key={block.semesterNumber}>
                <h3 className="text-sm font-semibold text-slate-900 mb-2">
                  {block.semesterDisplayName}
                </h3>
                {block.subjects.length === 0 ? (
                  <p className="text-sm text-slate-500">No subject data available.</p>
                ) : (
                  <Table
                    headers={[
                      'SN',
                      'Subject Name',
                      'Faculty',
                      'On Roll',
                      'Appeared',
                      'Passed',
                      'Pass %',
                      'Dist',
                      'I Class',
                      'H.II',
                      'II Class',
                      'Pass',
                      'Highest',
                    ]}
                  >
                    {block.subjects.map((s) => (
                      <tr
                        key={`${s.subjectCode}-${s.sn}`}
                        className="border-b last:border-0"
                      >
                        <td className="py-2 pr-4">{s.sn}</td>
                        <td className="py-2 pr-4">{s.subjectName}</td>
                        <td className="py-2 pr-4">{s.facultyName}</td>
                        <td className="py-2 pr-4">{s.onRoll}</td>
                        <td className="py-2 pr-4">{s.appeared}</td>
                        <td className="py-2 pr-4">{s.passed}</td>
                        <td className="py-2 pr-4">{s.passingPercentage}%</td>
                        <td className="py-2 pr-4">{s.distinction}</td>
                        <td className="py-2 pr-4">{s.firstClass}</td>
                        <td className="py-2 pr-4">{s.higherSecond}</td>
                        <td className="py-2 pr-4">{s.secondClass}</td>
                        <td className="py-2 pr-4">{s.passClass}</td>
                        <td className="py-2 pr-4">{s.highestMarks ?? '—'}</td>
                      </tr>
                    ))}
                  </Table>
                )}
              </Card>
            ))}

            <Card>
              <h3 className="text-sm font-semibold text-slate-900 mb-2">
                List of Topper Students
              </h3>
              {(report.toppers || []).length === 0 ? (
                <p className="text-sm text-slate-500">No toppers found.</p>
              ) : (
                <Table headers={['Rank', 'Name', 'SGPA']}>
                  {report.toppers.map((t) => (
                    <tr key={t.rank} className="border-b last:border-0">
                      <td className="py-2 pr-4">{t.rank}</td>
                      <td className="py-2 pr-4">{t.name}</td>
                      <td className="py-2 pr-4">{t.sgpa}</td>
                    </tr>
                  ))}
                </Table>
              )}
            </Card>
          </>
        )
      )}
    </div>
  );
}
