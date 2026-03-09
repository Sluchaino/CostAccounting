import React, { useEffect, useState } from 'react';
import { Receipt } from 'lucide-react';
import { TransactionService, CategoryService } from '../../api/services';
import TransactionsSummaryCards from './components/TransactionsSummaryCards';
import TransactionFormCard from './components/TransactionFormCard';
import TransactionFiltersCard from './components/TransactionFiltersCard';
import TransactionsTableCard from './components/TransactionsTableCard';

const emptyForm = {
  date: new Date().toISOString().slice(0, 16),
  amount: '',
  categoryId: '',
  comment: '',
};

const emptySummary = {
  totalIncome: 0,
  totalExpense: 0,
  balance: 0,
};

const TransactionsPage = () => {
  const [transactions, setTransactions] = useState([]);
  const [categories, setCategories] = useState([]);
  const [summary, setSummary] = useState(emptySummary);

  const [filters, setFilters] = useState({
    from: '',
    to: '',
    categoryId: '',
    type: '',
  });

  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [loading, setLoading] = useState(true);

  const loadCategories = async () => {
    const res = await CategoryService.getAll();
    const items = Array.isArray(res.data) ? res.data : [];
    setCategories(items);

    if (!form.categoryId && items.length > 0) {
      setForm((prev) => ({ ...prev, categoryId: items[0].id }));
    }
  };

  const loadSummary = async () => {
    const res = await TransactionService.getSummary();
    setSummary(res.data || emptySummary);
  };

  const loadTransactions = async (currentFilters = filters) => {
    const params = {};

    if (currentFilters.from) params.from = currentFilters.from;
    if (currentFilters.to) params.to = currentFilters.to;
    if (currentFilters.categoryId) params.categoryId = currentFilters.categoryId;
    if (currentFilters.type) params.type = currentFilters.type;

    const res = await TransactionService.getAll(params);
    setTransactions(Array.isArray(res.data) ? res.data : []);
  };

  const loadData = async () => {
    try {
      setLoading(true);
      await Promise.all([loadCategories(), loadSummary(), loadTransactions()]);
    } catch (err) {
      console.error('Ошибка загрузки транзакций:', err);
      alert(err?.response?.data?.message || 'Не удалось загрузить данные');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const resetForm = () => {
    setEditingId(null);
    setForm({
      date: new Date().toISOString().slice(0, 16),
      amount: '',
      categoryId: categories[0]?.id || '',
      comment: '',
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    try {
      const payload = {
        date: new Date(form.date).toISOString(),
        amount: Number(form.amount),
        categoryId: form.categoryId,
        comment: form.comment?.trim() ? form.comment.trim() : null,
      };

      if (editingId) {
        await TransactionService.update(editingId, payload);
      } else {
        await TransactionService.create(payload);
      }

      resetForm();
      await Promise.all([loadSummary(), loadTransactions(filters)]);
    } catch (err) {
      console.error('Ошибка сохранения транзакции:', err);
      alert(err?.response?.data?.message || 'Не удалось сохранить транзакцию');
    }
  };

  const handleEdit = (tx) => {
    setEditingId(tx.id);
    setForm({
      date: tx.date ? tx.date.slice(0, 16) : '',
      amount: tx.amount,
      categoryId: tx.categoryId,
      comment: tx.comment || '',
    });
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Удалить транзакцию?')) return;

    try {
      await TransactionService.delete(id);
      if (editingId === id) resetForm();
      await Promise.all([loadSummary(), loadTransactions(filters)]);
    } catch (err) {
      console.error('Ошибка удаления транзакции:', err);
      alert(err?.response?.data?.message || 'Не удалось удалить транзакцию');
    }
  };

  const applyFilters = async () => {
    await loadTransactions(filters);
  };

  const clearFilters = async () => {
    const cleared = { from: '', to: '', categoryId: '', type: '' };
    setFilters(cleared);
    await loadTransactions(cleared);
  };

  if (loading) {
    return <div>Загрузка транзакций...</div>;
  }

  return (
    <div style={{ maxWidth: '1280px', margin: '0 auto' }}>
      <h1 style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '24px' }}>
        <Receipt color="#4f46e5" />
        Транзакции
      </h1>

      <TransactionsSummaryCards summary={summary} />

      <TransactionFormCard
        form={form}
        setForm={setForm}
        categories={categories}
        editingId={editingId}
        onSubmit={handleSubmit}
        onCancel={resetForm}
      />

      <TransactionFiltersCard
        filters={filters}
        setFilters={setFilters}
        categories={categories}
        onApply={applyFilters}
        onClear={clearFilters}
      />

      <TransactionsTableCard
        transactions={transactions}
        onEdit={handleEdit}
        onDelete={handleDelete}
      />
    </div>
  );
};

export default TransactionsPage;