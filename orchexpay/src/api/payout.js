import { payoutClient, setIdempotencyKey } from './config';

export const payoutApi = {
  getCommission(merchantId) {
    return payoutClient.get(`/api/v1/merchants/${merchantId}/commission`);
  },
  /** Body: { commissionType: 'PERCENTAGE'|'FIXED_PLUS_PERCENTAGE', percentageValue: number, fixedAmount?: number, currencyCode?: string } */
  putCommission(merchantId, body) {
    return payoutClient.put(`/api/v1/merchants/${merchantId}/commission`, body);
  },
  listPayouts(params = {}) {
    return payoutClient.get('/api/v1/payouts', { params: { page: 0, size: 20, ...params } });
  },
  /** Pending orders (payment success done, split not done) for a vendor. Returns [{ orderId, amount, currencyCode, createdAt }]. */
  listPendingOrders(merchantId, vendorId) {
    return payoutClient.get('/api/v1/payouts/pending-orders', { params: { merchantId, vendorId } });
  },
  getPayout(payoutId) {
    return payoutClient.get(`/api/v1/payouts/${payoutId}`);
  },
  requestPayoutVendor(body, idempotencyKey) {
    setIdempotencyKey(payoutClient, idempotencyKey);
    return payoutClient.post('/api/v1/payouts/request', body);
  },
  createPayout(body, idempotencyKey) {
    setIdempotencyKey(payoutClient, idempotencyKey);
    return payoutClient.post('/api/v1/payouts', body);
  },
  confirmPayout(payoutId, idempotencyKey) {
    setIdempotencyKey(payoutClient, idempotencyKey);
    return payoutClient.post(`/api/v1/payouts/${payoutId}/confirm`, null);
  },
  reversePayout(payoutId, idempotencyKey) {
    setIdempotencyKey(payoutClient, idempotencyKey);
    return payoutClient.post(`/api/v1/payouts/${payoutId}/reverse`, null);
  },
  mockPaymentSuccess(body) {
    return payoutClient.post('/api/v1/mock/webhooks/payment-success', body);
  },
  mockOrderComplete(body) {
    return payoutClient.post('/api/v1/mock/webhooks/order-complete', body);
  },
};
