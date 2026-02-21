import React, { useState, useEffect } from 'react';
import PageHeader from '../../components/PageHeader';
import { getEntries } from '../../api/entries';
import ErrorMessage from '../../components/ErrorMessage';
import DataTable from '../../components/DataTable';
import styles from '../AdminShared.module.css';

export default function TransactionExplorer() {
  const [walletId, setWalletId] = useState('');
  const [merchantId, setMerchantId] = useState('');
  const [data, setData] = useState({ content: [], totalElements: 0, totalPages: 0 });
  const [page, setPage] = useState(0);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const search = (e) => {
    e?.preventDefault();
    if (!walletId.trim() && !merchantId.trim()) {
      setError('Provide wallet ID or merchant ID');
      return;
    }
    setLoading(true);
    setError('');
    getEntries({ walletId: walletId.trim() || undefined, merchantId: merchantId.trim() || undefined, page, size: 20 })
      .then((res) => setData(res.data))
      .catch((err) => setError(err.response?.data?.message || err.message || 'Failed'))
      .finally(() => setLoading(false));
  };


  const columns = [
    { key: 'id', label: 'Entry ID', render: (v) => v?.slice?.(0, 8) + '...' },
    { key: 'type', label: 'Type' },
    { key: 'amount', label: 'Amount' },
    { key: 'currencyCode', label: 'Currency' },
    { key: 'referenceType', label: 'Ref type' },
    { key: 'status', label: 'Status' },
    { key: 'createdAt', label: 'Created', render: (v) => v ? new Date(v).toLocaleString() : '' },
  ];

  return (
    <>
      <PageHeader title="Transaction explorer" subtitle="Search by wallet or merchant" />
      <div className={styles.card}>
        <form onSubmit={search} style={{ display: 'flex', flexWrap: 'wrap', gap: '1rem', alignItems: 'flex-end', marginBottom: '1rem' }}>
          <div>
            <label className={styles.label}>Wallet ID</label>
            <input type="text" value={walletId} onChange={(e) => setWalletId(e.target.value)} className={styles.input} style={{ minWidth: 220 }} placeholder="UUID" />
          </div>
          <div>
            <label className={styles.label}>Merchant ID</label>
            <input type="text" value={merchantId} onChange={(e) => setMerchantId(e.target.value)} className={styles.input} style={{ minWidth: 220 }} placeholder="UUID" />
          </div>
          <button type="submit" className={styles.btn} disabled={loading}>{loading ? 'Loading...' : 'Search'}</button>
        </form>
        <ErrorMessage message={error} onDismiss={() => setError('')} />
        {data.content?.length > 0 && (
          <>
            <DataTable columns={columns} data={data.content} emptyMessage="No entries" />
            <p className={styles.muted} style={{ marginTop: '1rem' }}>Total: {data.totalElements}</p>
          </>
        )}
      </div>
    </>
  );
}
