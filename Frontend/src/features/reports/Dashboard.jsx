import React, { useState, useEffect } from 'react';
import { TrendingUp, TrendingDown, CreditCard, PlusCircle, Trash2, Calendar, MessageSquare } from 'lucide-react';
import { TransactionService, CategoryService } from '../../api/services';


const Dashboard = () => {
  const [summary, setSummary] = useState({ totalIncome: 0, totalExpense: 0, balance: 0 });
  const [categories, setCategories] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);

  // Состояние для новой транзакции
  const [amount, setAmount] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [comment, setComment] = useState('');

  const loadData = async () => {
    try {
      const [summaryRes, categoriesRes, transactionsRes] = await Promise.all([
        TransactionService.getSummary(),
        CategoryService.getAll(),
        TransactionService.getAll()
      ]);
      setSummary(summaryRes.data);
      setCategories(categoriesRes.data);
      setTransactions(transactionsRes.data.sort((a, b) => new Date(b.date) - new Date(a.date))); // Сортировка: новые сверху
      setLoading(false);
    } catch (err) {
      console.error("Ошибка загрузки данных:", err);
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleAddTransaction = async (e) => {
    e.preventDefault();
    if (!amount || !categoryId) return;

    try {
      await TransactionService.create({
        amount: parseFloat(amount),
        categoryId: categoryId,
        comment: comment,
        date: new Date().toISOString()
      });
      
      setAmount('');
      setCategoryId('');
      setComment('');
      loadData();
    } catch (err) {
      alert("Ошибка при добавлении");
    }
  };

  const handleDeleteTransaction = async (id) => {
    if (window.confirm('Удалить эту операцию?')) {
      try {
        await TransactionService.delete(id);
        loadData();
      } catch (err) {
        alert("Ошибка при удалении");
      }
    }
  };

  if (loading) return <div style={{ padding: '20px' }}>Загрузка...</div>;

  const cardStyle = {
    background: 'white',
    padding: '24px',
    borderRadius: '16px',
    flex: 1,
    boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
  };

  return (
    <div style={{ maxWidth: '1200px', margin: '0 auto' }}>
      <h1 style={{ fontSize: '24px', fontWeight: '700', marginBottom: '32px' }}>Обзор финансов</h1>
      
      {/* Виджеты сводки */}
      <div style={{ display: 'flex', gap: '20px', marginBottom: '32px' }}>
        <div style={cardStyle}>
          <div style={{ color: '#6366f1', marginBottom: '8px' }}><CreditCard size={20} /></div>
          <div style={{ color: '#64748b', fontSize: '14px' }}>Баланс</div>
          <div style={{ fontSize: '24px', fontWeight: 'bold' }}>{summary.balance.toLocaleString()} ₽</div>
        </div>
        <div style={cardStyle}>
          <div style={{ color: '#22c55e', marginBottom: '8px' }}><TrendingUp size={20} /></div>
          <div style={{ color: '#64748b', fontSize: '14px' }}>Доходы</div>
          <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#15803d' }}>+{summary.totalIncome.toLocaleString()} ₽</div>
        </div>
        <div style={cardStyle}>
          <div style={{ color: '#ef4444', marginBottom: '8px' }}><TrendingDown size={20} /></div>
          <div style={{ color: '#64748b', fontSize: '14px' }}>Расходы</div>
          <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#b91c1c' }}>-{summary.totalExpense.toLocaleString()} ₽</div>
        </div>
      </div>

      {/* Форма добавления */}
      <div style={{ background: 'white', padding: '24px', borderRadius: '16px', marginBottom: '32px', boxShadow: '0 1px 3px rgba(0,0,0,0.1)' }}>
        <h3 style={{ margin: '0 0 20px 0', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <PlusCircle size={20} color="#4f46e5" /> Новая операция
        </h3>
        <form onSubmit={handleAddTransaction} style={{ display: 'grid', gridTemplateColumns: '1fr 1.5fr 1.5fr auto', gap: '15px', alignItems: 'end' }}>
          <input type="number" placeholder="Сумма" value={amount} onChange={e => setAmount(e.target.value)} required style={{ padding: '10px', borderRadius: '8px', border: '1px solid #ddd' }} />
         <select 
          value={categoryId} 
          onChange={e => {
            console.log("Выбран ID категории:", e.target.value); // Для отладки в консоли
            setCategoryId(e.target.value);
          }} 
          required 
          style={{ padding: '10px', borderRadius: '8px', border: '1px solid #ddd', width: '100%' }}
        >
          <option value="">Выберите категорию</option>
          {categories.map(cat => (
            <option key={cat.id} value={cat.id}>
              {cat.name}
            </option>
          ))}
        </select>
          <input type="text" placeholder="Комментарий" value={comment} onChange={e => setComment(e.target.value)} style={{ padding: '10px', borderRadius: '8px', border: '1px solid #ddd' }} />
          <button type="submit" style={{ background: '#4f46e5', color: 'white', border: 'none', padding: '10px 24px', borderRadius: '8px', cursor: 'pointer', fontWeight: '600' }}>Добавить</button>
        </form>
      </div>

      {/* История транзакций */}
      <div style={{ background: 'white', borderRadius: '16px', boxShadow: '0 1px 3px rgba(0,0,0,0.1)', overflow: 'hidden' }}>
        <div style={{ padding: '20px', borderBottom: '1px solid #f1f5f9', fontWeight: 'bold' }}>Последние операции</div>
        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
          <thead>
            <tr style={{ background: '#f8fafc', color: '#64748b', fontSize: '13px' }}>
              <th style={{ padding: '12px 20px' }}>Дата</th>
              <th style={{ padding: '12px 20px' }}>Категория</th>
              <th style={{ padding: '12px 20px' }}>Комментарий</th>
              <th style={{ padding: '12px 20px', textAlign: 'right' }}>Сумма</th>
              <th style={{ padding: '12px 20px', width: '50px' }}></th>
            </tr>
          </thead>
          <tbody>
            {transactions.map(t => (
  <tr key={t.id} style={{ borderBottom: '1px solid #f1f5f9', transition: 'background 0.2s' }} onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#f8fafc'} onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'transparent'}>
    <td style={{ padding: '16px 20px', fontSize: '14px', color: '#64748b' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
        <Calendar size={14} /> {new Date(t.date).toLocaleDateString()}
      </div>
    </td>
    <td style={{ padding: '16px 20px' }}>
      <span style={{ 
        padding: '4px 10px', 
        borderRadius: '20px', 
        fontSize: '12px', 
        fontWeight: '500',
        background: t.category?.type === 2 ? '#dcfce7' : '#f1f5f9',
        color: t.category?.type === 2 ? '#166534' : '#475569'
      }}>
        {t.category?.name || 'Без категории'}
      </span>
    </td>
    <td style={{ padding: '16px 20px', color: '#64748b', fontSize: '14px' }}>
      {t.comment ? (
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <MessageSquare size={14} style={{ opacity: 0.5 }} /> {t.comment}
        </div>
      ) : '—'}
    </td>
    <td style={{ padding: '16px 20px', textAlign: 'right', fontWeight: '700', fontSize: '16px', color: t.category?.type === 2 ? '#22c55e' : '#1e293b' }}>
      {t.category?.type === 2 ? '+' : '-'} {t.amount.toLocaleString()} ₽
    </td>
    <td style={{ padding: '16px 20px' }}>
      <button 
        onClick={() => handleDeleteTransaction(t.id)} 
        style={{ color: '#94a3b8', border: 'none', background: 'none', cursor: 'pointer', padding: '4px' }}
        onMouseEnter={(e) => e.currentTarget.style.color = '#ef4444'}
        onMouseLeave={(e) => e.currentTarget.style.color = '#94a3b8'}
      >
        <Trash2 size={18} />
      </button>
    </td>
  </tr>
))}
          </tbody>
        </table>
        {transactions.length === 0 && <div style={{ padding: '40px', textAlign: 'center', color: '#94a3b8' }}>Транзакций пока нет</div>}
      </div>
    </div>
  );
};

export default Dashboard;