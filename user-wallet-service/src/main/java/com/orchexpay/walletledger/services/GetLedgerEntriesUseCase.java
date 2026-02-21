package com.orchexpay.walletledger.services;

import com.orchexpay.walletledger.repositories.LedgerEntryRepository;
import com.orchexpay.walletledger.models.LedgerEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Returns paginated, filterable ledger entries by wallet and/or merchant.
 * One of walletId or merchantId must be provided.
 */
@Service
@RequiredArgsConstructor
public class GetLedgerEntriesUseCase {

    private final LedgerEntryRepository ledgerEntryRepository;

    @Transactional(readOnly = true)
    public Page<LedgerEntry> execute(EntriesFilter filter, Pageable pageable) {
        if (filter.getWalletId() == null && filter.getMerchantId() == null) {
            throw new IllegalArgumentException("Either walletId or merchantId must be provided");
        }
        return ledgerEntryRepository.findFiltered(filter, pageable);
    }
}
