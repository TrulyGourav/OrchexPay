package com.orchexpay.walletledger.mappers;

import com.orchexpay.walletledger.dtos.UserResponse;
import com.orchexpay.walletledger.models.User;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return toResponse(user, null, null, null);
    }

    public UserResponse toResponse(User user, UUID mainWalletId, UUID escrowWalletId, UUID vendorWalletId) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .roles(user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()))
                .merchantId(user.getMerchantId())
                .status(user.getStatus().name())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .mainWalletId(mainWalletId)
                .escrowWalletId(escrowWalletId)
                .vendorWalletId(vendorWalletId)
                .build();
    }
}
