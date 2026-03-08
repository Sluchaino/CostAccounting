import React, { useState, useEffect } from 'react';
import { Tags, Plus, Trash2 } from 'lucide-react';
import { CategoryService } from '../../api/services';

const CategoryPage = () => {
  const [categories, setCategories] = useState([]);
  const [name, setName] = useState('');
  const [type, setType] = useState(1);

  const loadData = async () => {
    const res = await CategoryService.getAll();
    setCategories(res.data);
  };

  useEffect(() => { loadData(); }, []);

  const handleAdd = async (e) => {
    e.preventDefault();
    await CategoryService.create({ name, type: Number(type) });
    setName('');
    loadData();
  };

  const handleDelete = async (id) => {
    if (window.confirm('Удалить категорию?')) {
      await CategoryService.delete(id);
      loadData();
    }
  };

  return (
    <div style={{ maxWidth: '800px' }}>
      <h1 style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
        <Tags color="#4f46e5" /> Категории
      </h1>

      <form onSubmit={handleAdd} style={{ display: 'flex', gap: '10px', margin: '20px 0', background: 'white', padding: '20px', borderRadius: '12px', boxShadow: '0 1px 3px rgba(0,0,0,0.1)' }}>
        <input 
          style={{ flex: 1, padding: '10px', borderRadius: '8px', border: '1px solid #ddd' }}
          placeholder="Название категории"
          value={name}
          onChange={e => setName(e.target.value)}
          required
        />
        <select style={{ padding: '10px', borderRadius: '8px', border: '1px solid #ddd' }} value={type} onChange={e => setType(e.target.value)}>
          <option value={1}>Расход</option>
          <option value={2}>Доход</option>
        </select>
        <button style={{ background: '#4f46e5', color: 'white', border: 'none', padding: '10px 20px', borderRadius: '8px', cursor: 'pointer' }}>
          Добавить
        </button>
      </form>

      <div style={{ display: 'grid', gap: '10px' }}>
        {categories.map(cat => (
          <div key={cat.id} style={{ background: 'white', padding: '15px', borderRadius: '12px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <span style={{ fontWeight: '600' }}>{cat.name}</span>
              <span style={{ marginLeft: '10px', fontSize: '12px', color: cat.type === 2 ? '#16a34a' : '#dc2626' }}>
                {cat.type === 2 ? 'Доход' : 'Расход'}
              </span>
            </div>
            <button onClick={() => handleDelete(cat.id)} style={{ background: 'none', border: 'none', color: '#94a3b8', cursor: 'pointer' }}>
              <Trash2 size={18} />
            </button>
          </div>
        ))}
      </div>
    </div>
  );
};

export default CategoryPage;