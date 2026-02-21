import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import PageHeader from '../../components/PageHeader';
import ErrorMessage from '../../components/ErrorMessage';
import DataTable from '../../components/DataTable';
import { getAdminStats, getPayoutStats } from '../../api/admin';
import { merchantsApi } from '../../api/merchants';
import styles from '../AdminShared.module.css';

function StatCard({ label, value, sub }) {
  return (
    <div className={styles.statCard}>
      <p className={styles.value}>{value}</p>
      <p className={styles.label}>{label}</p>
      {sub != null && sub !== '' && <p className={styles.muted} style={{ margin: '0.25rem 0 0', fontSize: '0.8rem' }}>{sub}</p>}
    </div>
  );
}

export default function AdminDashboard() {
  const [platformStats, setPlatformStats] = useState(null);
  const [payoutStats, setPayoutStats] = useState(null);
  const [recentMerchants, setRecentMerchants] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      setError('');
      try {
        const [statsRes, payoutRes, merchantsRes] = await Promise.allSettled([
          getAdminStats(),
          getPayoutStats(),
          merchantsApi.listMerchants({ page: 0, size: 5 }),
        ]);
        if (statsRes.status === 'fulfilled') setPlatformStats(statsRes.value.data);
        else setError(prev => prev || statsRes.reason?.response?.data?.message || 'Failed to load platform stats');
        if (payoutRes.status === 'fulfilled') setPayoutStats(payoutRes.value.data);
        if (merchantsRes.status === 'fulfilled') setRecentMerchants(merchantsRes.value.data?.content || []);
      } catch (err) {
        setError(err.response?.data?.message || err.message || 'Failed to load dashboard');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const merchantColumns = [
    { key: 'username', label: 'Username' },
    { key: 'merchantId', label: 'Merchant ID', render: v => (v ? String(v).slice(0, 8) + '...' : '—') },
  ];

  return (
    <>
      <PageHeader title="Admin dashboard" subtitle="Platform overview and insights" />
      <ErrorMessage message={error} onDismiss={() => setError('')} />

      {loading ? (
        <p>Loading dashboard...</p>
      ) : (
        <>
          <section className={styles.dashboardSection}>
            <h3>Platform overview</h3>
            <div className={styles.statGrid}>
              {platformStats && (
                <>
                  <StatCard label="Total merchants" value={platformStats.totalMerchants} />
                  <StatCard label="Total vendors" value={platformStats.totalVendors} />
                  <StatCard label="Total wallets" value={platformStats.totalWallets} />
                  <StatCard label="Frozen wallets" value={platformStats.frozenWallets} sub={platformStats.frozenWallets > 0 ? 'Review in Freeze wallet' : ''} />
                  <StatCard label="Ledger entries" value={platformStats.totalLedgerEntries} />
                </>
              )}
            </div>
            {!platformStats && !error && <p className={styles.muted}>Platform stats could not be loaded.</p>}
          </section>

          <section className={styles.dashboardSection}>
            <h3>Payout insights</h3>
            <div className={styles.statGrid}>
              {payoutStats && (
                <>
                  <StatCard label="Total payouts" value={payoutStats.totalPayouts} />
                  <StatCard label="Settled" value={payoutStats.settledCount} />
                  <StatCard label="Processing" value={payoutStats.processingCount} />
                  <StatCard label="Failed" value={payoutStats.failedCount} />
                  <StatCard label="Total settled amount" value={payoutStats.totalSettledAmount != null ? Number(payoutStats.totalSettledAmount).toFixed(2) : '0'} sub="All time" />
                </>
              )}
            </div>
            {!payoutStats && !error && <p className={styles.muted}>Payout stats could not be loaded.</p>}
          </section>

          <section className={styles.dashboardSection}>
            <h3>Recent merchants</h3>
            <div className={styles.card}>
              <DataTable columns={merchantColumns} data={recentMerchants} emptyMessage="No merchants yet" />
              <p className={styles.muted} style={{ marginTop: '0.75rem' }}>
                <Link to="/admin/merchants">View all merchants →</Link>
              </p>
            </div>
          </section>

          <section className={styles.dashboardSection}>
            <h3>Quick actions</h3>
            <div className={styles.quickLinks}>
              <Link to="/admin/merchants">Merchants</Link>
              <Link to="/admin/wallets">Wallet search</Link>
              <Link to="/admin/transactions">Transaction explorer</Link>
              <Link to="/admin/freeze">Freeze / unfreeze wallet</Link>
              <Link to="/admin/settlement">Settlement report</Link>
            </div>
          </section>
        </>
      )}
    </>
  );
}
