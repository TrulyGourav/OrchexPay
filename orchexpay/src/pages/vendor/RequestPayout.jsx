import React, { useState } from 'react';
import { payoutApi } from '../../api/payout';
import PageHeader from '../../components/PageHeader';
import ErrorMessage from '../../components/ErrorMessage';
import styles from '../AdminShared.module.css';

function generateIdempotencyKey() {
  return 'payout-' + Date.now() + '-' + Math.random().toString(36).slice(2, 10);
}

export default function RequestPayout() {
  const [amount, setAmount] = useState('');
  const [currencyCode, setCurrencyCode] = useState('INR');
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setResult(null);
    const num = parseFloat(amount);
    if (Number.isNaN(num) || num < 0.01) {
      setError('Amount must be at least 0.01');
      return;
    }
    setLoading(true);
    try {
      const res = await payoutApi.requestPayoutVendor(
        { amount: num, currencyCode: currencyCode.trim() || 'INR' },
        generateIdempotencyKey()
      );
      setResult(res.data);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to create payout');
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <PageHeader title="Request payout" subtitle="Request a payout from your vendor wallet" />
      <ErrorMessage message={error} onDismiss={() => setError('')} />
      <div className={styles.card}>
        <form onSubmit={submit} style={{ maxWidth: 400 }}>
          <div style={{ marginBottom: '0.75rem' }}>
            <label className={styles.label}>Amount</label>
            <input type="number" step="0.01" min="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} className={styles.input} style={{ width: 120 }} />
          </div>
          <div style={{ marginBottom: '0.75rem' }}>
            <label className={styles.label}>Currency</label>
            <input type="text" value={currencyCode} onChange={(e) => setCurrencyCode(e.target.value.slice(0, 3))} className={styles.input} style={{ width: 80 }} maxLength={3} />
          </div>
          <button type="submit" className={styles.btn} disabled={loading}>{loading ? 'Creating...' : 'Request payout'}</button>
        </form>
        {result && (
          <div style={{ marginTop: '1rem', padding: '1rem', background: '#f0f9f0', borderRadius: 4 }}>
            <p><strong>Payout created</strong></p>
            <p>ID: {result.id}</p>
            <p>Status: {result.status}</p>
          </div>
        )}
      </div>
    </>
  );
}
