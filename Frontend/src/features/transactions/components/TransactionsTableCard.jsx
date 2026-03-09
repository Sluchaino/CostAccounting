import React from 'react';
import { Pencil, Trash2 } from 'lucide-react';
import SectionCard from '../../../components/ui/SectionCard';
import CategoryTypeBadge from '../../../components/finance/CategoryTypeBadge';
import AmountValue from '../../../components/finance/AmountValue';

const TransactionsTableCard = ({
  transactions,
  onEdit,
  onDelete,
}) => {
  return (
    <SectionCard>
      <h3 style={{ marginTop: 0, marginBottom: '16px' }}>Список транзакций</h3>

      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr style={{ background: '#f8fafc', textAlign: 'left' }}>
            <th style={{ padding: '12px 16px' }}>Дата</th>
            <th style={{ padding: '12px 16px' }}>Категория</th>
            <th style={{ padding: '12px 16px' }}>Комментарий</th>
            <th style={{ padding: '12px 16px', textAlign: 'right' }}>Сумма</th>
            <th style={{ padding: '12px 16px' }}></th>
          </tr>
        </thead>
        <tbody>
          {transactions.map((tx) => (
            <tr key={tx.id} style={{ borderTop: '1px solid #e2e8f0' }}>
              <td style={{ padding: '12px 16px' }}>
                {new Date(tx.date).toLocaleString('ru-RU')}
              </td>

              <td style={{ padding: '12px 16px' }}>
                <CategoryTypeBadge type={tx.categoryType}>
                  {tx.categoryName}
                </CategoryTypeBadge>
              </td>

              <td style={{ padding: '12px 16px' }}>
                {tx.comment || '—'}
              </td>

              <td style={{ padding: '12px 16px' }}>
                <AmountValue amount={tx.amount} type={tx.categoryType} />
              </td>

              <td style={{ padding: '12px 16px' }}>
                <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
                  <button
                    onClick={() => onEdit(tx)}
                    style={{ background: 'none', border: 'none', color: '#4f46e5', cursor: 'pointer' }}
                  >
                    <Pencil size={18} />
                  </button>

                  <button
                    onClick={() => onDelete(tx.id)}
                    style={{ background: 'none', border: 'none', color: '#94a3b8', cursor: 'pointer' }}
                  >
                    <Trash2 size={18} />
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {transactions.length === 0 && (
        <div style={{ padding: '40px', textAlign: 'center', color: '#64748b' }}>
          Транзакции не найдены
        </div>
      )}
    </SectionCard>
  );
};

export default TransactionsTableCard;