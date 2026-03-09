import React, { useEffect, useMemo, useState } from 'react';
import { Tags } from 'lucide-react';
import { categoryService } from './categoryService';
import CategoryFormCard from './components/CategoryFormCard';
import CategoryAccordionCard from './components/CategoryAccordionCard';

const emptyForm = {
  name: '',
  type: 'Expense',
};

const CategoryPage = () => {
  const [categories, setCategories] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);

  const [openSections, setOpenSections] = useState({
    Expense: true,
    Income: false,
  });

  const loadData = async () => {
    try {
      const res = await categoryService.getAll();
      setCategories(Array.isArray(res.data) ? res.data : []);
    } catch (err) {
      console.error('Ошибка загрузки категорий:', err);
      alert(err?.response?.data?.message || 'Не удалось загрузить категории');
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const expenseCategories = useMemo(() => {
    return categories.filter((cat) => cat.type === 'Expense');
  }, [categories]);

  const incomeCategories = useMemo(() => {
    return categories.filter((cat) => cat.type === 'Income');
  }, [categories]);

  const resetForm = () => {
    setForm(emptyForm);
    setEditingId(null);
  };

  const toggleSection = (type) => {
    setOpenSections((prev) => ({
      ...prev,
      [type]: !prev[type],
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    try {
      const payload = {
        name: form.name.trim(),
        type: form.type,
      };

      if (editingId) {
        await categoryService.update(editingId, payload);
      } else {
        await categoryService.create(payload);
      }

      resetForm();
      await loadData();
    } catch (err) {
      console.error('Ошибка сохранения категории:', err);
      alert(err?.response?.data?.message || 'Не удалось сохранить категорию');
    }
  };

  const handleEdit = (cat) => {
    setEditingId(cat.id);
    setForm({
      name: cat.name,
      type: cat.type,
    });

    setOpenSections((prev) => ({
      ...prev,
      [cat.type]: true,
    }));
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Удалить категорию?')) return;

    try {
      await categoryService.delete(id);

      if (editingId === id) {
        resetForm();
      }

      await loadData();
    } catch (err) {
      console.error('Ошибка удаления категории:', err);
      alert(err?.response?.data?.message || 'Не удалось удалить категорию');
    }
  };

  return (
    <div style={{ maxWidth: '900px' }}>
      <h1 style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
        <Tags color="#4f46e5" />
        Категории
      </h1>

      <CategoryFormCard
        form={form}
        setForm={setForm}
        editingId={editingId}
        onSubmit={handleSubmit}
        onCancel={resetForm}
      />

      <div style={{ display: 'grid', gap: '16px' }}>
        <CategoryAccordionCard
          title="Расходы"
          type="Expense"
          items={expenseCategories}
          isOpen={openSections.Expense}
          onToggle={() => toggleSection('Expense')}
          onEdit={handleEdit}
          onDelete={handleDelete}
        />

        <CategoryAccordionCard
          title="Доходы"
          type="Income"
          items={incomeCategories}
          isOpen={openSections.Income}
          onToggle={() => toggleSection('Income')}
          onEdit={handleEdit}
          onDelete={handleDelete}
        />
      </div>
    </div>
  );
};

export default CategoryPage;