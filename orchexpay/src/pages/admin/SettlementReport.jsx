import React, { useState } from 'react';
import PageHeader from '../../components/PageHeader';
import { getWalletApi } from '../../api/wallet';
import ErrorMessage from '../../components/ErrorMessage';
import styles from '../AdminShared.module.css';

export default function SettlementReport() {
  const [merchantId, setMerchantId] = useState('');
  const [currencyCode, setCurrencyCode] = useState('INR');
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const load = async (e) => {
    e.preventDefault();
    setError('');
    setData(null);
    if (!merchantId.trim()) {
      setError('Merchant ID required');
      return;
    }
    setLoading(true);
    try {
      const res = await getWalletApi.getSettlement(merchantId.trim(), currencyCode);
      setData(res.data);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to load settlement');
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <PageHeader title="Settlement report" subtitle="Reconciliation for merchant escrow" />
      <div className={styles.card}>
        <form onSubmit={load} style={{ display: 'flex', flexWrap: 'wrap', gap: '1rem', alignItems: 'flex-end', marginBottom: '1rem' }}>
          <div>
            <label className={styles.label}>Merchant ID</label>
            <input
              type="text"
              value={merchantId}
              onChange={(e) => setMerchantId(e.target.value)}
              placeholder="UUID"
              className={styles.input}
              style={{ minWidth: 280 }}
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
          <button type="submit" className={styles.btn} disabled={loading}>{loading ? 'Loading...' : 'Load'}</button>
        </form>
        <ErrorMessage message={error} onDismiss={() => setError('')} />
        {data && (
          <div style={{ marginTop: '1rem' }}>
            <p><strong>Escrow wallet:</strong> {data.escrowWalletId || '—'}</p>
            <p><strong>Total confirmed credits:</strong> {data.totalConfirmedEscrowCredits} {data.currencyCode}</p>
            <p><strong>Total payout debits:</strong> {data.totalPayoutDebits}</p>
            <p><strong>Total refund debits:</strong> {data.totalRefundDebits}</p>
            <p><strong>Expected balance:</strong> {data.expectedBalance}</p>
            <p><strong>Ledger net balance:</strong> {data.ledgerNetBalance}</p>
            <p><strong>Reconciled:</strong> {data.reconciled ? 'Yes' : 'No (mismatch)'}</p>
          </div>
        )}
      </div>
    </>
  );
}
