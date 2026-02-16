package com.orchexpay.walletledger.infrastructure.persistence.converter;

import com.orchexpay.walletledger.domain.model.Role;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Converter
public class RoleSetConverter implements AttributeConverter<Set<Role>, String> {

    @Override
    public String convertToDatabaseColumn(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) return "";
        return roles.stream().map(Enum::name).collect(Collectors.joining(","));
    }

    @Override
    public Set<Role> convertToEntityAttribute(String db) {
        if (db == null || db.isBlank()) return Collections.emptySet();
        return Arrays.stream(db.split(","))
                .map(String::trim)
                .map(Role::valueOf)
                .collect(Collectors.toSet());
    }
}
