import React from 'react';

const StatCard = ({ icon, title, value, color, borderColor }) => {
  return (
    <div
      style={{
        background: 'white',
        padding: '24px',
        borderRadius: '16px',
        boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
        borderLeft: borderColor ? `4px solid ${borderColor}` : 'none',
      }}
    >
      <div style={{ color: color || '#6366f1', marginBottom: '10px' }}>
        {icon}
      </div>

      <div style={{ color: '#64748b', fontSize: '14px', marginBottom: '6px' }}>
        {title}
      </div>

      <div style={{ fontSize: '28px', fontWeight: '700', color: color || '#0f172a' }}>
        {value}
      </div>
    </div>
  );
};

export default StatCard;