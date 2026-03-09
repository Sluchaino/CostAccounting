import React from 'react';

const CategoryTypeBadge = ({ type, children }) => {
  const isIncome = type === 'Income';

  return (
    <span
      style={{
        padding: '4px 10px',
        borderRadius: '999px',
        fontSize: '12px',
        fontWeight: '600',
        background: isIncome ? '#dcfce7' : '#fee2e2',
        color: isIncome ? '#166534' : '#991b1b',
      }}
    >
      {children}
    </span>
  );
};

export default CategoryTypeBadge;