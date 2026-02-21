/**
 * Decode JWT payload without verification (frontend only; backend validates).
 * Returns { sub, roles, merchantId } or null.
 */
export function decodeJwtPayload(token) {
  if (!token || typeof token !== 'string') return null;
  try {
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const json = atob(base64);
    const payload = JSON.parse(json);
    return {
      sub: payload.sub,
      roles: payload.roles || [],
      merchantId: payload.merchantId || null,
    };
  } catch {
    return null;
  }
}
