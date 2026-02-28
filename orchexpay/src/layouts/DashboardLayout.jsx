import React, { useState, useEffect } from 'react';
import { Outlet, NavLink, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { payoutApi } from '../api/payout';
import styles from './DashboardLayout.module.css';

const ADMIN_LINKS = [
  { to: '/admin', end: true, label: 'Dashboard' },
  { to: '/admin/merchants', label: 'Merchants' },
  { to: '/admin/wallets', label: 'Wallet search' },
  { to: '/admin/transactions', label: 'Transactions' },
  { to: '/admin/payouts', label: 'Payout management' },
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
  { to: '/merchant/payouts', label: 'Vendor payouts', badgeKey: 'payouts' },
  { to: '/merchant/payments', label: 'Payments' },
];

const VENDOR_LINKS = [
  { to: '/vendor', end: true, label: 'Dashboard' },
  { to: '/vendor/transactions', label: 'Transactions' },
  { to: '/vendor/payout', label: 'Request payout' },
  { to: '/vendor/payout-status', label: 'Payout status' },
  { to: '/vendor/bank', label: 'Bank details' },
];

function NavLinks({ links }) {
  return (
    <nav className={styles.nav}>
      {links.map(({ to, end, label, badge }) => (
        <NavLink
          key={to}
          to={to}
          end={end}
          className={({ isActive }) => (isActive ? styles.linkActive : styles.link)}
        >
          {label}
          {badge != null && badge > 0 && (
            <span className={styles.navBadge} aria-label={`${badge} pending`}>{badge > 99 ? '99+' : badge}</span>
          )}
        </NavLink>
      ))}
    </nav>
  );
}

export default function DashboardLayout({ role }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [menuOpen, setMenuOpen] = useState(false);
  const [processingCount, setProcessingCount] = useState(0);
  const [showAck, setShowAck] = useState(true);

  const baseLinks = role === 'ADMIN' ? ADMIN_LINKS : role === 'MERCHANT' ? MERCHANT_LINKS : VENDOR_LINKS;
  const links = role === 'MERCHANT'
    ? baseLinks.map((link) =>
        link.badgeKey === 'payouts'
          ? { ...link, badge: processingCount }
          : { ...link, badge: undefined }
      )
    : baseLinks.map((link) => ({ ...link, badge: undefined }));

  useEffect(() => {
    if (role !== 'MERCHANT') return;
    payoutApi
      .getMerchantProcessingCount()
      .then((res) => setProcessingCount(res.data?.processingCount ?? 0))
      .catch(() => setProcessingCount(0));
  }, [role, location.pathname]);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const handleAck = () => setShowAck(false);

  return (
    <div className={styles.layout}>
      {showAck && (
        <div className={styles.ackOverlay} role="dialog" aria-modal="true" aria-labelledby="ack-title">
          <div className={styles.ackModal}>
            <h2 id="ack-title" className={styles.ackModalTitle}>Please acknowledge</h2>
            <p className={styles.ackModalBody}>
              This project is <strong>Backend and System Design &amp; Architecture</strong> oriented. The focus is on APIs, services, data flows, and domain logic. The interface you see is kept minimal yet user friendly.
            </p>
            <p className={styles.ackModalBody} style={{ marginBottom: '1rem', fontSize: '0.9rem', color: '#666' }}>
              By continuing, you acknowledge that you understand this context (e.g. as a recruiter or reviewer).
            </p>
            <button type="button" className={styles.ackModalBtn} onClick={handleAck}>
              I understand
            </button>
          </div>
        </div>
      )}
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
          <span className={styles.logo}>OrchexPay</span>
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
