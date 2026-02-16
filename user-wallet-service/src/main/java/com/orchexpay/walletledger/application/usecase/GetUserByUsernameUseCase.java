package com.orchexpay.walletledger.application.usecase;

import com.orchexpay.walletledger.application.exception.UserNotFoundException;
import com.orchexpay.walletledger.application.port.UserRepository;
import com.orchexpay.walletledger.domain.model.User;
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
