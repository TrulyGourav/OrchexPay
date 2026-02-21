package com.orchexpay.walletledger.repositories;

import com.orchexpay.walletledger.services.EntriesFilter;
import com.orchexpay.walletledger.models.LedgerEntry;
import com.orchexpay.walletledger.enums.ReferenceType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LedgerEntryRepositoryAdapter implements LedgerEntryRepository {

    private final JpaLedgerEntryRepository jpaLedgerEntryRepository;

    @Override
    public LedgerEntry save(LedgerEntry entry) {
        if (entry.getId() == null) entry.setId(UUID.randomUUID());
        return jpaLedgerEntryRepository.save(entry);
    }

    @Override
    public Optional<LedgerEntry> findById(UUID id) {
        return jpaLedgerEntryRepository.findById(id);
    }

    @Override
    public BigDecimal computeBalance(UUID walletId) {
        var sum = jpaLedgerEntryRepository.sumConfirmedBalanceByWalletId(walletId);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Override
    public List<LedgerEntry> findByWalletId(UUID walletId, int limit, int offset) {
        int page = limit > 0 ? offset / limit : 0;
        return jpaLedgerEntryRepository.findByWalletIdOrderByCreatedAtDesc(walletId, PageRequest.of(page, limit));
    }

    @Override
    public Optional<LedgerEntry> findByReferenceId(UUID walletId, String referenceId) {
        return jpaLedgerEntryRepository.findByWalletIdAndReferenceId(walletId, referenceId);
    }

    @Override
    public Optional<LedgerEntry> findByWalletIdAndReferenceIdAndReferenceType(UUID walletId, String referenceId, ReferenceType referenceType) {
        return jpaLedgerEntryRepository.findByWalletIdAndReferenceIdAndReferenceType(walletId, referenceId, referenceType);
    }

    @Override
    public BigDecimal sumConfirmedCreditsByWalletId(UUID walletId) {
        var sum = jpaLedgerEntryRepository.sumConfirmedCreditsByWalletId(walletId);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal sumConfirmedDebitsByWalletIdAndReferenceType(UUID walletId, ReferenceType referenceType) {
        var sum = jpaLedgerEntryRepository.sumConfirmedDebitsByWalletIdAndReferenceType(walletId, referenceType);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Override
    public Page<LedgerEntry> findFiltered(EntriesFilter filter, Pageable pageable) {
        Specification<LedgerEntry> spec = Specification.where(null);
        if (filter.getWalletId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("walletId"), filter.getWalletId()));
        }
        if (filter.getMerchantId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("merchantId"), filter.getMerchantId()));
        }
        if (filter.getFrom() != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getFrom()));
        }
        if (filter.getTo() != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), filter.getTo()));
        }
        if (filter.getMinAmount() != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("amountValue"), filter.getMinAmount()));
        }
        if (filter.getMaxAmount() != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("amountValue"), filter.getMaxAmount()));
        }
        if (filter.getReferenceType() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("referenceType"), filter.getReferenceType()));
        }
        if (filter.getStatus() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), filter.getStatus()));
        }
        return jpaLedgerEntryRepository.findAll(spec, pageable);
    }

    @Override
    public long count() {
        return jpaLedgerEntryRepository.count();
    }
}
