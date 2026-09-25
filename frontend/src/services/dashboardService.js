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
};

export default dashboardService;
