import { walletClient } from './config';

export const authApi = {
  login(username, password) {
    return walletClient.post('/auth/login', { username, password });
  },
};
