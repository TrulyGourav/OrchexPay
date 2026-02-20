package com.orchexpay.walletledger.services;

import com.orchexpay.walletledger.exceptions.WalletNotFoundException;
import com.orchexpay.walletledger.repositories.LedgerEntryRepository;
import com.orchexpay.walletledger.repositories.WalletRepository;
import com.orchexpay.walletledger.models.Wallet;
import com.orchexpay.walletledger.models.Money;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Returns wallet with balance derived from ledger (never stored).
 */
@Service
@RequiredArgsConstructor
public class GetWalletUseCase {

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    @Transactional(readOnly = true)
    public WalletWithBalance execute(UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));
        BigDecimal balanceAmount = ledgerEntryRepository.computeBalance(walletId);
        Money balance = Money.of(balanceAmount, wallet.getCurrency().getCode());
        return new WalletWithBalance(wallet, wallet.balance(balance));
    }

    public record WalletWithBalance(Wallet wallet, Money balance) {}
}
