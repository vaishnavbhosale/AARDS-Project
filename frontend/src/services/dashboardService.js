import api from './api';

// Dashboard filters and analytics data.
const dashboardService = {
  async getFilters() {
    const res = await api.get('/dashboard/filters');
    return res.data.data;
  },

  async getDashboard({ sessionId, departmentId, year, semester }) {
    const res = await api.get('/dashboard', {
      params: { sessionId, departmentId, year, semester },
    });
    return res.data.data;
  },

  async downloadDepartmentReport({ sessionId, departmentId, year, semester }) {
    const res = await api.get('/reports/department', {
      params: { sessionId, departmentId, year, semester },
      responseType: 'blob',
    });
    return res.data;
  },

  async downloadSubjectReport(subjectId, { sessionId, departmentId, year, semester }) {
    const res = await api.get('/reports/subject', {
      params: { subjectId, sessionId, departmentId, year, semester },
      responseType: 'blob',
    });
    return res.data;
  },

  async downloadInstituteReport({ sessionId, year, semester }) {
    const res = await api.get('/reports/institute', {
      params: { sessionId, year, semester },
      responseType: 'blob',
    });
    return res.data;
  },
};

export default dashboardService;

// Save a blob (e.g. a generated PDF) as a file in the browser.
export const downloadBlob = (blob, filename) => {
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  window.URL.revokeObjectURL(url);
};
