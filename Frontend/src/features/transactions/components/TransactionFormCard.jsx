import React from 'react';
import SectionCard from '../../../components/ui/SectionCard';

const TransactionFormCard = ({
  form,
  setForm,
  categories,
  editingId,
  onSubmit,
  onCancel,
}) => {
  return (
    <SectionCard style={{ marginBottom: '24px' }}>
      <h3 style={{ marginTop: 0 }}>
        {editingId ? 'Редактировать транзакцию' : 'Новая транзакция'}
      </h3>

      <form
        onSubmit={onSubmit}
        style={{
          display: 'grid',
          gridTemplateColumns: '1.2fr 1fr 1.2fr 1.5fr auto auto',
          gap: '10px',
          alignItems: 'end',
        }}
      >
        <div>
          <label style={{ display: 'block', fontSize: '12px', marginBottom: '4px' }}>
            Дата
          </label>
          <input
            type="datetime-local"
            value={form.date}
            onChange={(e) => setForm((prev) => ({ ...prev, date: e.target.value }))}
            required
            style={{ width: '100%', padding: '10px', borderRadius: '8px', border: '1px solid #ddd' }}
          />
        </div>

        <div>
          <label style={{ display: 'block', fontSize: '12px', marginBottom: '4px' }}>
            Сумма
          </label>
          <input
            type="number"
            step="0.01"
            value={form.amount}
            onChange={(e) => setForm((prev) => ({ ...prev, amount: e.target.value }))}
            required
            style={{ width: '100%', padding: '10px', borderRadius: '8px', border: '1px solid #ddd' }}
          />
        </div>

        <div>
          <label style={{ display: 'block', fontSize: '12px', marginBottom: '4px' }}>
            Категория
          </label>
          <select
            value={form.categoryId}
            onChange={(e) => setForm((prev) => ({ ...prev, categoryId: e.target.value }))}
            required
            style={{ width: '100%', padding: '10px', borderRadius: '8px', border: '1px solid #ddd' }}
          >
            <option value="">Выберите категорию</option>
            {categories.map((cat) => (
              <option key={cat.id} value={cat.id}>
                {cat.name} ({cat.type === 'Income' ? 'Доход' : 'Расход'})
              </option>
            ))}
          </select>
        </div>

        <div>
          <label style={{ display: 'block', fontSize: '12px', marginBottom: '4px' }}>
            Комментарий
          </label>
          <input
            type="text"
            value={form.comment}
            onChange={(e) => setForm((prev) => ({ ...prev, comment: e.target.value }))}
            style={{ width: '100%', padding: '10px', borderRadius: '8px', border: '1px solid #ddd' }}
          />
        </div>

        <button
          type="submit"
          style={{
            background: '#4f46e5',
            color: 'white',
            border: 'none',
            padding: '10px 16px',
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
    </SectionCard>
  );
};

export default TransactionFormCard;