import api from './api';

export const usageService = {
  getCurrentUsage: async () => {
    const response = await api.get('/usage/current');
    return response.data.data;
  },

  getUsageHistory: async (metric) => {
    const response = await api.get('/usage/history', { params: metric ? { metric } : {} });
    return response.data.data;
  },

  getUsageSummary: async () => {
    const response = await api.get('/usage/summary');
    return response.data.data;
  },

  recordUsage: async (metric, quantity, metadata) => {
    const response = await api.post('/usage/simulate', { metric, quantity, metadata });
    return response.data.data;
  },

  simulateUsage: async (metric, quantity, metadata) => {
    const response = await api.post('/usage/simulate', { metric, quantity, metadata });
    return response.data.data;
  },
};
