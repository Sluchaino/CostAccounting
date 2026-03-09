import React from 'react';

const AmountValue = ({ amount, type, align = 'right' }) => {
  const isIncome = type === 'Income';

  return (
    <div
      style={{
        textAlign: align,
        fontWeight: '700',
        color: isIncome ? '#15803d' : '#b91c1c',
      }}
    >
      {isIncome ? '+' : '-'} {Number(amount || 0).toLocaleString('ru-RU')} ₽
    </div>
  );
};

export default AmountValue;