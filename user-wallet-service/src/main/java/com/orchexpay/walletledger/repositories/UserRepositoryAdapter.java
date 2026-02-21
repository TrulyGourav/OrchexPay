package com.orchexpay.walletledger.repositories;

import com.orchexpay.walletledger.models.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final JpaUserRepository jpaUserRepository;

    @Override
    public User save(User user) {
        if (user.getId() == null) user.setId(UUID.randomUUID());
        user.ensureDefaults();
        return jpaUserRepository.save(user);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaUserRepository.findById(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaUserRepository.findByUsername(username);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaUserRepository.existsByUsername(username);
    }

    @Override
    public Page<User> findMerchantUsers(Pageable pageable) {
        return jpaUserRepository.findMerchantUsers(pageable);
    }

    @Override
    public List<User> findVendorUsersByMerchantId(UUID merchantId) {
        return jpaUserRepository.findVendorUsersByMerchantId(merchantId);
    }

    @Override
    public long countMerchantUsers() {
        return jpaUserRepository.countMerchantUsers();
    }

    @Override
    public long countVendorUsers() {
        return jpaUserRepository.countVendorUsers();
    }
}
