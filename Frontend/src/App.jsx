import React from 'react';
import { BrowserRouter as Router, Routes, Route, Link, useLocation } from 'react-router-dom';
import { LayoutDashboard, Tags, WalletCards, Receipt, BarChart3 } from 'lucide-react';

import DashboardPage from './features/dashboard/DashboardPage';
import CategoryPage from './features/categories/CategoryPage';
import TransactionsPage from './features/transactions/TransactionsPage';
import ReportsPage from './features/reports/ReportsPage';

const NavLink = ({ to, icon: Icon, children }) => {
  const location = useLocation();
  const active = location.pathname === to;

  return (
    <Link
      to={to}
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: '12px',
        color: 'white',
        textDecoration: 'none',
        padding: '12px',
        borderRadius: '8px',
        marginBottom: '8px',
        background: active ? '#312e81' : 'transparent',
        transition: 'background 0.2s',
      }}
    >
      <Icon size={20} />
      <span>{children}</span>
    </Link>
  );
};

function Layout() {
  return (
    <div style={{ display: 'flex', minHeight: '100vh', backgroundColor: '#f8fafc' }}>
      <nav
        style={{
          width: '260px',
          background: '#1e1b4b',
          color: 'white',
          padding: '24px',
          position: 'fixed',
          height: '100vh',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '40px' }}>
          <div style={{ background: '#4f46e5', padding: '8px', borderRadius: '10px' }}>
            <WalletCards size={24} />
          </div>
          <h2 style={{ fontSize: '20px', fontWeight: 'bold', margin: 0 }}>CashFlow</h2>
        </div>

        <NavLink to="/" icon={LayoutDashboard}>Обзор</NavLink>
        <NavLink to="/categories" icon={Tags}>Категории</NavLink>
        <NavLink to="/transactions" icon={Receipt}>Транзакции</NavLink>
        <NavLink to="/reports" icon={BarChart3}>Отчёты</NavLink>
      </nav>

      <main style={{ flex: 1, padding: '40px', marginLeft: '260px' }}>
        <Routes>
          <Route path="/" element={<DashboardPage  />} />
          <Route path="/categories" element={<CategoryPage />} />
          <Route path="/transactions" element={<TransactionsPage />} />
          <Route path="/reports" element={<ReportsPage />} />
        </Routes>
      </main>
    </div>
  );
}

function App() {
  return (
    <Router>
      <Layout />
    </Router>
  );
}

export default App;