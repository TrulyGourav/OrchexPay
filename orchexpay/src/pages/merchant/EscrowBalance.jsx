import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { getWalletApi } from '../../api/wallet';
import PageHeader from '../../components/PageHeader';
import ErrorMessage from '../../components/ErrorMessage';
import styles from '../AdminShared.module.css';

export default function EscrowBalance() {
  const { user } = useAuth();
  const merchantId = user?.merchantId;
  const [wallet, setWallet] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!merchantId) {
      setLoading(false);
      return;
    }
    getWalletApi
      .getWalletByType(merchantId, 'INR', 'ESCROW', null)
      .then((res) => setWallet(res.data))
      .catch((err) => {
        if (err.response?.status === 403) {
          setError('Backend must allow MERCHANT to resolve own wallets.');
        } else {
          setError(err.response?.data?.message || err.message || 'Failed to load escrow wallet');
        }
      })
      .finally(() => setLoading(false));
  }, [merchantId]);

  if (loading) return (<><PageHeader title="Escrow balance" /><p>Loading...</p></>);
  if (!merchantId) return (<><PageHeader title="Escrow balance" /><p>Merchant ID not found.</p></>);

  return (
    <>
      <PageHeader title="Escrow balance" subtitle="Current escrow wallet balance" />
      <ErrorMessage message={error} onDismiss={() => setError('')} />
      <div className={styles.card}>
        {wallet ? (
          <>
            <p style={{ fontSize: '1.5rem', fontWeight: 600, margin: 0 }}>{wallet.balance} {wallet.currencyCode}</p>
            <p style={{ margin: '0.5rem 0 0', fontSize: '0.85rem', color: '#666' }}>Wallet ID: {wallet.id}</p>
          </>
        ) : (
          <p className={styles.muted}>No escrow wallet data.</p>
        )}
      </div>
    </>
  );
}
