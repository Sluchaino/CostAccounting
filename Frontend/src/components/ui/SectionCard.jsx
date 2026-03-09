import React from 'react';

const SectionCard = ({ children, style = {} }) => {
  return (
    <div
      style={{
        background: 'white',
        padding: '24px',
        borderRadius: '16px',
        boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
        ...style,
      }}
    >
      {children}
    </div>
  );
};

export default SectionCard;