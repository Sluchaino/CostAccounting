import React from 'react';

const CategoryReportTable = ({ title, rows }) => {
  return (
    <div
      style={{
        background: 'white',
        padding: '24px',
        borderRadius: '16px',
        boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
      }}
    >
      <h3 style={{ margin: '0 0 16px 0' }}>{title}</h3>

      {rows.length === 0 ? (
        <div style={{ color: '#64748b' }}>Нет данных</div>
      ) : (
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ background: '#f8fafc', textAlign: 'left' }}>
              <th style={{ padding: '12px 14px' }}>Категория</th>
              <th style={{ padding: '12px 14px' }}>Тип</th>
              <th style={{ padding: '12px 14px', textAlign: 'right' }}>Сумма</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((item) => (
              <tr key={item.categoryId} style={{ borderTop: '1px solid #e2e8f0' }}>
                <td style={{ padding: '12px 14px' }}>{item.categoryName}</td>
                <td style={{ padding: '12px 14px' }}>
                  {item.categoryType === 'Income' ? 'Доход' : 'Расход'}
                </td>
                <td style={{ padding: '12px 14px', textAlign: 'right', fontWeight: '700' }}>
                  {Number(item.total || 0).toLocaleString('ru-RU')} ₽
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
};

export default CategoryReportTable;