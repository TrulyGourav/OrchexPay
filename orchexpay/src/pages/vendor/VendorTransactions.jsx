import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { usersApi } from '../../api/users';
import { getEntries } from '../../api/entries';
import PageHeader from '../../components/PageHeader';
import ErrorMessage from '../../components/ErrorMessage';
import DataTable from '../../components/DataTable';
import styles from '../AdminShared.module.css';

const ENTRY_COLUMNS = [
  { key: 'id', label: 'Entry ID', render: (v) => (v ? String(v).slice(0, 8) + '…' : '') },
  { key: 'type', label: 'Type' },
  { key: 'amount', label: 'Amount', render: (v) => v != null ? Number(v).toLocaleString() : '' },
  { key: 'currencyCode', label: 'Currency' },
  { key: 'referenceType', label: 'Ref type' },
  { key: 'referenceId', label: 'Ref ID', render: (v) => (v ? String(v).slice(0, 12) + (String(v).length > 12 ? '…' : '') : '') },
  { key: 'status', label: 'Status' },
  { key: 'createdAt', label: 'Created', render: (v) => (v ? new Date(v).toLocaleString() : '') },
];

export default function VendorTransactions() {
  const { user } = useAuth();
  const [walletId, setWalletId] = useState(null);
  const [profileLoaded, setProfileLoaded] = useState(false);
  const [data, setData] = useState({ content: [], totalElements: 0, totalPages: 0 });
  const [page, setPage] = useState(0);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    let cancelled = false;
    usersApi
      .getMe()
      .then((res) => {
        if (!cancelled) {
          setWalletId(res.data?.vendorWalletId ?? null);
          setProfileLoaded(true);
        }
      })
      .catch(() => setProfileLoaded(true));
    return () => { cancelled = true; };
  }, []);

  useEffect(() => {
    if (!walletId) return;
    setLoading(true);
    setError('');
    getEntries({ walletId, page, size: 20 })
      .then((res) => setData(res.data))
      .catch((err) => setError(err.response?.data?.message || err.message || 'Failed to load transactions'))
      .finally(() => setLoading(false));
  }, [walletId, page]);

  if (!profileLoaded) {
    return (
      <>
        <PageHeader title="Transaction history" subtitle="Filterable ledger entries" />
        <div className={styles.card}>
          <p className={styles.muted}>Loading your wallet…</p>
        </div>
      </>
    );
  }

  if (!walletId) {
    return (
      <>
        <PageHeader title="Transaction history" subtitle="Filterable ledger entries" />
        <div className={styles.card}>
          <p className={styles.muted}>No vendor wallet found for your account.</p>
        </div>
      </>
    );
  }

  return (
    <>
      <PageHeader title="Transaction history" subtitle="Ledger entries for your vendor wallet" />
      <div className={styles.card}>
        <ErrorMessage message={error} onDismiss={() => setError('')} />
        {data.content?.length > 0 ? (
          <>
            <DataTable columns={ENTRY_COLUMNS} data={data.content} emptyMessage="No entries" />
            <p className={styles.muted} style={{ marginTop: '1rem' }}>
              Page {page + 1} of {data.totalPages || 1} · Total: {data.totalElements}
            </p>
            <div style={{ marginTop: '0.5rem' }}>
              <button
                type="button"
                className={styles.btn}
                disabled={page <= 0 || loading}
                onClick={() => setPage((p) => p - 1)}
              >
                Previous
              </button>
              <button
                type="button"
                className={styles.btn}
                disabled={page >= (data.totalPages || 1) - 1 || loading}
                onClick={() => setPage((p) => p + 1)}
                style={{ marginLeft: '0.5rem' }}
              >
                Next
              </button>
            </div>
          </>
        ) : !loading && (
          <p className={styles.muted}>No transactions yet.</p>
        )}
      </div>
    </>
  );
}
