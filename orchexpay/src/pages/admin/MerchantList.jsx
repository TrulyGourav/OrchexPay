import React, { useState, useEffect } from 'react';
import PageHeader from '../../components/PageHeader';
import { merchantsApi } from '../../api/merchants';
import ErrorMessage from '../../components/ErrorMessage';
import DataTable from '../../components/DataTable';
import styles from '../AdminShared.module.css';

export default function MerchantList() {
  const [data, setData] = useState({ content: [], totalElements: 0, totalPages: 0 });
  const [page, setPage] = useState(0);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      try {
        const res = await merchantsApi.listMerchants({ page, size: 20 });
        setData(res.data);
      } catch (err) {
        setError(err.response?.data?.message || err.message || 'Failed to load merchants');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [page]);

  const columns = [
    { key: 'username', label: 'Username' },
    { key: 'id', label: 'User ID', render: (v) => v?.slice?.(0, 8) + '...' },
    { key: 'merchantId', label: 'Merchant ID', render: (v) => v?.slice?.(0, 8) + '...' },
  ];

  return (
    <>
      <PageHeader title="Merchant list" subtitle="All merchants in the platform" />
      <ErrorMessage message={error} onDismiss={() => setError('')} />
      <div className={styles.card}>
        {loading ? <p>Loading...</p> : (
          <>
            <DataTable columns={columns} data={data.content} emptyMessage="No merchants" />
            {data.totalPages > 1 && (
              <p className={styles.muted} style={{ marginTop: '1rem' }}>
                Page {page + 1} of {data.totalPages} ({data.totalElements} total)
                <button type="button" className={styles.btn} style={{ marginLeft: '1rem' }} disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Previous</button>
                <button type="button" className={styles.btn} style={{ marginLeft: '0.5rem' }} disabled={page >= data.totalPages - 1} onClick={() => setPage((p) => p + 1)}>Next</button>
              </p>
            )}
          </>
        )}
      </div>
    </>
  );
}
