import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:5288/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

export const CategoryService = {
  getAll: () => api.get('/categories'),
  getById: (id) => api.get(`/categories/${id}`),
  create: (data) => api.post('/categories', data),
  update: (id, data) => api.put(`/categories/${id}`, data),
  delete: (id) => api.delete(`/categories/${id}`),
};

export const TransactionService = {
  getAll: (params) => api.get('/transactions', { params }),
  getById: (id) => api.get(`/transactions/${id}`),
  create: (data) => api.post('/transactions', data),
  update: (id, data) => api.put(`/transactions/${id}`, data),
  delete: (id) => api.delete(`/transactions/${id}`),
  getSummary: (params) => api.get('/reports/summary', { params }),
  getByCategory: (params) => api.get('/reports/by-category', { params }),
};