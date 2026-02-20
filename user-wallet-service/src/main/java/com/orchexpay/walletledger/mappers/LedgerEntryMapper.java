package com.orchexpay.walletledger.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orchexpay.walletledger.dtos.LedgerEntryResponse;
import com.orchexpay.walletledger.models.LedgerEntry;
import org.springframework.stereotype.Component;

@Component
public class LedgerEntryMapper {

    private final ObjectMapper objectMapper;

    public LedgerEntryMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public LedgerEntryResponse toResponse(LedgerEntry entry) {
        return LedgerEntryResponse.builder()
                .id(entry.getId())
                .walletId(entry.getWalletId())
                .type(entry.getType().name())
                .amount(entry.getAmount().getAmount())
                .currencyCode(entry.getAmount().getCurrency().getCode())
                .referenceType(entry.getReferenceType() != null ? entry.getReferenceType().name() : null)
                .referenceId(entry.getReferenceId())
                .status(entry.getStatus() != null ? entry.getStatus().name() : null)
                .description(entry.getDescription())
                .createdAt(entry.getCreatedAt())
                .build();
    }

    public String toJson(LedgerEntryResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize LedgerEntryResponse", e);
        }
    }

    public LedgerEntryResponse toResponseFromJson(String json) {
        try {
            return objectMapper.readValue(json, LedgerEntryResponse.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize LedgerEntryResponse", e);
        }
    }

    public String transferResultToJson(com.orchexpay.walletledger.dtos.TransferResultResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize TransferResultResponse", e);
        }
    }

    public com.orchexpay.walletledger.dtos.TransferResultResponse transferResultFromJson(String json) {
        try {
            return objectMapper.readValue(json, com.orchexpay.walletledger.dtos.TransferResultResponse.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize TransferResultResponse", e);
        }
    }
}
