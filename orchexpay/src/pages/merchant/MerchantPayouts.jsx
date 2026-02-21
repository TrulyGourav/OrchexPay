import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../../context/AuthContext';
import { usersApi } from '../../api/users';
import { payoutApi } from '../../api/payout';
import PageHeader from '../../components/PageHeader';
import ErrorMessage from '../../components/ErrorMessage';
import DataTable from '../../components/DataTable';
import StatusBadge from '../../components/StatusBadge';
import styles from '../AdminShared.module.css';

function idempotencyKeyConfirm(payoutId) {
  return `mock-confirm-${payoutId}`;
}
function idempotencyKeyReverse(payoutId) {
  return `mock-reverse-${payoutId}`;
}

export default function MerchantPayouts() {
  const { user } = useAuth();
  const [merchantId, setMerchantId] = useState(null);
  const [data, setData] = useState({ content: [], totalElements: 0 });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [actioningId, setActioningId] = useState(null);

  const load = useCallback(async () => {
    const me = await usersApi.getMe();
    const mid = me.data?.merchantId;
    if (!mid) {
      setLoading(false);
      return;
    }
    setMerchantId(mid);
    setLoading(true);
    setError('');
    try {
      const res = await payoutApi.listPayouts({ merchantId: mid, size: 100 });
      setData(res.data);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to load payouts');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const handleConfirm = async (payoutId) => {
    setError('');
    setActioningId(payoutId);
    try {
      await payoutApi.confirmPayout(payoutId, idempotencyKeyConfirm(payoutId));
      await load();
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to mark as completed');
    } finally {
      setActioningId(null);
    }
  };

  const handleReverse = async (payoutId) => {
    setError('');
    setActioningId(payoutId);
    try {
      await payoutApi.reversePayout(payoutId, idempotencyKeyReverse(payoutId));
      await load();
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to mark as failed');
    } finally {
      setActioningId(null);
    }
  };

  const columns = [
    { key: 'id', label: 'Payout ID', render: (v) => (v ? String(v).slice(0, 8) + '...' : '—') },
    { key: 'amount', label: 'Amount' },
    { key: 'currencyCode', label: 'Currency' },
    { key: 'status', label: 'Status', render: (v, row) => <StatusBadge status={v} /> },
    { key: 'createdAt', label: 'Created', render: (v) => (v ? new Date(v).toLocaleString() : '') },
    {
      key: 'actions',
      label: 'Actions',
      render: (_, row) => {
        if (row.status !== 'PROCESSING') return '—';
        const busy = actioningId === row.id;
        return (
          <span style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
            <button
              type="button"
              className={styles.btn}
              disabled={busy}
              onClick={() => handleConfirm(row.id)}
            >
              {busy ? '...' : 'Mark completed'}
            </button>
            <button
              type="button"
              className={styles.btn}
              disabled={busy}
              onClick={() => handleReverse(row.id)}
              style={{ background: '#6c757d' }}
            >
              {busy ? '...' : 'Mark failed'}
            </button>
          </span>
        );
      },
    },
  ];

  return (
    <>
      <PageHeader
        title="Vendor payouts"
        subtitle="View and complete payout requests. Mark as completed when bank transfer succeeds, or as failed to return amount to vendor wallet."
      />
      <ErrorMessage message={error} onDismiss={() => setError('')} />
      <div className={styles.card}>
        {loading ? (
          <p>Loading...</p>
        ) : !merchantId ? (
          <p className={styles.muted}>Merchant ID not found.</p>
        ) : (
          <>
            <div style={{ marginBottom: '0.75rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <button type="button" className={styles.btn} onClick={load} disabled={loading}>
                Refresh
              </button>
            </div>
            <DataTable
              columns={columns}
              data={data.content || []}
              emptyMessage="No payouts for your merchant yet."
            />
            {data.totalElements > 0 && (
              <p className={styles.muted} style={{ marginTop: '0.5rem' }}>
                Total: {data.totalElements}
              </p>
            )}
          </>
        )}
      </div>
    </>
  );
}
