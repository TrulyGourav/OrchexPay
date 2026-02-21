import React, { useState } from 'react';
import PageHeader from '../../components/PageHeader';
import { getWalletApi } from '../../api/wallet';
import ErrorMessage from '../../components/ErrorMessage';
import styles from '../AdminShared.module.css';

export default function FreezeWallet() {
  const [walletId, setWalletId] = useState('');
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const freeze = async (e) => {
    e.preventDefault();
    if (!walletId.trim()) return;
    setError('');
    setResult(null);
    setLoading(true);
    try {
      const res = await getWalletApi.freezeWallet(walletId.trim());
      setResult({ action: 'Frozen', wallet: res.data });
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to freeze');
    } finally {
      setLoading(false);
    }
  };

  const unfreeze = async (e) => {
    e.preventDefault();
    if (!walletId.trim()) return;
    setError('');
    setResult(null);
    setLoading(true);
    try {
      const res = await getWalletApi.unfreezeWallet(walletId.trim());
      setResult({ action: 'Unfrozen', wallet: res.data });
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to unfreeze');
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <PageHeader title="Freeze / unfreeze wallet" subtitle="Suspend or restore wallet operations" />
      <div className={styles.card}>
        <form onSubmit={(e) => e.preventDefault()} style={{ display: 'flex', gap: '1rem', alignItems: 'flex-end', marginBottom: '1rem' }}>
          <div>
            <label className={styles.label}>Wallet ID (UUID)</label>
            <input type="text" value={walletId} onChange={(e) => setWalletId(e.target.value)} className={styles.input} style={{ minWidth: 280 }} placeholder="UUID" />
          </div>
          <button type="button" className={styles.btn} onClick={freeze} disabled={loading}>Freeze</button>
          <button type="button" className={styles.btn} onClick={unfreeze} disabled={loading}>Unfreeze</button>
        </form>
        <ErrorMessage message={error} onDismiss={() => setError('')} />
        {result && (
          <p style={{ color: 'green' }}>{result.action}. Balance: {result.wallet?.balance} {result.wallet?.currencyCode} (status: {result.wallet?.status})</p>
        )}
      </div>
    </>
  );
}
