import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { merchantsApi } from '../../api/merchants';
import PageHeader from '../../components/PageHeader';
import ErrorMessage from '../../components/ErrorMessage';
import DataTable from '../../components/DataTable';
import styles from '../AdminShared.module.css';

export default function VendorWallets() {
  const { user } = useAuth();
  const merchantId = user?.merchantId;
  const [list, setList] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!merchantId) {
      setLoading(false);
      return;
    }
    merchantsApi.listVendors(merchantId)
      .then((res) => setList(res.data || []))
      .catch((err) => setError(err.response?.data?.message || err.message || 'Failed to load vendors'))
      .finally(() => setLoading(false));
  }, [merchantId]);

  const columns = [
    { key: 'userId', label: 'User ID', render: (v) => v?.slice?.(0, 8) + '...' },
    { key: 'username', label: 'Username' },
    { key: 'vendorWalletId', label: 'Vendor wallet ID', render: (v) => v?.slice?.(0, 8) + '...' },
  ];

  return (
    <>
      <PageHeader title="Vendor wallets" subtitle="Vendors under your merchant" />
      <ErrorMessage message={error} onDismiss={() => setError('')} />
      <div className={styles.card}>
        {loading ? <p>Loading...</p> : !merchantId ? <p>Merchant ID not found.</p> : (
          <DataTable columns={columns} data={list} emptyMessage="No vendors" />
        )}
      </div>
    </>
  );
}
