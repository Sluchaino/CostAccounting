// Путь: src/features/transactions/AddTransactionForm.jsx
import React, { useState, useEffect } from 'react';
import { CategoryService, TransactionService } from '../../api/services';

const AddTransactionForm = ({ onTransactionAdded }) => {
  const [categories, setCategories] = useState([]);
  const [formData, setFormData] = useState({
    amount: '',
    categoryId: '', // Изначально пусто
    comment: '',
    date: new Date().toISOString().split('T')[0]
  });

  useEffect(() => {
    CategoryService.getAll().then(res => {
      setCategories(res.data);
      // Если нужно, чтобы по умолчанию была выбрана первая категория:
      // if (res.data.length > 0) setFormData(prev => ({ ...prev, categoryId: res.data[0].id }));
    });
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();

    // Проверка перед отправкой
    if (!formData.categoryId) {
      alert("Ошибка: Категория не выбрана в выпадающем списке!");
      return;
    }

    const payload = {
      amount: parseFloat(formData.amount),
      categoryId: formData.categoryId,
      comment: formData.comment || "",
      date: new Date(formData.date).toISOString()
    };

    console.log("Отправляем на бэкенд:", payload);

    try {
      await TransactionService.create(payload);
      // Очищаем только сумму и комментарий, оставляя дату и категорию для удобства
      setFormData({ ...formData, amount: '', comment: '' });
      onTransactionAdded(); 
    } catch (err) {
      console.error("Ошибка API:", err.response?.data);
      alert("Ошибка при сохранении. Проверьте консоль браузера.");
    }
  };

  return (
    <form onSubmit={handleSubmit} className="grid grid-cols-1 md:grid-cols-4 gap-4 bg-indigo-50 p-6 rounded-xl mb-8">
      <input 
        type="number" required placeholder="Сумма" step="0.01"
        className="p-2 border rounded"
        value={formData.amount}
        onChange={e => setFormData({...formData, amount: e.target.value})}
      />
      
      {/* ПРОВЕРЬТЕ ЭТОТ БЛОК: value и onChange критически важны */}
      <select 
        required 
        className="p-2 border rounded"
        value={formData.categoryId} 
        onChange={e => {
          const val = e.target.value;
          console.log("Selected Category ID:", val);
          setFormData({...formData, categoryId: val});
        }}
      >
        <option value="">-- Выбрать категорию --</option>
        {categories.map(c => (
          <option key={c.id} value={c.id}>
            {c.name}
          </option>
        ))}
      </select>

      <input 
        type="text" placeholder="Комментарий"
        className="p-2 border rounded"
        value={formData.comment}
        onChange={e => setFormData({...formData, comment: e.target.value})}
      />

      <button type="submit" className="bg-indigo-600 text-white p-2 rounded font-bold hover:bg-indigo-700 transition-colors">
        Сохранить
      </button>
    </form>
  );
};

export default AddTransactionForm;