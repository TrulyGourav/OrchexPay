package com.orchexpay.walletledger.controllers;

import com.orchexpay.walletledger.dtos.CreateUserRequest;
import com.orchexpay.walletledger.dtos.UserResponse;
import com.orchexpay.walletledger.mappers.UserMapper;
import com.orchexpay.walletledger.services.CreateUserResult;
import com.orchexpay.walletledger.services.CreateUserUseCase;
import com.orchexpay.walletledger.dtos.BankDetailsRequest;
import com.orchexpay.walletledger.dtos.BankDetailsResponse;
import com.orchexpay.walletledger.services.GetBankDetailsUseCase;
import com.orchexpay.walletledger.services.GetCurrentUserProfileUseCase;
import com.orchexpay.walletledger.services.GetUserByUsernameUseCase;
import com.orchexpay.walletledger.services.SaveBankDetailsUseCase;
import com.orchexpay.walletledger.enums.Role;
import com.orchexpay.walletledger.models.User;
import com.orchexpay.walletledger.models.VendorBankDetails;
import com.orchexpay.walletledger.repositories.UserRepository;
import com.orchexpay.walletledger.security.LedgerPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final GetCurrentUserProfileUseCase getCurrentUserProfileUseCase;
    private final GetUserByUsernameUseCase getUserByUsernameUseCase;
    private final GetBankDetailsUseCase getBankDetailsUseCase;
    private final SaveBankDetailsUseCase saveBankDetailsUseCase;
    private final UserMapper userMapper;
    private final UserRepository userRepository;

    /**
     * Get minimal user info by id (id, username). Allowed: ADMIN; or same user; or MERCHANT viewing a user in their merchant (e.g. vendor).
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getById(@PathVariable UUID id, Authentication authentication) {
        User current = getUserByUsernameUseCase.execute(((LedgerPrincipal) authentication.getPrincipal()).username());
        if (current.hasRole(Role.ADMIN)) {
            return userRepository.findById(id)
                    .map(u -> ResponseEntity.ok(userMapper.toResponse(u)))
                    .orElse(ResponseEntity.notFound().build());
        }
        if (current.getId().equals(id)) {
            return userRepository.findById(id)
                    .map(u -> ResponseEntity.ok(userMapper.toResponse(u)))
                    .orElse(ResponseEntity.notFound().build());
        }
        if (current.hasRole(Role.MERCHANT) && current.getMerchantId() != null) {
            return userRepository.findById(id)
                    .filter(u -> current.getMerchantId().equals(u.getMerchantId()))
                    .map(u -> ResponseEntity.ok(userMapper.toResponse(u)))
                    .orElse(ResponseEntity.notFound().build());
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        String username = ((LedgerPrincipal) authentication.getPrincipal()).username();
        GetCurrentUserProfileUseCase.CurrentUserProfile profile = getCurrentUserProfileUseCase.execute(username);
        return ResponseEntity.ok(userMapper.toResponse(
                profile.user(),
                profile.mainWalletId(),
                profile.escrowWalletId(),
                profile.vendorWalletId()));
    }

    @GetMapping("/me/bank-details")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<BankDetailsResponse> getMyBankDetails(Authentication authentication) {
        String username = ((LedgerPrincipal) authentication.getPrincipal()).username();
        var profile = getCurrentUserProfileUseCase.execute(username);
        var details = getBankDetailsUseCase.execute(profile.user().getId());
        if (details.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        VendorBankDetails d = details.get();
        return ResponseEntity.ok(BankDetailsResponse.builder()
                .userId(d.getUserId())
                .accountNumber(d.getAccountNumber())
                .ifscCode(d.getIfscCode())
                .beneficiaryName(d.getBeneficiaryName())
                .updatedAt(d.getUpdatedAt())
                .build());
    }

    @PutMapping("/me/bank-details")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<BankDetailsResponse> updateMyBankDetails(
            @Valid @RequestBody BankDetailsRequest request,
            Authentication authentication) {
        String username = ((LedgerPrincipal) authentication.getPrincipal()).username();
        var profile = getCurrentUserProfileUseCase.execute(username);
        VendorBankDetails saved = saveBankDetailsUseCase.execute(
                profile.user().getId(),
                request.getAccountNumber(),
                request.getIfscCode(),
                request.getBeneficiaryName());
        return ResponseEntity.ok(BankDetailsResponse.builder()
                .userId(saved.getUserId())
                .accountNumber(saved.getAccountNumber())
                .ifscCode(saved.getIfscCode())
                .beneficiaryName(saved.getBeneficiaryName())
                .updatedAt(saved.getUpdatedAt())
                .build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        Set<Role> roles = request.getRoles().stream()
                .map(String::toUpperCase)
                .map(Role::valueOf)
                .collect(Collectors.toSet());
        if (roles.contains(Role.MERCHANT) && (request.getCurrencyCode() == null || request.getCurrencyCode().isBlank())) {
            throw new IllegalArgumentException("Currency code is required when creating a MERCHANT account");
        }
        CreateUserResult result = createUserUseCase.execute(
                request.getUsername(),
                request.getPassword(),
                roles,
                request.getCurrencyCode()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(
                userMapper.toResponse(result.user(), result.mainWalletId(), result.escrowWalletId(), null));
    }
}
