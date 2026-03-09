import React from 'react';
import { BarChart3 } from 'lucide-react';
import SectionCard from '../../../components/ui/SectionCard';

const CategoryTotalsCard = ({ title, color, rows }) => {
  return (
    <SectionCard>
      <h3
        style={{
          margin: '0 0 16px 0',
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
        }}
      >
        <BarChart3 size={18} color={color} />
        {title}
      </h3>

      {rows.length === 0 ? (
        <div style={{ color: '#64748b' }}>Нет данных</div>
      ) : (
        <div style={{ display: 'grid', gap: '10px' }}>
          {rows.map((item) => (
            <div
              key={item.categoryId}
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                padding: '10px 12px',
                borderRadius: '10px',
                background: '#f8fafc',
              }}
            >
              <span>{item.categoryName}</span>
              <strong>{Number(item.total || 0).toLocaleString('ru-RU')} ₽</strong>
            </div>
          ))}
        </div>
      )}
    </SectionCard>
  );
};

export default CategoryTotalsCard;