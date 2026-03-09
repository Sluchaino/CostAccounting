import React, { useState } from 'react';
import { BarChart3 } from 'lucide-react';
import { TransactionService } from '../../api/services';
import SectionCard from '../../components/ui/SectionCard';
import ReportsSummaryCards from './components/ReportsSummaryCards';
import CategoryTotalsCard from './components/CategoryTotalsCard';
import { buildPeriodParams } from './reportUtils';

const emptySummary = {
  from: null,
  to: null,
  totalIncome: 0,
  totalExpense: 0,
  balance: 0,
};

const ReportsPage = () => {
  const [summaryFilters, setSummaryFilters] = useState({
    from: '',
    to: '',
  });

  const [categoryFilters, setCategoryFilters] = useState({
    from: '',
    to: '',
    type: 'Expense',
  });

  const [summary, setSummary] = useState(emptySummary);
  const [categoryRows, setCategoryRows] = useState([]);

  const [summaryLoading, setSummaryLoading] = useState(false);
  const [categoryLoading, setCategoryLoading] = useState(false);

  const handleBuildSummary = async () => {
    try {
      setSummaryLoading(true);
      const params = buildPeriodParams(summaryFilters);
      const res = await TransactionService.getSummary(params);
      setSummary(res.data || emptySummary);
    } catch (err) {
      console.error('Ошибка общего отчёта:', err);
      alert(err?.response?.data?.message || 'Не удалось построить общий отчёт');
    } finally {
      setSummaryLoading(false);
    }
  };

  const handleResetSummary = () => {
    setSummaryFilters({ from: '', to: '' });
    setSummary(emptySummary);
  };

  const handleBuildCategoryReport = async () => {
    try {
      setCategoryLoading(true);
      const params = {
        ...buildPeriodParams(categoryFilters),
        type: categoryFilters.type,
      };
      const res = await TransactionService.getByCategory(params);
      setCategoryRows(Array.isArray(res.data) ? res.data : []);
    } catch (err) {
      console.error('Ошибка отчёта по категориям:', err);
      alert(err?.response?.data?.message || 'Не удалось построить отчёт по категориям');
    } finally {
      setCategoryLoading(false);
    }
  };

  const handleResetCategory = () => {
    setCategoryFilters({ from: '', to: '', type: 'Expense' });
    setCategoryRows([]);
  };

  return (
    <div style={{ maxWidth: '1280px', margin: '0 auto', display: 'grid', gap: '24px' }}>
      <h1 style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '0' }}>
        <BarChart3 color="#4f46e5" />
        Отчёты
      </h1>

      <SectionCard>
        <h2 style={{ marginTop: 0, marginBottom: '20px' }}>Общий отчёт за период</h2>

        <div
          style={{
            display: 'grid',
            gridTemplateColumns: '1fr 1fr auto auto',
            gap: '12px',
            alignItems: 'end',
            marginBottom: '24px',
          }}
        >
          <div>
            <label style={{ display: 'block', fontSize: '12px', color: '#64748b', marginBottom: '4px' }}>
              С
            </label>
            <input
              type="date"
              value={summaryFilters.from}
              onChange={(e) => setSummaryFilters((prev) => ({ ...prev, from: e.target.value }))}
              style={{ width: '100%', padding: '10px', borderRadius: '8px', border: '1px solid #ddd' }}
            />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '12px', color: '#64748b', marginBottom: '4px' }}>
              По
            </label>
            <input
              type="date"
              value={summaryFilters.to}
              onChange={(e) => setSummaryFilters((prev) => ({ ...prev, to: e.target.value }))}
              style={{ width: '100%', padding: '10px', borderRadius: '8px', border: '1px solid #ddd' }}
            />
          </div>

          <button
            onClick={handleBuildSummary}
            style={{
              background: '#4f46e5',
              color: 'white',
              border: 'none',
              padding: '10px 18px',
              borderRadius: '8px',
              cursor: 'pointer',
              fontWeight: '600',
            }}
          >
            Построить
          </button>

          <button
            onClick={handleResetSummary}
            style={{
              background: '#e2e8f0',
              color: '#0f172a',
              border: 'none',
              padding: '10px 18px',
              borderRadius: '8px',
              cursor: 'pointer',
              fontWeight: '600',
            }}
          >
            Сбросить
          </button>
        </div>

        {summaryLoading ? <div>Загрузка общего отчёта...</div> : <ReportsSummaryCards summary={summary} />}
      </SectionCard>

      <SectionCard>
        <h2 style={{ marginTop: 0, marginBottom: '20px' }}>Отчёт по категориям за период</h2>

        <div
          style={{
            display: 'grid',
            gridTemplateColumns: '1fr 1fr 1fr auto auto',
            gap: '12px',
            alignItems: 'end',
            marginBottom: '24px',
          }}
        >
          <div>
            <label style={{ display: 'block', fontSize: '12px', color: '#64748b', marginBottom: '4px' }}>
              С
            </label>
            <input
              type="date"
              value={categoryFilters.from}
              onChange={(e) => setCategoryFilters((prev) => ({ ...prev, from: e.target.value }))}
              style={{ width: '100%', padding: '10px', borderRadius: '8px', border: '1px solid #ddd' }}
            />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '12px', color: '#64748b', marginBottom: '4px' }}>
              По
            </label>
            <input
              type="date"
              value={categoryFilters.to}
              onChange={(e) => setCategoryFilters((prev) => ({ ...prev, to: e.target.value }))}
              style={{ width: '100%', padding: '10px', borderRadius: '8px', border: '1px solid #ddd' }}
            />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '12px', color: '#64748b', marginBottom: '4px' }}>
              Тип
            </label>
            <select
              value={categoryFilters.type}
              onChange={(e) => setCategoryFilters((prev) => ({ ...prev, type: e.target.value }))}
              style={{ width: '100%', padding: '10px', borderRadius: '8px', border: '1px solid #ddd' }}
            >
              <option value="Expense">Расходы</option>
              <option value="Income">Доходы</option>
            </select>
          </div>

          <button
            onClick={handleBuildCategoryReport}
            style={{
              background: '#4f46e5',
              color: 'white',
              border: 'none',
              padding: '10px 18px',
              borderRadius: '8px',
              cursor: 'pointer',
              fontWeight: '600',
            }}
          >
            Построить
          </button>

          <button
            onClick={handleResetCategory}
            style={{
              background: '#e2e8f0',
              color: '#0f172a',
              border: 'none',
              padding: '10px 18px',
              borderRadius: '8px',
              cursor: 'pointer',
              fontWeight: '600',
            }}
          >
            Сбросить
          </button>
        </div>

        {categoryLoading ? (
          <div>Загрузка отчёта по категориям...</div>
        ) : (
          <CategoryTotalsCard
            title={categoryFilters.type === 'Income' ? 'Доходы по категориям' : 'Расходы по категориям'}
            color={categoryFilters.type === 'Income' ? '#22c55e' : '#ef4444'}
            rows={categoryRows}
          />
        )}
      </SectionCard>
    </div>
  );
};

export default ReportsPage;