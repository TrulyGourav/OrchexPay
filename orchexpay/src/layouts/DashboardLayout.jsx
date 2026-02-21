import React, { useState } from 'react';
import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import styles from './DashboardLayout.module.css';

const ADMIN_LINKS = [
  { to: '/admin', end: true, label: 'Dashboard' },
  { to: '/admin/merchants', label: 'Merchants' },
  { to: '/admin/wallets', label: 'Wallet search' },
  { to: '/admin/transactions', label: 'Transactions' },
  { to: '/admin/freeze', label: 'Freeze wallet' },
  { to: '/admin/settlement', label: 'Settlement' },
];

const MERCHANT_LINKS = [
  { to: '/merchant', end: true, label: 'Dashboard' },
  { to: '/merchant/vendors', label: 'Vendors' },
  { to: '/merchant/vendor-wallets', label: 'Vendor wallets' },
  { to: '/merchant/order-settlement', label: 'Order settlement' },
  { to: '/merchant/commission', label: 'Commission' },
  { to: '/merchant/escrow', label: 'Escrow' },
  { to: '/merchant/transactions', label: 'Transactions' },
  { to: '/merchant/order-complete', label: 'Complete order' },
  { to: '/merchant/payments', label: 'Payments' },
];

const VENDOR_LINKS = [
  { to: '/vendor', end: true, label: 'Dashboard' },
  { to: '/vendor/transactions', label: 'Transactions' },
  { to: '/vendor/payout', label: 'Request payout' },
  { to: '/vendor/payout-status', label: 'Payout status' },
  { to: '/vendor/bank', label: 'Bank details' },
];

function NavLinks({ links, base }) {
  return (
    <nav className={styles.nav}>
      {links.map(({ to, end, label }) => (
        <NavLink
          key={to}
          to={to}
          end={end}
          className={({ isActive }) => (isActive ? styles.linkActive : styles.link)}
        >
          {label}
        </NavLink>
      ))}
    </nav>
  );
}

export default function DashboardLayout({ role }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);
  const links = role === 'ADMIN' ? ADMIN_LINKS : role === 'MERCHANT' ? MERCHANT_LINKS : VENDOR_LINKS;

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className={styles.layout}>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <button
            type="button"
            className={styles.menuBtn}
            onClick={() => setMenuOpen((o) => !o)}
            aria-label="Menu"
          >
            ≡
          </button>
          <span className={styles.logo}>LedgerX</span>
          <span className={styles.user}>
            {user?.username} <span className={styles.role}>({role})</span>
          </span>
          <button type="button" className={styles.logoutBtn} onClick={handleLogout}>
            Logout
          </button>
        </div>
      </header>
      <aside className={`${styles.sidebar} ${menuOpen ? styles.sidebarOpen : ''}`}>
        <NavLinks links={links} />
      </aside>
      {menuOpen && (
        <div className={styles.overlay} onClick={() => setMenuOpen(false)} aria-hidden />
      )}
      <main className={styles.content}>
        <Outlet />
      </main>
    </div>
  );
}
