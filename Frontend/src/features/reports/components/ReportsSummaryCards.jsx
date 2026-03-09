import React from 'react';
import { Wallet, TrendingUp, TrendingDown } from 'lucide-react';
import StatCard from '../../../components/ui/StatCard';

const ReportsSummaryCards = ({ summary }) => {
  return (
    <div
      style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(3, minmax(0, 1fr))',
        gap: '16px',
      }}
    >
      <StatCard
        icon={<TrendingUp size={20} />}
        title="Общий доход"
        value={`${Number(summary.totalIncome || 0).toLocaleString('ru-RU')} ₽`}
        color="#15803d"
        borderColor="#22c55e"
      />

      <StatCard
        icon={<TrendingDown size={20} />}
        title="Общий расход"
        value={`${Number(summary.totalExpense || 0).toLocaleString('ru-RU')} ₽`}
        color="#b91c1c"
        borderColor="#ef4444"
      />

      <StatCard
        icon={<Wallet size={20} />}
        title="Баланс"
        value={`${Number(summary.balance || 0).toLocaleString('ru-RU')} ₽`}
        color="#4f46e5"
      />
    </div>
  );
};

export default ReportsSummaryCards;