import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
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

export default function MerchantTransactions() {
  const { user } = useAuth();
  const merchantId = user?.merchantId;
  const [data, setData] = useState({ content: [], totalElements: 0, totalPages: 0 });
  const [page, setPage] = useState(0);
  const [referenceType, setReferenceType] = useState('');
  const [status, setStatus] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const load = () => {
    if (!merchantId) return;
    setLoading(true);
    setError('');
    getEntries({
      merchantId,
      page,
      size: 20,
      referenceType: referenceType || undefined,
      status: status || undefined,
    })
      .then((res) => setData(res.data))
      .catch((err) => setError(err.response?.data?.message || err.message || 'Failed to load entries'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    if (merchantId) load();
  }, [merchantId, page, referenceType, status]);

  if (!merchantId) {
    return (
      <>
        <PageHeader title="Transactions" subtitle="Filterable ledger entries" />
        <p>Merchant ID not found.</p>
      </>
    );
  }

  return (
    <>
      <PageHeader title="Transactions" subtitle="Filterable ledger entries for your merchant" />
      <div className={styles.card}>
        <form
          onSubmit={(e) => { e.preventDefault(); setPage(0); }}
          style={{ display: 'flex', flexWrap: 'wrap', gap: '1rem', alignItems: 'flex-end', marginBottom: '1rem' }}
        >
          <div>
            <label className={styles.label}>Ref type</label>
            <select
              value={referenceType}
              onChange={(e) => setReferenceType(e.target.value)}
              className={styles.input}
              style={{ minWidth: 120 }}
            >
              <option value="">All</option>
              <option value="ORDER">ORDER</option>
              <option value="PAYOUT">PAYOUT</option>
              <option value="REFUND">REFUND</option>
              <option value="REVERSAL">REVERSAL</option>
            </select>
          </div>
          <div>
            <label className={styles.label}>Status</label>
            <select
              value={status}
              onChange={(e) => setStatus(e.target.value)}
              className={styles.input}
              style={{ minWidth: 120 }}
            >
              <option value="">All</option>
              <option value="PENDING">PENDING</option>
              <option value="CONFIRMED">CONFIRMED</option>
              <option value="REVERSED">REVERSED</option>
            </select>
          </div>
          <button type="submit" className={styles.btn} disabled={loading}>
            {loading ? 'Loading…' : 'Apply'}
          </button>
        </form>
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
          <p className={styles.muted}>No ledger entries match the filters.</p>
        )}
      </div>
    </>
  );
}
