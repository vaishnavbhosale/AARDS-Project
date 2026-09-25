import api from './api';

// Upload PDF and fetch upload batches.
const uploadService = {
  async uploadPdf(file) {
    const formData = new FormData();
    formData.append('file', file);
    const res = await api.post('/uploads', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return res.data.data;
  },

  async listUploads() {
    const res = await api.get('/uploads');
    return res.data.data;
  },

  async getUpload(id) {
    const res = await api.get(`/uploads/${id}`);
    return res.data.data;
  },
};

export default uploadService;
