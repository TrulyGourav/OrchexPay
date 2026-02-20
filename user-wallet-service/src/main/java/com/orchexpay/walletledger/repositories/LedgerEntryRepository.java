package com.orchexpay.walletledger.repositories;

import com.orchexpay.walletledger.services.EntriesFilter;
import com.orchexpay.walletledger.models.LedgerEntry;
import com.orchexpay.walletledger.enums.ReferenceType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Port for ledger entry persistence. Balance is derived only from CONFIRMED entries.
 * Balance = SUM(CONFIRMED credits) − SUM(CONFIRMED debits).
 */
public interface LedgerEntryRepository {

    LedgerEntry save(LedgerEntry entry);

    Optional<LedgerEntry> findById(UUID id);

    Page<LedgerEntry> findFiltered(EntriesFilter filter, Pageable pageable);

    /**
     * Computes current balance for a wallet: SUM(CONFIRMED credits) − SUM(CONFIRMED debits).
     */
    BigDecimal computeBalance(UUID walletId);

    List<LedgerEntry> findByWalletId(UUID walletId, int limit, int offset);

    Optional<LedgerEntry> findByReferenceId(UUID walletId, String referenceId);

    Optional<LedgerEntry> findByWalletIdAndReferenceIdAndReferenceType(UUID walletId, String referenceId, ReferenceType referenceType);

    BigDecimal sumConfirmedCreditsByWalletId(UUID walletId);

    BigDecimal sumConfirmedDebitsByWalletIdAndReferenceType(UUID walletId, ReferenceType referenceType);

    long count();
}
