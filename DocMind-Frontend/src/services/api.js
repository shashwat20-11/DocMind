import axios from 'axios';
import { useAuthStore } from '../store/authStore';

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Attach JWT token to every request from the Zustand auth store
api.interceptors.request.use(
  (config) => {
    const token = useAuthStore.getState().token;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Catch 401 Unauthorized responses, log out, and redirect to /login
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      useAuthStore.getState().logout();
      if (window.location.pathname !== '/login' && window.location.pathname !== '/register') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

// Document APIs
export const documentApi = {
  upload: async (file, onProgress) => {
    const formData = new FormData();
    formData.append('file', file);
    const { data } = await api.post('/documents/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (e) => {
        if (e.total && onProgress) {
          onProgress(Math.round((e.loaded / e.total) * 100));
        }
      },
    });
    return data;
  },

  uploadMultiple: async (files, onProgress) => {
    const formData = new FormData();
    files.forEach((f) => formData.append('files', f));
    const { data } = await api.post('/documents/upload-multiple', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (e) => {
        if (e.total && onProgress) {
          onProgress(Math.round((e.loaded / e.total) * 100));
        }
      },
    });
    return data;
  },

  // Logged-in user's own documents
  getMine: async () => {
    const { data } = await api.get('/documents/user');
    return data;
  },

  // Admin-only all documents
  getAll: async () => {
    const { data } = await api.get('/documents');
    return data;
  },

  getById: async (id) => {
    const { data } = await api.get(`/documents/${id}`);
    return data;
  },

  delete: async (id) => {
    const { data } = await api.delete(`/documents/${id}`);
    return data;
  },
};

// Chat APIs
export const chatApi = {
  query: async (request) => {
    const { data } = await api.post('/chat/query', request);
    return data;
  },

  searchSimilarity: async (request) => {
    const { data } = await api.post('/chat/search/similarity', request);
    return data;
  },

  // Helper for raw fetch streaming with auth header
  stream: async (request, signal) => {
    const token = useAuthStore.getState().token;
    const headers = {
      'Content-Type': 'application/json',
    };
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch('/api/chat/stream', {
      method: 'POST',
      headers,
      body: JSON.stringify(request),
      signal,
    });

    if (response.status === 401) {
      useAuthStore.getState().logout();
      if (window.location.pathname !== '/login' && window.location.pathname !== '/register') {
        window.location.href = '/login';
      }
    }

    return response;
  },
};

export default api;