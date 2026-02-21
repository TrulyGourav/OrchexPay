import React, { useState } from 'react';
import PageHeader from '../../components/PageHeader';
import { getWalletApi } from '../../api/wallet';
import ErrorMessage from '../../components/ErrorMessage';
import styles from '../AdminShared.module.css';

const inputStyle = { width: '100%', minWidth: 200 };

export default function WalletSearch() {
  const [merchantId, setMerchantId] = useState('');
  const [currencyCode, setCurrencyCode] = useState('INR');
  const [walletType, setWalletType] = useState('ESCROW');
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const search = async (e) => {
    e.preventDefault();
    setError('');
    setResult(null);
    if (!merchantId.trim()) {
      setError('Merchant ID required');
      return;
    }
    setLoading(true);
    try {
      const res = await getWalletApi.getWalletByType(merchantId.trim(), currencyCode, walletType, null);
      setResult(res.data);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to fetch wallet');
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <PageHeader title="Wallet search" subtitle="Resolve wallet by merchant and type" />
      <div className={styles.card}>
        <form onSubmit={search} style={{ display: 'flex', flexWrap: 'wrap', gap: '1rem', alignItems: 'flex-end', marginBottom: '1rem' }}>
          <div>
            <label className={styles.label}>Merchant ID (UUID)</label>
            <input
              type="text"
              value={merchantId}
              onChange={(e) => setMerchantId(e.target.value)}
              placeholder="e.g. 550e8400-e29b-41d4-a716-446655440000"
              className={styles.input}
              style={inputStyle}
            />
          </div>
          <div>
            <label className={styles.label}>Currency</label>
            <input
              type="text"
              value={currencyCode}
              onChange={(e) => setCurrencyCode(e.target.value.toUpperCase().slice(0, 3))}
              className={styles.input}
              style={{ width: 80 }}
              maxLength={3}
            />
          </div>
          <div>
            <label className={styles.label}>Wallet type</label>
            <select value={walletType} onChange={(e) => setWalletType(e.target.value)} className={styles.input}>
              <option value="MAIN">MAIN</option>
              <option value="ESCROW">ESCROW</option>
              <option value="VENDOR">VENDOR</option>
            </select>
          </div>
          <button type="submit" className={styles.btn} disabled={loading}>{loading ? 'Searching...' : 'Search'}</button>
        </form>
        <ErrorMessage message={error} onDismiss={() => setError('')} />
        {result && (
          <pre style={{ background: '#f5f5f5', padding: '1rem', borderRadius: 4, overflow: 'auto', fontSize: '0.85rem' }}>
            {JSON.stringify(result, null, 2)}
          </pre>
        )}
      </div>
    </>
  );
}
