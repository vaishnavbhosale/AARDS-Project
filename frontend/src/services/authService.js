import api from './api';

// All auth API calls in one place. Token storage lives in AuthContext.
const authService = {
  async login(username, password) {
    const res = await api.post('/auth/login', { username, password });
    return res.data.data;
  },

  async getCurrentUser() {
    const res = await api.get('/auth/me');
    return res.data.data;
  },

  logout() {
    localStorage.removeItem('aards_token');
    localStorage.removeItem('aards_user');
  },

  async registerUser(payload) {
    const res = await api.post('/auth/register', payload);
    return res.data.data;
  },
};

export default authService;
