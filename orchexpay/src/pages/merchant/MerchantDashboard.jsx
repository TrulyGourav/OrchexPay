import React, { useState, useEffect, useCallback } from 'react';
import { useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { usersApi } from '../../api/users';
import { getWalletApi } from '../../api/wallet';
import PageHeader from '../../components/PageHeader';
import ErrorMessage from '../../components/ErrorMessage';
import styles from '../AdminShared.module.css';

export default function MerchantDashboard() {
  const { user } = useAuth();
  const location = useLocation();
  const [main, setMain] = useState(null);
  const [escrow, setEscrow] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const meRes = await usersApi.getMe();
      const { mainWalletId, escrowWalletId } = meRes.data;
      if (mainWalletId && escrowWalletId) {
        const [mainRes, escrowRes] = await Promise.all([
          getWalletApi.getWallet(mainWalletId),
          getWalletApi.getWallet(escrowWalletId),
        ]);
        setMain(mainRes.data);
        setEscrow(escrowRes.data);
      } else {
        setMain(null);
        setEscrow(null);
      }
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to load wallets');
    } finally {
      setLoading(false);
    }
  }, []);

  // Refetch whenever user navigates to dashboard (e.g. after crediting escrow on Order Completion)
  useEffect(() => {
    if (location.pathname === '/merchant') {
      load();
    }
  }, [location.pathname, load]);

  return (
    <>
      <PageHeader title="Merchant dashboard" subtitle={user?.username ? `Logged in as ${user.username}` : ''} />
      <ErrorMessage message={error} onDismiss={() => setError('')} />
      {loading ? (
        <p>Loading...</p>
      ) : (
        <>
          <div style={{ marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <button type="button" className={styles.btn} onClick={load} disabled={loading}>
              Refresh balances
            </button>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))', gap: '1rem' }}>
        <div className={styles.card}>
          <h3 style={{ margin: '0 0 0.5rem', fontSize: '1rem' }}>MAIN wallet</h3>
          {main ? (
            <>
              <p style={{ margin: 0, fontSize: '1.5rem', fontWeight: 600 }}>{main.balance ?? 0} {main.currencyCode}</p>
              <p style={{ margin: '0.5rem 0 0', fontSize: '0.85rem', color: '#666' }}>ID: {main.id}</p>
            </>
          ) : (
            <p style={{ margin: 0, color: '#666' }}>—</p>
          )}
        </div>
        <div className={styles.card}>
          <h3 style={{ margin: '0 0 0.5rem', fontSize: '1rem' }}>ESCROW wallet</h3>
          {escrow ? (
            <>
              <p style={{ margin: 0, fontSize: '1.5rem', fontWeight: 600 }}>{escrow.balance ?? 0} {escrow.currencyCode}</p>
              <p style={{ margin: '0.5rem 0 0', fontSize: '0.85rem', color: '#666' }}>ID: {escrow.id}</p>
            </>
          ) : (
            <p style={{ margin: 0, color: '#666' }}>—</p>
          )}
        </div>
      </div>
        </>
      )}
    </>
  );
}
