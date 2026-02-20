package com.orchexpay.walletledger.services;

import com.orchexpay.walletledger.exceptions.UserNotFoundException;
import com.orchexpay.walletledger.repositories.UserRepository;
import com.orchexpay.walletledger.models.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetUserByUsernameUseCase {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User execute(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
    }
}
