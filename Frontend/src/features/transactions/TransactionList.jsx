import React from 'react';
import { useTransactions } from '../../hooks/useTransactions'; // Наш хук из прошлого шага
import { formatCurrency, formatDate } from '../../utils/formatters';

const TransactionList = () => {
  const { transactions, loading } = useTransactions();

  if (loading) return <p>Загрузка данных...</p>;

  return (
    <div className="bg-white rounded-lg shadow overflow-hidden">
      <table className="w-full text-left">
        <thead className="bg-gray-100 border-b">
          <tr>
            <th className="p-4">Дата</th>
            <th className="p-4">Категория</th>
            <th className="p-4">Комментарий</th>
            <th className="p-4 text-right">Сумма</th>
          </tr>
        </thead>
        <tbody>
          {transactions.map((t) => (
            <tr key={t.id} className="border-b hover:bg-gray-50">
              <td className="p-4">{formatDate(t.date)}</td>
              <td className="p-4">
                <span className="px-2 py-1 rounded text-xs bg-blue-100 text-blue-700">
                  {t.category?.name || 'Без категории'}
                </span>
              </td>
              <td className="p-4 text-gray-500 text-sm">{t.comment || '—'}</td>
              <td className={`p-4 text-right font-semibold ${t.category?.type === 1 ? 'text-red-500' : 'text-green-500'}`}>
                {t.category?.type === 1 ? '-' : '+'} {formatCurrency(t.amount)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};