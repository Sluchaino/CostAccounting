import React, { useState } from 'react';
import { ListTree } from 'lucide-react';
import { TransactionService } from '../../../api/services';
import { buildPeriodParams } from '../reportUtils';
import ReportPeriodFilter from './ReportPeriodFilter';
import CategoryReportTable from './CategoryReportTable';

const sectionStyle = {
  background: 'white',
  padding: '24px',
  borderRadius: '16px',
  boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
};

const CategoryReportSection = () => {
  const [filters, setFilters] = useState({
    from: '',
    to: '',
    type: 'Expense',
  });

  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(false);

  const loadCategoryReport = async (currentFilters) => {
    try {
      setLoading(true);

      const params = {
        ...buildPeriodParams(currentFilters),
        type: currentFilters.type,
      };

      const res = await TransactionService.getByCategory(params);
      setRows(Array.isArray(res.data) ? res.data : []);
    } catch (err) {
      console.error('Ошибка загрузки отчёта по категориям:', err);
      alert(err?.response?.data?.message || 'Не удалось построить отчёт по категориям');
    } finally {
      setLoading(false);
    }
  };

  const handleApply = async () => {
    await loadCategoryReport(filters);
  };

  const handleReset = async () => {
    const cleared = { from: '', to: '', type: 'Expense' };
    setFilters(cleared);
    await loadCategoryReport(cleared);
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
        <ListTree color="#4f46e5" />
        Отчёт по категориям за период
      </h2>

      <ReportPeriodFilter
        from={filters.from}
        to={filters.to}
        onFromChange={(value) => setFilters((prev) => ({ ...prev, from: value }))}
        onToChange={(value) => setFilters((prev) => ({ ...prev, to: value }))}
        onApply={handleApply}
        onReset={handleReset}
        extra={
          <div>
            <label
              style={{
                display: 'block',
                fontSize: '12px',
                color: '#64748b',
                marginBottom: '4px',
              }}
            >
              Тип
            </label>
            <select
              value={filters.type}
              onChange={(e) => setFilters((prev) => ({ ...prev, type: e.target.value }))}
              style={{
                width: '100%',
                padding: '10px',
                borderRadius: '8px',
                border: '1px solid #d1d5db',
                background: 'white',
              }}
            >
              <option value="Expense">Расходы</option>
              <option value="Income">Доходы</option>
            </select>
          </div>
        }
      />

      <div style={{ marginTop: '24px' }}>
        {loading ? (
          <div>Загрузка отчёта по категориям...</div>
        ) : (
          <CategoryReportTable
            title={filters.type === 'Income' ? 'Доходы по категориям' : 'Расходы по категориям'}
            rows={rows}
          />
        )}
      </div>
    </div>
  );
};

export default CategoryReportSection;