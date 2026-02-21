import React, { useState, useEffect, useCallback } from 'react';
import { useLocation } from 'react-router-dom';
import { usersApi } from '../../api/users';
import { payoutApi } from '../../api/payout';
import { getWalletApi } from '../../api/wallet';
import PageHeader from '../../components/PageHeader';
import ErrorMessage from '../../components/ErrorMessage';
import DataTable from '../../components/DataTable';
import StatusBadge from '../../components/StatusBadge';
import styles from '../AdminShared.module.css';

const STATUS_LABELS = {
  CREATED: 'Created',
  PROCESSING: 'Pending bank transfer',
  SETTLED: 'Completed',
  FAILED: 'Failed (amount returned to wallet)',
};

export default function PayoutStatus() {
  const location = useLocation();
  const [vendorId, setVendorId] = useState(null);
  const [walletBalance, setWalletBalance] = useState(null);
  const [data, setData] = useState({ content: [], totalElements: 0 });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setError('');
    try {
      const me = await usersApi.getMe();
      const id = me.data?.id;
      const vendorWalletId = me.data?.vendorWalletId;
      if (!id) {
        setLoading(false);
        return;
      }
      setVendorId(id);
      const [payoutsRes, walletRes] = await Promise.all([
        payoutApi.listPayouts({ vendorId: id, size: 50 }),
        vendorWalletId ? getWalletApi.getWallet(vendorWalletId).catch(() => null) : Promise.resolve(null),
      ]);
      setData(payoutsRes.data);
      setWalletBalance(walletRes?.data ?? null);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to load');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (location.pathname === '/vendor/payout-status') {
      load();
    }
  }, [location.pathname, load]);

  const columns = [
    { key: 'id', label: 'Payout ID', render: (v) => (v ? String(v).slice(0, 8) + '...' : '—') },
    { key: 'amount', label: 'Amount' },
    { key: 'currencyCode', label: 'Currency' },
    {
      key: 'status',
      label: 'Status',
      render: (v) => (
        <span title={v}>
          <StatusBadge status={v} /> {STATUS_LABELS[v] ? `(${STATUS_LABELS[v]})` : ''}
        </span>
      ),
    },
    { key: 'createdAt', label: 'Created', render: (v) => (v ? new Date(v).toLocaleString() : '') },
  ];

  return (
    <>
      <PageHeader title="Payout status" subtitle="Track payout status; balance updates when payout is completed or failed." />
      <ErrorMessage message={error} onDismiss={() => setError('')} />
      {walletBalance != null && (
        <div className={styles.card} style={{ marginBottom: '1rem' }}>
          <h3 style={{ margin: '0 0 0.5rem', fontSize: '1rem' }}>Current wallet balance</h3>
          <p style={{ margin: 0, fontSize: '1.25rem', fontWeight: 600 }}>
            {walletBalance.balance ?? 0} {walletBalance.currencyCode ?? 'INR'}
          </p>
          <p className={styles.muted} style={{ marginTop: '0.5rem', marginBottom: 0 }}>
            This balance reflects completed ledger entries. Processing payouts are reserved until the transfer is completed or failed.
          </p>
        </div>
      )}
      <div className={styles.card}>
        <div style={{ marginBottom: '0.75rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <button type="button" className={styles.btn} onClick={load} disabled={loading}>
            Refresh
          </button>
        </div>
        {loading ? (
          <p>Loading...</p>
        ) : (
          <>
            <DataTable columns={columns} data={data.content || []} emptyMessage="No payouts yet." />
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
