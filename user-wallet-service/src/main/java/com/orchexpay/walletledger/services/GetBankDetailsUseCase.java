package com.orchexpay.walletledger.services;

import com.orchexpay.walletledger.repositories.VendorBankDetailsRepository;
import com.orchexpay.walletledger.models.VendorBankDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetBankDetailsUseCase {

    private final VendorBankDetailsRepository repository;

    @Transactional(readOnly = true)
    public Optional<VendorBankDetails> execute(UUID userId) {
        return repository.findByUserId(userId);
    }
}
