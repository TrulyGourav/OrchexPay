import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { usersApi } from '../../api/users';
import { merchantsApi } from '../../api/merchants';
import { payoutApi } from '../../api/payout';
import PageHeader from '../../components/PageHeader';
import ErrorMessage from '../../components/ErrorMessage';
import styles from '../AdminShared.module.css';

export default function OrderCompletion() {
  const { user } = useAuth();
  const merchantId = user?.merchantId;

  const [tab, setTab] = useState('payment');
  const [profile, setProfile] = useState(null);
  const [vendors, setVendors] = useState([]);
  const [loadingProfile, setLoadingProfile] = useState(true);
  const [profileError, setProfileError] = useState('');

  // Payment success tab
  const [paymentVendorId, setPaymentVendorId] = useState('');
  const [orderId, setOrderId] = useState('');
  const [amount, setAmount] = useState('');
  const [currencyCode, setCurrencyCode] = useState('INR');

  // Order complete tab
  const [completeVendorId, setCompleteVendorId] = useState('');
  const [pendingOrders, setPendingOrders] = useState([]);
  const [selectedPendingOrder, setSelectedPendingOrder] = useState(null);
  const [loadingPending, setLoadingPending] = useState(false);

  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!merchantId) {
      setLoadingProfile(false);
      return;
    }
    setLoadingProfile(true);
    setProfileError('');
    Promise.all([usersApi.getMe(), merchantsApi.listVendors(merchantId)])
      .then(([meRes, vendorsRes]) => {
        setProfile(meRes.data);
        setVendors(Array.isArray(vendorsRes.data) ? vendorsRes.data : []);
      })
      .catch((err) => setProfileError(err.response?.data?.message || err.message || 'Failed to load'))
      .finally(() => setLoadingProfile(false));
  }, [merchantId]);

  useEffect(() => {
    if (tab === 'order' && merchantId && completeVendorId) {
      setLoadingPending(true);
      setPendingOrders([]);
      setSelectedPendingOrder(null);
      payoutApi
        .listPendingOrders(merchantId, completeVendorId)
        .then((res) => setPendingOrders(Array.isArray(res.data) ? res.data : []))
        .catch(() => setPendingOrders([]))
        .finally(() => setLoadingPending(false));
    } else if (!completeVendorId) {
      setPendingOrders([]);
      setSelectedPendingOrder(null);
    }
  }, [tab, merchantId, completeVendorId]);

  const runPaymentSuccess = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    if (!merchantId || !profile?.escrowWalletId) {
      setError('Escrow wallet not found. Refresh the page.');
      return;
    }
    if (!paymentVendorId) {
      setError('Please select a vendor.');
      return;
    }
    if (!orderId.trim()) {
      setError('Order ID is required.');
      return;
    }
    const num = parseFloat(amount);
    if (Number.isNaN(num) || num < 0.01) {
      setError('Amount must be at least 0.01');
      return;
    }
    setLoading(true);
    try {
      const res = await payoutApi.mockPaymentSuccess({
        merchantId,
        vendorId: paymentVendorId,
        orderId: orderId.trim(),
        amount: num,
        currencyCode: (currencyCode || 'INR').trim().toUpperCase().slice(0, 3),
        escrowWalletId: profile.escrowWalletId,
      });
      setMessage(res.data?.message || 'Escrow credited.');
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed');
    } finally {
      setLoading(false);
    }
  };

  const runOrderComplete = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    const vendor = vendors.find((v) => v.userId === completeVendorId);
    if (!merchantId || !profile?.escrowWalletId || !profile?.mainWalletId) {
      setError('Wallet IDs not found. Refresh the page.');
      return;
    }
    if (!vendor) {
      setError('Please select a vendor.');
      return;
    }
    if (!selectedPendingOrder) {
      setError('Please select an order to complete.');
      return;
    }
    setLoading(true);
    try {
      const res = await payoutApi.mockOrderComplete({
        merchantId,
        orderId: selectedPendingOrder.orderId,
        amount: selectedPendingOrder.amount,
        currencyCode: selectedPendingOrder.currencyCode || 'INR',
        vendorId: vendor.userId,
        escrowWalletId: profile.escrowWalletId,
        mainWalletId: profile.mainWalletId,
        vendorWalletId: vendor.vendorWalletId,
      });
      setMessage(res.data?.message || 'Order distributed.');
      setSelectedPendingOrder(null);
      setPendingOrders((prev) => prev.filter((p) => p.orderId !== selectedPendingOrder.orderId));
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed');
    } finally {
      setLoading(false);
    }
  };

  if (!merchantId) {
    return (
      <>
        <PageHeader title="Complete order" />
        <p>Merchant ID not found.</p>
      </>
    );
  }

  if (loadingProfile) {
    return (
      <>
        <PageHeader title="Complete order" />
        <p>Loading...</p>
      </>
    );
  }

  if (profileError || !profile) {
    return (
      <>
        <PageHeader title="Complete order" />
        <ErrorMessage message={profileError || 'Could not load wallet info'} onDismiss={() => setProfileError('')} />
      </>
    );
  }

  return (
    <>
      <PageHeader title="Order completion" subtitle="Payment success or order complete (split) for your merchant" />
      <ErrorMessage message={error} onDismiss={() => setError('')} />
      {message && <p style={{ color: 'green', marginBottom: '1rem' }}>{message}</p>}
      <div style={{ marginBottom: '1rem' }}>
        <button type="button" className={styles.btn} style={{ marginRight: '0.5rem' }} onClick={() => setTab('payment')}>
          Payment success
        </button>
        <button type="button" className={styles.btn} onClick={() => setTab('order')}>
          Order complete (split)
        </button>
      </div>
      <div className={styles.card}>
        {tab === 'payment' ? (
          <form onSubmit={runPaymentSuccess}>
            <p className={styles.muted}>Credit escrow for an order and associate it with a vendor (idempotent by orderId).</p>
            <div style={{ marginBottom: '0.75rem' }}>
              <label className={styles.label}>Vendor</label>
              <select
                value={paymentVendorId}
                onChange={(e) => setPaymentVendorId(e.target.value)}
                className={styles.input}
                style={{ width: '100%', maxWidth: 320 }}
                required
              >
                <option value="">Select vendor</option>
                {vendors.map((v) => (
                  <option key={v.userId} value={v.userId}>
                    {v.username}
                  </option>
                ))}
              </select>
              {vendors.length === 0 && <p className={styles.muted} style={{ marginTop: 4 }}>No vendors under this merchant. Add vendors first.</p>}
            </div>
            <div style={{ marginBottom: '0.75rem' }}>
              <label className={styles.label}>Order ID</label>
              <input type="text" value={orderId} onChange={(e) => setOrderId(e.target.value)} className={styles.input} style={{ width: '100%' }} placeholder="e.g. order-001" required />
            </div>
            <div style={{ marginBottom: '0.75rem' }}>
              <label className={styles.label}>Amount</label>
              <input type="number" step="0.01" min="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} className={styles.input} style={{ width: 120 }} required />
            </div>
            <div style={{ marginBottom: '0.75rem' }}>
              <label className={styles.label}>Currency</label>
              <input type="text" value={currencyCode} onChange={(e) => setCurrencyCode(e.target.value.slice(0, 3).toUpperCase())} className={styles.input} style={{ width: 80 }} maxLength={3} />
            </div>
            <div style={{ marginBottom: '0.75rem' }}>
              <label className={styles.label}>Escrow wallet ID</label>
              <input type="text" value={profile.escrowWalletId || ''} readOnly className={styles.input} style={{ width: '100%', background: '#f5f5f5' }} />
            </div>
            <button type="submit" className={styles.btn} disabled={loading || vendors.length === 0}>
              {loading ? 'Running...' : 'Credit escrow'}
            </button>
          </form>
        ) : (
          <form onSubmit={runOrderComplete}>
            <p className={styles.muted}>Distribute order amount: escrow → main + vendor by commission. Select a vendor and an order that had payment success but not yet completed.</p>
            <div style={{ marginBottom: '0.75rem' }}>
              <label className={styles.label}>Vendor</label>
              <select
                value={completeVendorId}
                onChange={(e) => {
                  setCompleteVendorId(e.target.value);
                  setSelectedPendingOrder(null);
                }}
                className={styles.input}
                style={{ width: '100%', maxWidth: 320 }}
              >
                <option value="">Select vendor</option>
                {vendors.map((v) => (
                  <option key={v.userId} value={v.userId}>
                    {v.username}
                  </option>
                ))}
              </select>
            </div>
            {completeVendorId && (
              <>
                <div style={{ marginBottom: '0.75rem' }}>
                  <label className={styles.label}>Order (pending completion)</label>
                  <select
                    value={selectedPendingOrder?.orderId ?? ''}
                    onChange={(e) => {
                      const o = pendingOrders.find((p) => p.orderId === e.target.value);
                      setSelectedPendingOrder(o || null);
                    }}
                    className={styles.input}
                    style={{ width: '100%', maxWidth: 320 }}
                  >
                    <option value="">Select order</option>
                    {pendingOrders.map((p) => (
                      <option key={p.orderId} value={p.orderId}>
                        {p.orderId} — {p.amount} {p.currencyCode}
                      </option>
                    ))}
                  </select>
                  {loadingPending && <p className={styles.muted} style={{ marginTop: 4 }}>Loading...</p>}
                  {!loadingPending && pendingOrders.length === 0 && <p className={styles.muted} style={{ marginTop: 4 }}>No orders pending completion for this vendor.</p>}
                </div>
                {selectedPendingOrder && (
                  <>
                    <div style={{ marginBottom: '0.75rem' }}>
                      <label className={styles.label}>Amount</label>
                      <input type="text" value={`${selectedPendingOrder.amount} ${selectedPendingOrder.currencyCode || 'INR'}`} readOnly className={styles.input} style={{ width: 140, background: '#f5f5f5' }} />
                    </div>
                    <div style={{ marginBottom: '0.75rem' }}>
                      <label className={styles.label}>Vendor user ID</label>
                      <input type="text" value={vendors.find((v) => v.userId === completeVendorId)?.userId || ''} readOnly className={styles.input} style={{ width: '100%', background: '#f5f5f5' }} />
                    </div>
                    <div style={{ marginBottom: '0.75rem' }}>
                      <label className={styles.label}>Vendor wallet ID</label>
                      <input type="text" value={vendors.find((v) => v.userId === completeVendorId)?.vendorWalletId || ''} readOnly className={styles.input} style={{ width: '100%', background: '#f5f5f5' }} />
                    </div>
                  </>
                )}
              </>
            )}
            <div style={{ marginBottom: '0.75rem' }}>
              <label className={styles.label}>Escrow wallet ID</label>
              <input type="text" value={profile.escrowWalletId || ''} readOnly className={styles.input} style={{ width: '100%', background: '#f5f5f5' }} />
            </div>
            <div style={{ marginBottom: '0.75rem' }}>
              <label className={styles.label}>Main wallet ID</label>
              <input type="text" value={profile.mainWalletId || ''} readOnly className={styles.input} style={{ width: '100%', background: '#f5f5f5' }} />
            </div>
            <button type="submit" className={styles.btn} disabled={loading || !selectedPendingOrder}>
              {loading ? 'Running...' : 'Complete order (split)'}
            </button>
          </form>
        )}
      </div>
    </>
  );
}
