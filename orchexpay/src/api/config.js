import axios from 'axios';

const WALLET_BASE = import.meta.env.VITE_WALLET_API || '';
const PAYOUT_BASE = import.meta.env.VITE_PAYOUT_API || '/payout-api';

export const walletClient = axios.create({
  baseURL: WALLET_BASE,
  headers: { 'Content-Type': 'application/json' },
});

export const payoutClient = axios.create({
  baseURL: PAYOUT_BASE,
  headers: { 'Content-Type': 'application/json' },
});

export function setWalletAuth(token) {
  walletClient.defaults.headers.common['Authorization'] = token ? `Bearer ${token}` : '';
}

export function setPayoutAuth(token) {
  payoutClient.defaults.headers.common['Authorization'] = token ? `Bearer ${token}` : '';
}

function handleResponseError(error, on401) {
  if (error.response && error.response.status === 401 && on401) on401();
  return Promise.reject(error);
}

const redirectToLogin = () => {
  if (typeof window !== 'undefined' && !window.__ledgerx_redirecting) {
    window.__ledgerx_redirecting = true;
    sessionStorage.removeItem('ledgerx_token');
    sessionStorage.removeItem('ledgerx_user');
    window.location.href = '/login';
  }
};

walletClient.interceptors.response.use(
  (r) => r,
  (e) => handleResponseError(e, redirectToLogin)
);

payoutClient.interceptors.response.use(
  (r) => r,
  (e) => handleResponseError(e, redirectToLogin)
);

export function setIdempotencyKey(client, key) {
  if (key) client.defaults.headers.common['Idempotency-Key'] = key;
}
