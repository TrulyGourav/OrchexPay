import React, { useState, useEffect, useCallback } from 'react';
import PageHeader from '../../components/PageHeader';
import { merchantsApi } from '../../api/merchants';
import { usersApi } from '../../api/users';
import ErrorMessage from '../../components/ErrorMessage';
import DataTable from '../../components/DataTable';
import styles from '../AdminShared.module.css';

export default function MerchantList() {
  const [data, setData] = useState({ content: [], totalElements: 0, totalPages: 0 });
  const [page, setPage] = useState(0);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [loading, setLoading] = useState(true);
  const [showOnboardForm, setShowOnboardForm] = useState(false);
  const [onboardUsername, setOnboardUsername] = useState('');
  const [onboardPassword, setOnboardPassword] = useState('');
  const [onboardCurrency, setOnboardCurrency] = useState('INR');
  const [onboardSubmitting, setOnboardSubmitting] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await merchantsApi.listMerchants({ page, size: 20 });
      setData(res.data);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to load merchants');
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    load();
  }, [load]);

  const handleOnboardSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccessMessage('');
    const username = onboardUsername.trim();
    const password = onboardPassword;
    if (!username || username.length < 2) {
      setError('Username must be at least 2 characters');
      return;
    }
    if (!password || password.length < 8) {
      setError('Password must be at least 8 characters');
      return;
    }
    setOnboardSubmitting(true);
    try {
      await usersApi.createUser({
        username,
        password,
        roles: ['MERCHANT'],
        currencyCode: onboardCurrency || 'INR',
      });
      setSuccessMessage(
        `Merchant "${username}" onboarded successfully. Please record these credentials (username and password) and share with the merchant. They can sign in at the Login page.`
      );
      setOnboardUsername('');
      setOnboardPassword('');
      setOnboardCurrency('INR');
      setShowOnboardForm(false);
      load();
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to onboard merchant');
    } finally {
      setOnboardSubmitting(false);
    }
  };

  const columns = [
    { key: 'username', label: 'Username' },
    { key: 'id', label: 'User ID', render: (v) => v?.slice?.(0, 8) + '...' },
    { key: 'merchantId', label: 'Merchant ID', render: (v) => v?.slice?.(0, 8) + '...' },
  ];

  return (
    <>
      <PageHeader title="Merchant list" subtitle="All merchants in the platform" />
      <ErrorMessage message={error} onDismiss={() => setError('')} />
      {successMessage && (
        <div className={styles.successBanner} role="alert">
          <span>{successMessage}</span>
          <button type="button" className={styles.successBannerDismiss} onClick={() => setSuccessMessage('')} aria-label="Dismiss">×</button>
        </div>
      )}
      <div className={styles.card}>
        <div style={{ marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
          <button
            type="button"
            className={styles.btn}
            onClick={() => { setShowOnboardForm((v) => !v); setError(''); setSuccessMessage(''); }}
          >
            {showOnboardForm ? 'Cancel' : 'Onboard merchant'}
          </button>
        </div>
        {showOnboardForm && (
          <form onSubmit={handleOnboardSubmit} style={{ marginBottom: '1.5rem', padding: '1rem', background: '#f8f9fa', borderRadius: '8px', maxWidth: '400px' }}>
            <h3 style={{ margin: '0 0 0.75rem', fontSize: '1rem', color: '#333' }}>Register new merchant</h3>
            <label className={styles.label}>Username</label>
            <input
              type="text"
              className={styles.input}
              value={onboardUsername}
              onChange={(e) => setOnboardUsername(e.target.value)}
              minLength={2}
              placeholder="e.g. acme_merchant"
              style={{ width: '100%', marginBottom: '0.75rem' }}
            />
            <label className={styles.label}>Password (min 8 characters)</label>
            <input
              type="password"
              className={styles.input}
              value={onboardPassword}
              onChange={(e) => setOnboardPassword(e.target.value)}
              minLength={8}
              placeholder="••••••••"
              style={{ width: '100%', marginBottom: '0.75rem' }}
            />
            <label className={styles.label}>Currency</label>
            <input
              type="text"
              className={styles.input}
              value={onboardCurrency}
              onChange={(e) => setOnboardCurrency(e.target.value.toUpperCase().slice(0, 3))}
              placeholder="INR"
              maxLength={3}
              style={{ width: '100%', marginBottom: '1rem' }}
            />
            <button type="submit" className={styles.btn} disabled={onboardSubmitting}>
              {onboardSubmitting ? 'Creating...' : 'Create merchant'}
            </button>
          </form>
        )}
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
