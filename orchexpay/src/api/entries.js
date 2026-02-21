import { walletClient } from './config';

/**
 * GET /api/v1/entries with optional walletId, merchantId, and filters.
 * @param {Object} params - { walletId?, merchantId?, from?, to?, minAmount?, maxAmount?, referenceType?, status?, page?, size?, sort? }
 */
export function getEntries(params = {}) {
  const q = { page: params.page ?? 0, size: params.size ?? 20, sort: params.sort ?? 'createdAt,desc' };
  if (params.walletId) q.walletId = params.walletId;
  if (params.merchantId) q.merchantId = params.merchantId;
  if (params.from) q.from = params.from;
  if (params.to) q.to = params.to;
  if (params.minAmount != null) q.minAmount = params.minAmount;
  if (params.maxAmount != null) q.maxAmount = params.maxAmount;
  if (params.referenceType) q.referenceType = params.referenceType;
  if (params.status) q.status = params.status;
  return walletClient.get('/api/v1/entries', { params: q });
}
