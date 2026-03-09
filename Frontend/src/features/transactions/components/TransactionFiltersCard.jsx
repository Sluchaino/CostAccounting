import React from 'react';
import SectionCard from '../../../components/ui/SectionCard';

const TransactionFiltersCard = ({
  filters,
  setFilters,
  categories,
  onApply,
  onClear,
}) => {
  return (
    <SectionCard style={{ marginBottom: '24px' }}>
      <h3 style={{ marginTop: 0 }}>Фильтры</h3>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: '1fr 1fr 1fr 1fr auto auto',
          gap: '10px',
        }}
      >
        <input
          type="date"
          value={filters.from}
          onChange={(e) => setFilters((prev) => ({ ...prev, from: e.target.value }))}
          style={{ padding: '10px', borderRadius: '8px', border: '1px solid #ddd' }}
        />

        <input
          type="date"
          value={filters.to}
          onChange={(e) => setFilters((prev) => ({ ...prev, to: e.target.value }))}
          style={{ padding: '10px', borderRadius: '8px', border: '1px solid #ddd' }}
        />

        <select
          value={filters.categoryId}
          onChange={(e) => setFilters((prev) => ({ ...prev, categoryId: e.target.value }))}
          style={{ padding: '10px', borderRadius: '8px', border: '1px solid #ddd' }}
        >
          <option value="">Все категории</option>
          {categories.map((cat) => (
            <option key={cat.id} value={cat.id}>
              {cat.name}
            </option>
          ))}
        </select>

        <select
          value={filters.type}
          onChange={(e) => setFilters((prev) => ({ ...prev, type: e.target.value }))}
          style={{ padding: '10px', borderRadius: '8px', border: '1px solid #ddd' }}
        >
          <option value="">Все типы</option>
          <option value="Expense">Расход</option>
          <option value="Income">Доход</option>
        </select>

        <button
          onClick={onApply}
          style={{
            background: '#4f46e5',
            color: 'white',
            border: 'none',
            padding: '10px 16px',
            borderRadius: '8px',
            cursor: 'pointer',
          }}
        >
          Применить
        </button>

        <button
          onClick={onClear}
          style={{
            background: '#e2e8f0',
            color: '#0f172a',
            border: 'none',
            padding: '10px 16px',
            borderRadius: '8px',
            cursor: 'pointer',
          }}
        >
          Сбросить
        </button>
      </div>
    </SectionCard>
  );
};

export default TransactionFiltersCard;