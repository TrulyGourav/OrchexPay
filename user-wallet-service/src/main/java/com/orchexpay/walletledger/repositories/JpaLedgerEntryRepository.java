package com.orchexpay.walletledger.repositories;

import com.orchexpay.walletledger.models.LedgerEntry;
import com.orchexpay.walletledger.enums.ReferenceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaLedgerEntryRepository extends JpaRepository<LedgerEntry, UUID>, JpaSpecificationExecutor<LedgerEntry> {

    List<LedgerEntry> findByWalletIdOrderByCreatedAtDesc(UUID walletId, org.springframework.data.domain.Pageable pageable);

    Optional<LedgerEntry> findByWalletIdAndReferenceId(UUID walletId, String referenceId);

    Optional<LedgerEntry> findByWalletIdAndReferenceIdAndReferenceType(UUID walletId, String referenceId, ReferenceType referenceType);

    /**
     * Balance = SUM(CONFIRMED credits) − SUM(CONFIRMED debits). PENDING and REVERSED are excluded.
     */
    @Query("""
            SELECT COALESCE(SUM(CASE WHEN e.type = 'CREDIT' THEN e.amountValue ELSE -e.amountValue END), 0)
            FROM LedgerEntry e WHERE e.walletId = :walletId AND e.status = 'CONFIRMED'
            """)
    BigDecimal sumConfirmedBalanceByWalletId(@Param("walletId") UUID walletId);

    /** Legacy: sum all entries (pre-status). Use sumConfirmedBalanceByWalletId for correct balance. */
    @Query("""
            SELECT COALESCE(SUM(CASE WHEN e.type = 'CREDIT' THEN e.amountValue ELSE -e.amountValue END), 0)
            FROM LedgerEntry e WHERE e.walletId = :walletId
            """)
    BigDecimal sumBalanceByWalletId(@Param("walletId") UUID walletId);

    @Query("""
            SELECT COALESCE(SUM(e.amountValue), 0) FROM LedgerEntry e
            WHERE e.walletId = :walletId AND e.type = 'CREDIT' AND e.status = 'CONFIRMED'
            """)
    BigDecimal sumConfirmedCreditsByWalletId(@Param("walletId") UUID walletId);

    @Query("""
            SELECT COALESCE(SUM(e.amountValue), 0) FROM LedgerEntry e
            WHERE e.walletId = :walletId AND e.type = 'DEBIT' AND e.status = 'CONFIRMED' AND e.referenceType = :refType
            """)
    BigDecimal sumConfirmedDebitsByWalletIdAndReferenceType(@Param("walletId") UUID walletId, @Param("refType") ReferenceType refType);
}
