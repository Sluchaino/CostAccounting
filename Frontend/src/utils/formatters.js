export const CATEGORY_TYPE_OPTIONS = [
  { value: 'Expense', label: 'Расход' },
  { value: 'Income', label: 'Доход' },
];

export const getCategoryTypeLabel = (type) => (type === 'Income' ? 'Доход' : 'Расход');

export const isIncome = (type) => type === 'Income';

export const formatCurrency = (amount) => {
  return new Intl.NumberFormat('ru-RU', {
    style: 'currency',
    currency: 'RUB',
    maximumFractionDigits: 2,
  }).format(Number(amount) || 0);
};

export const formatDate = (dateString) => {
  if (!dateString) return '';
  return new Date(dateString).toLocaleDateString('ru-RU');
};

export const formatDateTime = (dateString) => {
  if (!dateString) return '';
  return new Date(dateString).toLocaleString('ru-RU');
};

export const toDateInputValue = (dateString) => {
  if (!dateString) return '';
  return new Date(dateString).toISOString().split('T')[0];
};

export const normalizeApiError = (error, fallback = 'Произошла ошибка.') => {
  return error?.response?.data?.message || fallback;
};
