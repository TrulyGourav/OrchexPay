import { walletClient } from './config';

export const merchantsApi = {
  listMerchants(params = {}) {
    return walletClient.get('/api/v1/merchants', { params: { page: 0, size: 20, ...params } });
  },
  listVendors(merchantId) {
    return walletClient.get(`/api/v1/merchants/${merchantId}/vendors`);
  },
  addVendor(merchantId, body) {
    return walletClient.post(`/api/v1/merchants/${merchantId}/vendors`, body);
  },
};
