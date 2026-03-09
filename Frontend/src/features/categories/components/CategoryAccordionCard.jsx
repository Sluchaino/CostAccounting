import React from 'react';
import { ChevronDown, ChevronRight, Pencil, Trash2 } from 'lucide-react';

const CategoryAccordionCard = ({
  title,
  type,
  items,
  isOpen,
  onToggle,
  onEdit,
  onDelete,
}) => {
  const isIncome = type === 'Income';

  return (
    <div
      style={{
        background: 'white',
        borderRadius: '12px',
        boxShadow: '0 1px 2px rgba(0,0,0,0.05)',
        overflow: 'hidden',
      }}
    >
      <button
        type="button"
        onClick={onToggle}
        style={{
          width: '100%',
          border: 'none',
          background: 'white',
          padding: '16px 18px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          cursor: 'pointer',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          {isOpen ? <ChevronDown size={18} /> : <ChevronRight size={18} />}

          <span style={{ fontWeight: '700', fontSize: '16px' }}>{title}</span>

          <span
            style={{
              fontSize: '12px',
              padding: '3px 8px',
              borderRadius: '999px',
              background: isIncome ? '#dcfce7' : '#fee2e2',
              color: isIncome ? '#166534' : '#991b1b',
              fontWeight: '600',
            }}
          >
            {items.length}
          </span>
        </div>
      </button>

      {isOpen && (
        <div style={{ borderTop: '1px solid #eef2f7' }}>
          {items.length === 0 ? (
            <div style={{ padding: '18px', color: '#64748b' }}>
              Категорий пока нет
            </div>
          ) : (
            items.map((cat, index) => (
              <div
                key={cat.id}
                style={{
                  padding: '14px 18px',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  borderTop: index === 0 ? 'none' : '1px solid #f1f5f9',
                }}
              >
                <div>
                  <span style={{ fontWeight: '600' }}>{cat.name}</span>
                </div>

                <div style={{ display: 'flex', gap: '8px' }}>
                  <button
                    type="button"
                    onClick={() => onEdit(cat)}
                    style={{
                      background: 'none',
                      border: 'none',
                      color: '#4f46e5',
                      cursor: 'pointer',
                      padding: '5px',
                    }}
                  >
                    <Pencil size={18} />
                  </button>

                  <button
                    type="button"
                    onClick={() => onDelete(cat.id)}
                    style={{
                      background: 'none',
                      border: 'none',
                      color: '#94a3b8',
                      cursor: 'pointer',
                      padding: '5px',
                    }}
                  >
                    <Trash2 size={18} />
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
};

export default CategoryAccordionCard;