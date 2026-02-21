import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { getWalletApi } from '../../api/wallet';
import { payoutApi } from '../../api/payout';
import PageHeader from '../../components/PageHeader';
import ErrorMessage from '../../components/ErrorMessage';
import styles from '../AdminShared.module.css';

export default function OrderSettlement() {
  const { user } = useAuth();
  const merchantId = user?.merchantId;
  const [settlement, setSettlement] = useState(null);
  const [commission, setCommission] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const load = async () => {
    if (!merchantId) return;
    setError('');
    setLoading(true);
    try {
      const [setRes, commRes] = await Promise.all([
        getWalletApi.getSettlement(merchantId, 'INR'),
        payoutApi.getCommission(merchantId).catch(() => ({ data: null })),
      ]);
      setSettlement(setRes.data);
      setCommission(commRes.data);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to load');
    } finally {
      setLoading(false);
    }
  };

  React.useEffect(() => {
    if (merchantId) load();
  }, [merchantId]);

  if (!merchantId) {
    return (
      <>
        <PageHeader title="Order settlement" />
        <p>Merchant ID not found.</p>
      </>
    );
  }

  return (
    <>
      <PageHeader title="Order settlement" subtitle="Escrow reconciliation and commission" />
      <ErrorMessage message={error} onDismiss={() => setError('')} />
      <div className={styles.card} style={{ marginBottom: '1rem' }}>
        <button type="button" className={styles.btn} onClick={load} disabled={loading}>
          {loading ? 'Loading...' : 'Refresh'}
        </button>
      </div>
      {settlement && (
        <div className={styles.card}>
          <h3 style={{ marginTop: 0 }}>Settlement (INR)</h3>
          <p><strong>Escrow wallet:</strong> {settlement.escrowWalletId || '—'}</p>
          <p><strong>Total confirmed credits:</strong> {settlement.totalConfirmedEscrowCredits}</p>
          <p><strong>Total payout debits:</strong> {settlement.totalPayoutDebits}</p>
          <p><strong>Total refund debits:</strong> {settlement.totalRefundDebits}</p>
          <p><strong>Expected balance:</strong> {settlement.expectedBalance}</p>
          <p><strong>Ledger net:</strong> {settlement.ledgerNetBalance} — {settlement.reconciled ? 'Reconciled' : 'Mismatch'}</p>
        </div>
      )}
      {commission != null && (
        <div className={styles.card}>
          <h3 style={{ marginTop: 0 }}>Commission</h3>
          <p><strong>Rate:</strong> {commission.percentageValue != null ? `${commission.percentageValue}%` : '—'}</p>
        </div>
      )}
    </>
  );
}
