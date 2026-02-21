/**
 * Centralized API error message for user display.
 * 401/403 are handled by interceptors (redirect); use this for inline form/table errors.
 * @param {Error} err - Axios error or similar
 * @returns {string} User-friendly message
 */
export function getApiErrorMessage(err) {
  if (!err) return '';
  const status = err.response?.status;
  const data = err.response?.data;
  const msg = data?.message || data?.error || err.message;

  if (status === 422) {
    if (msg && /insufficient|balance/i.test(msg)) return 'Insufficient balance.';
    return msg || 'Validation failed.';
  }
  if (status === 409) return 'Duplicate request (idempotency). Please do not retry.';
  if (status === 400) return msg || 'Invalid request.';
  if (status === 404) return msg || 'Not found.';
  if (status >= 500) return msg || 'Server error. Please try again later.';
  if (err.code === 'ERR_NETWORK') return 'Network error. Check connection.';

  return msg || 'Something went wrong.';
}
