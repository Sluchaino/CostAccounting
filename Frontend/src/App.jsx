import React from 'react';
import { BrowserRouter as Router, Routes, Route, Link, useLocation } from 'react-router-dom';
import { LayoutDashboard, Tags, WalletCards } from 'lucide-react';
import Dashboard from './features/reports/Dashboard';
import CategoryPage from './features/categories/CategoryPage';

// Компонент для красивых ссылок в меню
const NavLink = ({ to, icon: Icon, children }) => {
  return (
    <Link to={to} style={{
      display: 'flex',
      alignItems: 'center',
      gap: '12px',
      color: 'white',
      textDecoration: 'none',
      padding: '12px',
      borderRadius: '8px',
      marginBottom: '8px',
      transition: 'background 0.2s'
    }} 
    onMouseEnter={(e) => e.target.style.background = '#312e81'}
    onMouseLeave={(e) => e.target.style.background = 'transparent'}>
      <Icon size={20} />
      <span>{children}</span>
    </Link>
  );
};

function App() {
  return (
    <Router>
      <div style={{ display: 'flex', minHeight: '100vh', backgroundColor: '#f8fafc' }}>
        {/* Sidebar */}
        <nav style={{ width: '260px', background: '#1e1b4b', color: 'white', padding: '24px', position: 'fixed', height: '100vh' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '40px' }}>
            <div style={{ background: '#4f46e5', padding: '8px', borderRadius: '10px' }}>
              <WalletCards size={24} />
            </div>
            <h2 style={{ fontSize: '20px', fontWeight: 'bold', margin: 0 }}>CashFlow</h2>
          </div>
          
          <NavLink to="/" icon={LayoutDashboard}>Обзор</NavLink>
          <NavLink to="/categories" icon={Tags}>Категории</NavLink>
        </nav>

        {/* Контент */}
        <main style={{ flex: 1, padding: '40px', marginLeft: '260px' }}>
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/categories" element={<CategoryPage />} />
          </Routes>
        </main>
      </div>
    </Router>
  );
}

export default App;