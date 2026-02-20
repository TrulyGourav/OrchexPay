package com.orchexpay.walletledger.repositories;

import com.orchexpay.walletledger.models.VendorBankDetails;

import java.util.Optional;
import java.util.UUID;

public interface VendorBankDetailsRepository {

    Optional<VendorBankDetails> findByUserId(UUID userId);

    VendorBankDetails save(VendorBankDetails details);
}
