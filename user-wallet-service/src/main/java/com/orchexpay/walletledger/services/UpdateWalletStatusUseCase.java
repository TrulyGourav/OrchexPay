package com.orchexpay.walletledger.services;

import com.orchexpay.walletledger.exceptions.WalletNotFoundException;
import com.orchexpay.walletledger.repositories.WalletRepository;
import com.orchexpay.walletledger.models.Wallet;
import com.orchexpay.walletledger.enums.WalletStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateWalletStatusUseCase {

    private final WalletRepository walletRepository;

    @Transactional
    public Wallet execute(UUID walletId, WalletStatus status) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));
        wallet.setStatus(status);
        wallet.setUpdatedAt(Instant.now());
        return walletRepository.save(wallet);
    }
}
