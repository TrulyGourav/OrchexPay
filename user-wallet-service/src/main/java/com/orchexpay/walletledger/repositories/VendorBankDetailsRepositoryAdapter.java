package com.orchexpay.walletledger.repositories;

import com.orchexpay.walletledger.models.VendorBankDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VendorBankDetailsRepositoryAdapter implements VendorBankDetailsRepository {

    private final JpaVendorBankDetailsRepository jpaRepository;

    @Override
    public Optional<VendorBankDetails> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId);
    }

    @Override
    public VendorBankDetails save(VendorBankDetails details) {
        return jpaRepository.save(details);
    }
}
