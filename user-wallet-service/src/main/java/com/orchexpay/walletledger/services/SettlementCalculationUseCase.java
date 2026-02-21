package com.orchexpay.walletledger.services;

import com.orchexpay.walletledger.repositories.LedgerEntryRepository;
import com.orchexpay.walletledger.repositories.WalletRepository;
import com.orchexpay.walletledger.enums.WalletType;
import com.orchexpay.walletledger.enums.ReferenceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Daily settlement calculation for a merchant's ESCROW wallet.
 * Merchant Real Bank Expected Balance =
 *   SUM(confirmed ESCROW credits) - SUM(confirmed ESCROW debits for payouts) - SUM(confirmed ESCROW debits for refunds).
 * LedgerX net = computeBalance(escrowWalletId).
 * If mismatch → reconciliation issue (data integrity).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementCalculationUseCase {

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    @Transactional(readOnly = true)
    public SettlementResult execute(UUID merchantId, String currencyCode) {
        var escrowWallet = walletRepository.findByMerchantIdAndCurrencyCodeAndWalletTypeAndVendorUserId(
                merchantId, currencyCode, WalletType.ESCROW, null);
        if (escrowWallet.isEmpty()) {
            return SettlementResult.noEscrowWallet(merchantId, currencyCode);
        }
        UUID walletId = escrowWallet.get().getId();
        BigDecimal confirmedCredits = ledgerEntryRepository.sumConfirmedCreditsByWalletId(walletId);
        BigDecimal payoutDebits = ledgerEntryRepository.sumConfirmedDebitsByWalletIdAndReferenceType(walletId, ReferenceType.PAYOUT);
        BigDecimal refundDebits = ledgerEntryRepository.sumConfirmedDebitsByWalletIdAndReferenceType(walletId, ReferenceType.REFUND);
        BigDecimal ledgerNet = ledgerEntryRepository.computeBalance(walletId);
        BigDecimal expected = confirmedCredits.subtract(payoutDebits).subtract(refundDebits);
        boolean reconciled = ledgerNet.compareTo(expected) == 0;
        if (!reconciled) {
            log.warn("Settlement mismatch merchant {} currency {}: ledgerNet={} expected={}",
                    merchantId, currencyCode, ledgerNet, expected);
        }
        return SettlementResult.builder()
                .merchantId(merchantId)
                .currencyCode(currencyCode)
                .escrowWalletId(walletId)
                .totalConfirmedEscrowCredits(confirmedCredits)
                .totalPayoutDebits(payoutDebits)
                .totalRefundDebits(refundDebits)
                .expectedBalance(expected)
                .ledgerNetBalance(ledgerNet)
                .reconciled(reconciled)
                .build();
    }

    public record SettlementResult(
            UUID merchantId,
            String currencyCode,
            UUID escrowWalletId,
            BigDecimal totalConfirmedEscrowCredits,
            BigDecimal totalPayoutDebits,
            BigDecimal totalRefundDebits,
            BigDecimal expectedBalance,
            BigDecimal ledgerNetBalance,
            boolean reconciled
    ) {
        public static SettlementResult noEscrowWallet(UUID merchantId, String currencyCode) {
            return new SettlementResult(merchantId, currencyCode, null,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, true);
        }

        public static SettlementResultBuilder builder() {
            return new SettlementResultBuilder();
        }

        public static class SettlementResultBuilder {
            private UUID merchantId;
            private String currencyCode;
            private UUID escrowWalletId;
            private BigDecimal totalConfirmedEscrowCredits;
            private BigDecimal totalPayoutDebits;
            private BigDecimal totalRefundDebits;
            private BigDecimal expectedBalance;
            private BigDecimal ledgerNetBalance;
            private boolean reconciled;

            public SettlementResultBuilder merchantId(UUID merchantId) { this.merchantId = merchantId; return this; }
            public SettlementResultBuilder currencyCode(String currencyCode) { this.currencyCode = currencyCode; return this; }
            public SettlementResultBuilder escrowWalletId(UUID escrowWalletId) { this.escrowWalletId = escrowWalletId; return this; }
            public SettlementResultBuilder totalConfirmedEscrowCredits(BigDecimal v) { this.totalConfirmedEscrowCredits = v; return this; }
            public SettlementResultBuilder totalPayoutDebits(BigDecimal v) { this.totalPayoutDebits = v; return this; }
            public SettlementResultBuilder totalRefundDebits(BigDecimal v) { this.totalRefundDebits = v; return this; }
            public SettlementResultBuilder expectedBalance(BigDecimal v) { this.expectedBalance = v; return this; }
            public SettlementResultBuilder ledgerNetBalance(BigDecimal v) { this.ledgerNetBalance = v; return this; }
            public SettlementResultBuilder reconciled(boolean r) { this.reconciled = r; return this; }

            public SettlementResult build() {
                return new SettlementResult(merchantId, currencyCode, escrowWalletId,
                        totalConfirmedEscrowCredits, totalPayoutDebits, totalRefundDebits,
                        expectedBalance, ledgerNetBalance, reconciled);
            }
        }
    }
}
