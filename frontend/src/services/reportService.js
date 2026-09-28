import api from './api';

// SE Combine report: JSON preview + PDF download.
const reportService = {
  async fetchSECombineReport({ sessionId, departmentId, year, semester }) {
    const res = await api.get('/analytics/se-combine', {
      params: { sessionId, departmentId, year, semester },
    });
    return res.data.data;
  },

  async exportSECombineReport({ sessionId, departmentId, year, semester }) {
    const res = await api.get('/reports/se-combine', {
      params: { sessionId, departmentId, year, semester },
      responseType: 'blob',
    });
    return res.data;
  },
};

export default reportService;
