import api from './api';

export const aiService = {
  generateText: async (prompt, model = 'gemini-1.5-flash', parameters = {}) => {
    const response = await api.post('/ai/generate', { prompt, model, parameters });
    return response.data;
  },

  chat: async (messages, model = 'gemini-1.5-flash', parameters = {}) => {
    const response = await api.post('/ai/chat', { messages, model, parameters });
    return response.data;
  },

  generateImage: async (prompt, options = {}) => {
    const response = await api.post('/ai/image/generate', {
      prompt,
      model: options.model || 'flux-schnell',
      aspectRatio: options.aspectRatio || '1:1',
      stylePreset: options.stylePreset || 'Cinematic',
      parameters: options.parameters || {},
    });
    return response.data;
  },

  multimodalGenerate: async (formData) => {
    const response = await api.post('/ai/multimodal', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  },

  getModels: async () => {
    const response = await api.get('/ai/models');
    return response.data;
  },

  getImageModels: async () => {
    const response = await api.get('/ai/image/models');
    return response.data;
  },
};
