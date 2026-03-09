import React, { useState } from 'react';
import { BarChart3 } from 'lucide-react';
import { TransactionService } from '../../../api/services';
import { buildPeriodParams } from '../reportUtils';
import ReportPeriodFilter from './ReportPeriodFilter';
import SummaryCards from './SummaryCards';

const sectionStyle = {
  background: 'white',
  padding: '24px',
  borderRadius: '16px',
  boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
};

const emptySummary = {
  from: null,
  to: null,
  totalIncome: 0,
  totalExpense: 0,
  balance: 0,
};

const SummaryReportSection = () => {
  const [filters, setFilters] = useState({
    from: '',
    to: '',
  });

  const [summary, setSummary] = useState(emptySummary);
  const [loading, setLoading] = useState(false);

  const loadSummary = async (currentFilters) => {
    try {
      setLoading(true);
      const params = buildPeriodParams(currentFilters);
      const res = await TransactionService.getSummary(params);
      setSummary(res.data || emptySummary);
    } catch (err) {
      console.error('Ошибка загрузки общего отчёта:', err);
      alert(err?.response?.data?.message || 'Не удалось построить общий отчёт');
    } finally {
      setLoading(false);
    }
  };

  const handleApply = async () => {
    await loadSummary(filters);
  };

  const handleReset = async () => {
    const cleared = { from: '', to: '' };
    setFilters(cleared);
    await loadSummary(cleared);
  };

  return (
    <div style={sectionStyle}>
      <h2
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '10px',
          marginTop: 0,
          marginBottom: '20px',
        }}
      >
        <BarChart3 color="#4f46e5" />
        Общий отчёт за период
      </h2>

      <ReportPeriodFilter
        from={filters.from}
        to={filters.to}
        onFromChange={(value) => setFilters((prev) => ({ ...prev, from: value }))}
        onToChange={(value) => setFilters((prev) => ({ ...prev, to: value }))}
        onApply={handleApply}
        onReset={handleReset}
      />

      <div style={{ marginTop: '24px' }}>
        {loading ? <div>Загрузка общего отчёта...</div> : <SummaryCards summary={summary} />}
      </div>
    </div>
  );
};

export default SummaryReportSection;