import api from './api';

export const billingService = {
  createPaymentOrder: async (planCode, billingInterval) => {
    const response = await api.post('/billing/orders/create', { planCode, billingInterval });
    return response.data;
  },

  verifyPayment: async (paymentData) => {
    const response = await api.post('/billing/orders/verify', paymentData);
    return response.data;
  },

  getPaymentOrders: async () => {
    const response = await api.get('/billing/orders');
    return response.data;
  },

  getBillingConfig: async () => {
    const response = await api.get('/billing/config');
    return response.data;
  },

  getInvoices: async () => {
    const response = await api.get('/billing/invoices');
    return response.data;
  },

  getInvoiceById: async (invoiceId) => {
    const response = await api.get(`/billing/invoices/${invoiceId}`);
    return response.data;
  },

  getCurrentSubscription: async () => {
    const response = await api.get('/billing/subscription/current');
    return response.data;
  },

  cancelSubscription: async () => {
    const response = await api.post('/billing/subscription/cancel');
    return response.data;
  },

  resumeSubscription: async () => {
    const response = await api.post('/billing/subscription/resume');
    return response.data;
  },

  getPlans: async () => {
    const response = await api.get('/plans');
    return response.data;
  },
};
