import api from './api';

export const adminService = {
  getDashboard: async () => {
    const response = await api.get('/admin/dashboard');
    return response.data;
  },

  getAnalytics: async () => {
    const response = await api.get('/admin/analytics');
    return response.data;
  },

  getUsers: async (search = '', page = 0, size = 20) => {
    const response = await api.get('/admin/users', {
      params: { search: search || undefined, page, size },
    });
    return response.data;
  },

  getUserDetail: async (id) => {
    const response = await api.get(`/admin/users/${id}`);
    return response.data;
  },

  updateUserStatus: async (id, status) => {
    const response = await api.patch(`/admin/users/${id}/status`, { status });
    return response.data;
  },

  getAllPlans: async () => {
    const response = await api.get('/admin/plans');
    return response.data;
  },

  updatePlan: async (id, planData) => {
    const response = await api.put(`/admin/plans/${id}`, planData);
    return response.data;
  },

  getSubscriptions: async (page = 0, size = 20) => {
    const response = await api.get('/admin/subscriptions', { params: { page, size } });
    return response.data;
  },

  getPayments: async (page = 0, size = 20) => {
    const response = await api.get('/admin/payments', { params: { page, size } });
    return response.data;
  },

  getInvoices: async (page = 0, size = 20) => {
    const response = await api.get('/admin/invoices', { params: { page, size } });
    return response.data;
  },

  getAuditLogs: async (params = {}) => {
    const response = await api.get('/admin/audit-logs', { params });
    return response.data;
  },

  getHealth: async () => {
    const response = await api.get('/admin/health');
    return response.data;
  },
};
