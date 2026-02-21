import React, { useState, useEffect } from 'react';
import { usersApi } from '../../api/users';
import { payoutApi } from '../../api/payout';
import PageHeader from '../../components/PageHeader';
import ErrorMessage from '../../components/ErrorMessage';
import DataTable from '../../components/DataTable';
import StatusBadge from '../../components/StatusBadge';
import styles from '../AdminShared.module.css';

export default function PayoutStatus() {
  const [vendorId, setVendorId] = useState(null);
  const [data, setData] = useState({ content: [], totalElements: 0 });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    usersApi.getMe()
      .then((me) => {
        const id = me.data?.id;
        if (id) {
          setVendorId(id);
          return payoutApi.listPayouts({ vendorId: id, size: 50 });
        }
      })
      .then((res) => res && setData(res.data))
      .catch((err) => setError(err.response?.data?.message || err.message || 'Failed'))
      .finally(() => setLoading(false));
  }, []);

  const columns = [
    { key: 'id', label: 'Payout ID', render: (v) => v?.slice?.(0, 8) + '...' },
    { key: 'amount', label: 'Amount' },
    { key: 'currencyCode', label: 'Currency' },
    { key: 'status', label: 'Status', render: (v, row) => <StatusBadge status={v} /> },
    { key: 'createdAt', label: 'Created', render: (v) => v ? new Date(v).toLocaleString() : '' },
  ];

  return (
    <>
      <PageHeader title="Payout status" subtitle="Track payout status and reversals" />
      <ErrorMessage message={error} onDismiss={() => setError('')} />
      <div className={styles.card}>
        {loading ? <p>Loading...</p> : (
          <DataTable columns={columns} data={data.content || []} emptyMessage="No payouts" />
        )}
        {data.totalElements > 0 && <p className={styles.muted} style={{ marginTop: '0.5rem' }}>Total: {data.totalElements}</p>}
      </div>
    </>
  );
}
