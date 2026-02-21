import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { usersApi } from '../../api/users';
import { getWalletApi } from '../../api/wallet';
import PageHeader from '../../components/PageHeader';
import ErrorMessage from '../../components/ErrorMessage';
import styles from '../AdminShared.module.css';

export default function VendorDashboard() {
  const { user } = useAuth();
  const [wallet, setWallet] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      try {
        const meRes = await usersApi.getMe();
        const vendorWalletId = meRes.data?.vendorWalletId;
        if (vendorWalletId) {
          const w = await getWalletApi.getWallet(vendorWalletId);
          setWallet(w.data);
        }
      } catch (err) {
        setError(err.response?.data?.message || err.message || 'Failed to load wallet');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  return (
    <>
      <PageHeader title="Vendor dashboard" subtitle={user?.username ? `Logged in as ${user.username}` : ''} />
      <ErrorMessage message={error} onDismiss={() => setError('')} />
      {loading ? <p>Loading...</p> : (
        <div className={styles.card}>
          {wallet ? (
            <>
              <p style={{ fontSize: '1.5rem', fontWeight: 600, margin: 0 }}>{wallet.balance} {wallet.currencyCode}</p>
              <p style={{ margin: '0.5rem 0 0', fontSize: '0.85rem', color: '#666' }}>Wallet ID: {wallet.id}</p>
            </>
          ) : (
            <p className={styles.muted}>No vendor wallet found for your account.</p>
          )}
        </div>
      )}
    </>
  );
}
