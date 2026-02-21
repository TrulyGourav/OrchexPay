import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { merchantsApi } from '../../api/merchants';
import PageHeader from '../../components/PageHeader';
import ErrorMessage from '../../components/ErrorMessage';
import styles from '../AdminShared.module.css';

export default function VendorManagement() {
  const { user } = useAuth();
  const merchantId = user?.merchantId;
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [currencyCode, setCurrencyCode] = useState('INR');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    if (!merchantId) {
      setError('Merchant ID not found.');
      return;
    }
    if (!username.trim() || password.length < 8) {
      setError('Username required; password at least 8 characters.');
      return;
    }
    setLoading(true);
    try {
      await merchantsApi.addVendor(merchantId, {
        username: username.trim(),
        password,
        currencyCode: currencyCode.trim() || 'INR',
      });
      setMessage(`Vendor "${username}" created. They can log in with this username and password.`);
      setUsername('');
      setPassword('');
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to add vendor');
    } finally {
      setLoading(false);
    }
  };

  if (!merchantId) {
    return (
      <>
        <PageHeader title="Vendor management" />
        <p>Merchant ID not found in session.</p>
      </>
    );
  }

  return (
    <>
      <PageHeader title="Add vendor" subtitle="Create a vendor user under your merchant" />
      <div className={styles.card}>
        <form onSubmit={submit} style={{ maxWidth: 400 }}>
          <div style={{ marginBottom: '1rem' }}>
            <label className={styles.label}>Username</label>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className={styles.input}
              style={{ width: '100%' }}
              autoComplete="username"
            />
          </div>
          <div style={{ marginBottom: '1rem' }}>
            <label className={styles.label}>Password (min 8 characters)</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className={styles.input}
              style={{ width: '100%' }}
              autoComplete="new-password"
            />
          </div>
          <div style={{ marginBottom: '1rem' }}>
            <label className={styles.label}>Currency (3-letter, e.g. INR)</label>
            <input
              type="text"
              value={currencyCode}
              onChange={(e) => setCurrencyCode(e.target.value.toUpperCase().slice(0, 3))}
              className={styles.input}
              style={{ width: 80 }}
              maxLength={3}
            />
          </div>
          <button type="submit" className={styles.btn} disabled={loading}>
            {loading ? 'Creating...' : 'Create vendor'}
          </button>
        </form>
        <ErrorMessage message={error} onDismiss={() => setError('')} />
        {message && <p style={{ marginTop: '1rem', color: 'green' }}>{message}</p>}
      </div>
    </>
  );
}
