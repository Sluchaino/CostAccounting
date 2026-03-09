import React from 'react';
import { CreditCard, TrendingUp, TrendingDown } from 'lucide-react';
import StatCard from '../../../components/ui/StatCard';

const DashboardSummaryCards = ({ summary }) => {
  return (
    <div
      style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(3, minmax(0, 1fr))',
        gap: '16px',
        marginBottom: '24px',
      }}
    >
      <StatCard
        icon={<CreditCard size={20} />}
        title="Текущий баланс"
        value={`${Number(summary.balance || 0).toLocaleString('ru-RU')} ₽`}
        color="#202020"
      />

      <StatCard
        icon={<TrendingUp size={20} />}
        title="Общий доход"
        value={`+ ${Number(summary.totalIncome || 0).toLocaleString('ru-RU')} ₽`}
        color="#15803d"
        borderColor="#22c55e"
      />

      <StatCard
        icon={<TrendingDown size={20} />}
        title="Общий расход"
        value={`- ${Number(summary.totalExpense || 0).toLocaleString('ru-RU')} ₽`}
        color="#b91c1c"
        borderColor="#ef4444"
      />
    </div>
  );
};

export default DashboardSummaryCards;