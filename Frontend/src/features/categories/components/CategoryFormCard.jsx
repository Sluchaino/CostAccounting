import React from 'react';

const CategoryFormCard = ({ form, setForm, editingId, onSubmit, onCancel }) => {
  return (
    <form
      onSubmit={onSubmit}
      style={{
        display: 'flex',
        gap: '10px',
        margin: '20px 0 24px 0',
        background: 'white',
        padding: '20px',
        borderRadius: '12px',
        boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
      }}
    >
      <input
        style={{
          flex: 1,
          padding: '10px',
          borderRadius: '8px',
          border: '1px solid #ddd',
        }}
        placeholder="Название категории"
        value={form.name}
        onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
        required
      />

      <select
        style={{
          padding: '10px',
          borderRadius: '8px',
          border: '1px solid #ddd',
          background: 'white',
        }}
        value={form.type}
        onChange={(e) => setForm((prev) => ({ ...prev, type: e.target.value }))}
      >
        <option value="Expense">Расход</option>
        <option value="Income">Доход</option>
      </select>

      <button
        type="submit"
        style={{
          background: '#4f46e5',
          color: 'white',
          border: 'none',
          padding: '10px 20px',
          borderRadius: '8px',
          cursor: 'pointer',
        }}
      >
        {editingId ? 'Сохранить' : 'Добавить'}
      </button>

      {editingId && (
        <button
          type="button"
          onClick={onCancel}
          style={{
            background: '#e2e8f0',
            color: '#0f172a',
            border: 'none',
            padding: '10px 16px',
            borderRadius: '8px',
            cursor: 'pointer',
          }}
        >
          Отмена
        </button>
      )}
    </form>
  );
};

export default CategoryFormCard;