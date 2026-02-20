package com.orchexpay.walletledger.services;

import com.orchexpay.walletledger.repositories.LedgerEntryRepository;
import com.orchexpay.walletledger.repositories.UserRepository;
import com.orchexpay.walletledger.repositories.WalletRepository;
import com.orchexpay.walletledger.enums.WalletStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetAdminStatsUseCase {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    @Transactional(readOnly = true)
    public AdminStatsResult execute() {
        long totalMerchants = userRepository.countMerchantUsers();
        long totalVendors = userRepository.countVendorUsers();
        long totalWallets = walletRepository.count();
        long frozenWallets = walletRepository.countByStatus(WalletStatus.FROZEN);
        long totalLedgerEntries = ledgerEntryRepository.count();
        return new AdminStatsResult(totalMerchants, totalVendors, totalWallets, frozenWallets, totalLedgerEntries);
    }

    public record AdminStatsResult(long totalMerchants, long totalVendors, long totalWallets, long frozenWallets, long totalLedgerEntries) {}
}
