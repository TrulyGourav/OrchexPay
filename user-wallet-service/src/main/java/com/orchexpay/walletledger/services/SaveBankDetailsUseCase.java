package com.orchexpay.walletledger.services;

import com.orchexpay.walletledger.repositories.VendorBankDetailsRepository;
import com.orchexpay.walletledger.models.VendorBankDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SaveBankDetailsUseCase {

    private final VendorBankDetailsRepository repository;

    @Transactional
    public VendorBankDetails execute(UUID userId, String accountNumber, String ifscCode, String beneficiaryName) {
        Instant now = Instant.now();
        VendorBankDetails details = repository.findByUserId(userId)
                .orElse(VendorBankDetails.builder()
                        .userId(userId)
                        .createdAt(now)
                        .build());
        details.setAccountNumber(accountNumber);
        details.setIfscCode(ifscCode != null ? ifscCode : "");
        details.setBeneficiaryName(beneficiaryName);
        details.setUpdatedAt(now);
        return repository.save(details);
    }
}
