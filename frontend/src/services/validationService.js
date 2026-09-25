import api from './api';

// Teacher fixes doubtful values here, then approves the batch.
const validationService = {
  async getBatchErrors(batchId) {
    const res = await api.get(`/validation/batch/${batchId}`);
    return res.data.data;
  },

  async updateError(errorId, correctedValue) {
    const res = await api.put(`/validation/${errorId}`, { correctedValue });
    return res.data.data;
  },

  async approveBatch(batchId) {
    const res = await api.post(`/validation/batch/${batchId}/approve`);
    return res.data.data;
  },
};

export default validationService;
