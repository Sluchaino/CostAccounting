import React from 'react';

const ReportPeriodFilter = ({
  from,
  to,
  onFromChange,
  onToChange,
  onApply,
  onReset,
  extra,
}) => {
  return (
    <div
      style={{
        display: 'grid',
        gridTemplateColumns: '1fr 1fr auto auto',
        gap: '12px',
        alignItems: 'end',
      }}
    >
      <div>
        <label
          style={{
            display: 'block',
            fontSize: '12px',
            color: '#64748b',
            marginBottom: '4px',
          }}
        >
          С
        </label>
        <input
          type="date"
          value={from}
          onChange={(e) => onFromChange(e.target.value)}
          style={{
            width: '100%',
            padding: '10px',
            borderRadius: '8px',
            border: '1px solid #d1d5db',
          }}
        />
      </div>

      <div>
        <label
          style={{
            display: 'block',
            fontSize: '12px',
            color: '#64748b',
            marginBottom: '4px',
          }}
        >
          По
        </label>
        <input
          type="date"
          value={to}
          onChange={(e) => onToChange(e.target.value)}
          style={{
            width: '100%',
            padding: '10px',
            borderRadius: '8px',
            border: '1px solid #d1d5db',
          }}
        />
      </div>

      {extra || <div />}

      <div style={{ display: 'flex', gap: '10px' }}>
        <button
          onClick={onApply}
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
          onClick={onReset}
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
    </div>
  );
};

export default ReportPeriodFilter;