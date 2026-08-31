import api from './api';

export const userService = {
  getProfile: async () => {
    const response = await api.get('/users/me');
    return response.data;
  },

  updateProfile: async (data) => {
    const response = await api.patch('/users/me', data);
    return response.data;
  },

  changePassword: async (data) => {
    const response = await api.post('/users/me/change-password', data);
    return response.data;
  },

  getSessions: async () => {
    const response = await api.get('/users/me/sessions');
    return response.data;
  },

  revokeAllSessions: async () => {
    const response = await api.post('/users/me/sessions/revoke-all');
    return response.data;
  },
};
