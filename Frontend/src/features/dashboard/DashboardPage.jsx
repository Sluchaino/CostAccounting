import React, { useEffect, useMemo, useState } from 'react';
import DashboardSummaryCards from './components/DashboardSummaryCards';
import RecentTransactionsCard from './components/RecentTransactionsCard';
import DashboardCategoryPreviewCard from './components/DashboardCategoryPreviewCard';
import { TransactionService, CategoryService } from '../../api/services';

const emptySummary = {
  totalIncome: 0,
  totalExpense: 0,
  balance: 0,
};

const DashboardPage = () => {
  const [summary, setSummary] = useState(emptySummary);
  const [transactions, setTransactions] = useState([]);
  const [expenseByCategory, setExpenseByCategory] = useState([]);
  const [incomeByCategory, setIncomeByCategory] = useState([]);
  const [loading, setLoading] = useState(true);

  const loadData = async () => {
    try {
      setLoading(true);

      const [
        summaryRes,
        transactionsRes,
        expenseReportRes,
        incomeReportRes,
      ] = await Promise.all([
        TransactionService.getSummary(),
        TransactionService.getAll(),
        TransactionService.getByCategory({ type: 'Expense' }),
        TransactionService.getByCategory({ type: 'Income' }),
      ]);

      const transactionsData = Array.isArray(transactionsRes.data) ? transactionsRes.data : [];

      setSummary(summaryRes.data || emptySummary);
      setTransactions(transactionsData);
      setExpenseByCategory(Array.isArray(expenseReportRes.data) ? expenseReportRes.data : []);
      setIncomeByCategory(Array.isArray(incomeReportRes.data) ? incomeReportRes.data : []);
    } catch (err) {
      console.error('Ошибка загрузки dashboard:', err);
      alert(err?.response?.data?.message || 'Не удалось загрузить данные');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const recentTransactions = useMemo(() => {
    return [...transactions]
      .sort((a, b) => {
        const dateDiff = new Date(b.date) - new Date(a.date);
        if (dateDiff !== 0) return dateDiff;
        return new Date(b.createdAt || 0) - new Date(a.createdAt || 0);
      })
      .slice(0, 5);
  }, [transactions]);

  if (loading) {
    return <div style={{ padding: '40px' }}>Загрузка обзора...</div>;
  }

  return (
    <div style={{ maxWidth: '1280px', margin: '0 auto' }}>
      <h1 style={{ fontSize: '28px', fontWeight: '700', marginBottom: '24px' }}>
        Обзор финансов
      </h1>

      <DashboardSummaryCards summary={summary} />

     

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: '1.4fr 1fr',
          gap: '24px',
          alignItems: 'start',
        }}
      >
        <RecentTransactionsCard transactions={recentTransactions} />

        <div style={{ display: 'grid', gap: '24px' }}>
          <DashboardCategoryPreviewCard
            title="Расходы по категориям"
            color="#ef4444"
            rows={expenseByCategory.slice(0, 6)}
          />

          <DashboardCategoryPreviewCard
            title="Доходы по категориям"
            color="#22c55e"
            rows={incomeByCategory.slice(0, 6)}
          />
        </div>
      </div>
    </div>
  );
};

export default DashboardPage;