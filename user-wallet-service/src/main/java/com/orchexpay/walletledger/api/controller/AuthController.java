package com.orchexpay.walletledger.api.controller;

import com.orchexpay.walletledger.api.dto.LoginRequest;
import com.orchexpay.walletledger.api.dto.LoginResponse;
import com.orchexpay.walletledger.application.exception.InvalidCredentialsException;
import com.orchexpay.walletledger.application.exception.UserNotFoundException;
import com.orchexpay.walletledger.application.usecase.GetUserByUsernameUseCase;
import com.orchexpay.walletledger.domain.model.User;
import com.orchexpay.walletledger.infrastructure.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final GetUserByUsernameUseCase getUserByUsernameUseCase;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user;
        try {
            user = getUserByUsernameUseCase.execute(request.getUsername());
        } catch (UserNotFoundException e) {
            throw new InvalidCredentialsException();
        }
        if (!user.isActive()) {
            throw new InvalidCredentialsException();
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        List<String> roles = user.getRoles().stream().map(Enum::name).collect(Collectors.toList());
        String token = jwtService.generateToken(user.getUsername(), roles, user.getMerchantId());
        long expiresIn = jwtService.getExpirationSeconds();
        return ResponseEntity.ok(LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresInSeconds(expiresIn)
                .build());
    }
}
