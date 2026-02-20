package com.orchexpay.walletledger.exceptions;

public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String username) {
        super("User already exists: " + username);
    }
}
