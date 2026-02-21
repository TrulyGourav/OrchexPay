import React, { useState, useEffect } from 'react';
import { usersApi } from '../../api/users';
import PageHeader from '../../components/PageHeader';
import ErrorMessage from '../../components/ErrorMessage';
import styles from '../AdminShared.module.css';

export default function BankDetails() {
  const [details, setDetails] = useState(null);
  const [accountNumber, setAccountNumber] = useState('');
  const [ifscCode, setIfscCode] = useState('');
  const [beneficiaryName, setBeneficiaryName] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    usersApi.getMyBankDetails()
      .then((res) => {
        const d = res.data;
        if (d) {
          setDetails(d);
          setAccountNumber(d.accountNumber || '');
          setIfscCode(d.ifscCode || '');
          setBeneficiaryName(d.beneficiaryName || '');
        }
      })
      .catch((err) => {
        if (err.response?.status !== 204) setError(err.response?.data?.message || err.message || 'Failed to load');
      })
      .finally(() => setLoading(false));
  }, []);

  const submit = async (e) => {
    e.preventDefault();
    if (!accountNumber.trim() || !beneficiaryName.trim()) {
      setError('Account number and beneficiary name required');
      return;
    }
    setSaving(true);
    setError('');
    try {
      const res = await usersApi.updateMyBankDetails({ accountNumber: accountNumber.trim(), ifscCode: ifscCode.trim(), beneficiaryName: beneficiaryName.trim() });
      setDetails(res.data);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to save');
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <PageHeader title="Bank details" subtitle="Manage bank account for payouts" />
      <ErrorMessage message={error} onDismiss={() => setError('')} />
      <div className={styles.card}>
        {loading ? <p>Loading...</p> : (
          <form onSubmit={submit} style={{ maxWidth: 400 }}>
            <div style={{ marginBottom: '0.75rem' }}>
              <label className={styles.label}>Account number</label>
              <input type="text" value={accountNumber} onChange={(e) => setAccountNumber(e.target.value)} className={styles.input} style={{ width: '100%' }} />
            </div>
            <div style={{ marginBottom: '0.75rem' }}>
              <label className={styles.label}>IFSC code</label>
              <input type="text" value={ifscCode} onChange={(e) => setIfscCode(e.target.value)} className={styles.input} style={{ width: '100%' }} />
            </div>
            <div style={{ marginBottom: '0.75rem' }}>
              <label className={styles.label}>Beneficiary name</label>
              <input type="text" value={beneficiaryName} onChange={(e) => setBeneficiaryName(e.target.value)} className={styles.input} style={{ width: '100%' }} />
            </div>
            <button type="submit" className={styles.btn} disabled={saving}>{saving ? 'Saving...' : 'Save'}</button>
          </form>
        )}
        {details?.updatedAt && <p className={styles.muted} style={{ marginTop: '1rem' }}>Last updated: {new Date(details.updatedAt).toLocaleString()}</p>}
      </div>
    </>
  );
}
