import React from 'react';
import { Calendar, MessageSquare } from 'lucide-react';
import { Link } from 'react-router-dom';
import SectionCard from '../../../components/ui/SectionCard';
import CategoryTypeBadge from '../../../components/finance/CategoryTypeBadge';
import AmountValue from '../../../components/finance/AmountValue';

const RecentTransactionsCard = ({ transactions }) => {
  return (
    <SectionCard>
      <div
        style={{
          paddingBottom: '16px',
          borderBottom: '1px solid #e2e8f0',
          fontWeight: '700',
          marginBottom: '8px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
        }}
      >
        <span>Последние транзакции</span>

        <Link
          to="/transactions"
          style={{
            fontSize: '14px',
            color: '#4f46e5',
            textDecoration: 'none',
            fontWeight: '600',
          }}
        >
          Все транзакции
        </Link>
      </div>

      {transactions.length === 0 ? (
        <div style={{ padding: '24px 0', color: '#64748b' }}>
          Транзакций пока нет
        </div>
      ) : (
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ background: '#f8fafc', textAlign: 'left' }}>
              <th style={{ padding: '12px 14px' }}>Дата</th>
              <th style={{ padding: '12px 14px' }}>Категория</th>
              <th style={{ padding: '12px 14px' }}>Комментарий</th>
              <th style={{ padding: '12px 14px', textAlign: 'right' }}>Сумма</th>
            </tr>
          </thead>
          <tbody>
            {transactions.map((t) => (
              <tr key={t.id} style={{ borderTop: '1px solid #e2e8f0' }}>
                <td style={{ padding: '14px' }}>
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '8px',
                      color: '#475569',
                    }}
                  >
                    <Calendar size={14} />
                    {new Date(t.date).toLocaleString('ru-RU')}
                  </div>
                </td>

                <td style={{ padding: '14px' }}>
                  <CategoryTypeBadge type={t.categoryType}>
                    {t.categoryName}
                  </CategoryTypeBadge>
                </td>

                <td style={{ padding: '14px', color: '#475569' }}>
                  {t.comment ? (
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <MessageSquare size={14} />
                      {t.comment}
                    </div>
                  ) : (
                    '—'
                  )}
                </td>

                <td style={{ padding: '14px' }}>
                  <AmountValue amount={t.amount} type={t.categoryType} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </SectionCard>
  );
};

export default RecentTransactionsCard;