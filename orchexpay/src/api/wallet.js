import { walletClient, setIdempotencyKey } from './config';

export const getWalletApi = {
  getWallet(walletId) {
    return walletClient.get(`/api/v1/wallets/${walletId}`);
  },
  getWalletByType(merchantId, currencyCode, walletType, vendorUserId = null) {
    let url = `/api/v1/merchants/${merchantId}/wallets/by-type?currencyCode=${currencyCode}&walletType=${walletType}`;
    if (vendorUserId) url += `&vendorUserId=${vendorUserId}`;
    return walletClient.get(url);
  },
  getSettlement(merchantId, currencyCode = 'INR') {
    return walletClient.get(`/api/v1/merchants/${merchantId}/settlement`, { params: { currencyCode } });
  },
  creditWallet(walletId, body, idempotencyKey) {
    setIdempotencyKey(walletClient, idempotencyKey);
    return walletClient.post(`/api/v1/wallets/${walletId}/credit`, body);
  },
  transfer(body, idempotencyKey) {
    setIdempotencyKey(walletClient, idempotencyKey);
    return walletClient.post('/api/v1/transfers', body);
  },
  freezeWallet(walletId) {
    return walletClient.post(`/api/v1/wallets/${walletId}/freeze`);
  },
  unfreezeWallet(walletId) {
    return walletClient.post(`/api/v1/wallets/${walletId}/unfreeze`);
  },
};
