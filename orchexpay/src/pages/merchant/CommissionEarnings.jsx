import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { payoutApi } from '../../api/payout';
import PageHeader from '../../components/PageHeader';
import ErrorMessage from '../../components/ErrorMessage';
import styles from '../AdminShared.module.css';

export default function CommissionEarnings() {
  const { user } = useAuth();
  const merchantId = user?.merchantId;
  const [commission, setCommission] = useState(null);
  const [rateInput, setRateInput] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  const load = async () => {
    if (!merchantId) return;
    setLoading(true);
    setError('');
    try {
      const res = await payoutApi.getCommission(merchantId);
      setCommission(res.data);
      const rate = res.data.percentageValue != null ? res.data.percentageValue : '';
      setRateInput(rate !== '' ? String(rate) : '');
    } catch (err) {
      if (err.response?.status === 404) {
        setCommission(null);
        setRateInput('');
      } else {
        setError(err.response?.data?.message || err.message || 'Failed to load commission');
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (merchantId) load();
  }, [merchantId]);

  const saveRate = async (e) => {
    e.preventDefault();
    if (!merchantId) return;
    const rate = parseFloat(rateInput);
    if (Number.isNaN(rate) || rate < 0 || rate > 100) {
      setError('Enter a valid rate 0–100');
      return;
    }
    setSaving(true);
    setError('');
    try {
      await payoutApi.putCommission(merchantId, {
        commissionType: 'PERCENTAGE',
        percentageValue: rate,
        currencyCode: 'INR',
      });
      setCommission((c) => (c ? { ...c, percentageValue: rate } : { percentageValue: rate }));
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to update');
    } finally {
      setSaving(false);
    }
  };

  if (!merchantId) {
    return (
      <>
        <PageHeader title="Commission" />
        <p>Merchant ID not found.</p>
      </>
    );
  }

  return (
    <>
      <PageHeader title="Commission earnings" subtitle="View and set commission rate" />
      <ErrorMessage message={error} onDismiss={() => setError('')} />
      <div className={styles.card}>
        {loading ? (
          <p>Loading...</p>
        ) : (
          <>
            <p><strong>Current rate:</strong> {commission?.percentageValue != null ? `${commission.percentageValue}%` : 'Not set'}</p>
            <form onSubmit={saveRate} style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', marginTop: '1rem' }}>
              <label className={styles.label} style={{ marginBottom: 0 }}>Rate %</label>
              <input
                type="number"
                min={0}
                max={100}
                step={0.01}
                value={rateInput}
                onChange={(e) => setRateInput(e.target.value)}
                className={styles.input}
                style={{ width: 100 }}
              />
              <button type="submit" className={styles.btn} disabled={saving}>{saving ? 'Saving...' : 'Update'}</button>
            </form>
          </>
        )}
      </div>
    </>
  );
}
