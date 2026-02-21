import { walletClient } from './config';

export const usersApi = {
  getMe() {
    return walletClient.get('/api/v1/users/me');
  },
  createUser(body) {
    return walletClient.post('/api/v1/users', body);
  },
  getMyBankDetails() {
    return walletClient.get('/api/v1/users/me/bank-details');
  },
  updateMyBankDetails(body) {
    return walletClient.put('/api/v1/users/me/bank-details', body);
  },
};
