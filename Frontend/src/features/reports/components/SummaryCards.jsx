import React from 'react';
import { TrendingUp, TrendingDown, Wallet } from 'lucide-react';

const cardStyle = {
  background: 'white',
  padding: '24px',
  borderRadius: '16px',
  boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
};

const SummaryCards = ({ summary }) => {
  return (
    <div
      style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(3, minmax(0, 1fr))',
        gap: '16px',
      }}
    >
      <div style={cardStyle}>
        <div style={{ color: '#22c55e', marginBottom: '10px' }}>
          <TrendingUp size={20} />
        </div>
        <div style={{ fontSize: '14px', color: '#64748b', marginBottom: '6px' }}>
          Общий доход
        </div>
        <div style={{ fontSize: '28px', fontWeight: '700', color: '#15803d' }}>
          {Number(summary.totalIncome || 0).toLocaleString('ru-RU')} ₽
        </div>
      </div>

      <div style={cardStyle}>
        <div style={{ color: '#ef4444', marginBottom: '10px' }}>
          <TrendingDown size={20} />
        </div>
        <div style={{ fontSize: '14px', color: '#64748b', marginBottom: '6px' }}>
          Общий расход
        </div>
        <div style={{ fontSize: '28px', fontWeight: '700', color: '#b91c1c' }}>
          {Number(summary.totalExpense || 0).toLocaleString('ru-RU')} ₽
        </div>
      </div>

      <div style={cardStyle}>
        <div style={{ color: '#4f46e5', marginBottom: '10px' }}>
          <Wallet size={20} />
        </div>
        <div style={{ fontSize: '14px', color: '#64748b', marginBottom: '6px' }}>
          Баланс
        </div>
        <div style={{ fontSize: '28px', fontWeight: '700' }}>
          {Number(summary.balance || 0).toLocaleString('ru-RU')} ₽
        </div>
      </div>
    </div>
  );
};

export default SummaryCards;