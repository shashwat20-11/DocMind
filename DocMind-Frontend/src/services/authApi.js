import api from './api';

export const authApi = {
  login: async (credentials) => {
    const { data } = await api.post('/auth/login', credentials);
    return data;
  },

  register: async (payload) => {
    const { data } = await api.post('/auth/register', payload);
    return data;
  },
};

export default authApi;