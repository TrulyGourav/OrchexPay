import { walletClient } from './config';
import { payoutClient } from './config';

/**
 * Admin-only stats from user-wallet-service.
 * GET /api/v1/admin/stats → { totalMerchants, totalVendors, totalWallets, frozenWallets, totalLedgerEntries }
 */
export function getAdminStats() {
  return walletClient.get('/api/v1/admin/stats');
}

/**
 * Admin-only payout stats from payout-orchestrator.
 * GET /api/v1/payouts/stats → { totalPayouts, createdCount, processingCount, settledCount, failedCount, totalSettledAmount }
 */
export function getPayoutStats() {
  return payoutClient.get('/api/v1/payouts/stats');
}
