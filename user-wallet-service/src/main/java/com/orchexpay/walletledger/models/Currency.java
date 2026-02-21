package com.orchexpay.walletledger.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * Value object representing ISO 4217 currency code.
 * Wraps JDK Currency for domain use and validation.
 */
@Getter
@EqualsAndHashCode
public final class Currency {

    private final String code;

    public Currency(@NonNull String code) {
        if (code == null || code.length() != 3) {
            throw new IllegalArgumentException("Currency code must be 3-letter ISO 4217");
        }
        this.code = code.toUpperCase();
        java.util.Currency.getInstance(this.code); // validates code
    }

    public static Currency of(String code) {
        return new Currency(code);
    }

    @Override
    public String toString() {
        return code;
    }
}
