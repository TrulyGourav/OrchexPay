package com.orchexpay.walletledger.exceptions;

import com.orchexpay.walletledger.dtos.ErrorResponse;
import com.orchexpay.walletledger.exceptions.InsufficientBalanceException;
import com.orchexpay.walletledger.exceptions.InvalidCredentialsException;
import com.orchexpay.walletledger.exceptions.UserAlreadyExistsException;
import com.orchexpay.walletledger.exceptions.UserNotFoundException;
import com.orchexpay.walletledger.exceptions.LedgerEntryNotFoundException;
import com.orchexpay.walletledger.exceptions.WalletAlreadyExistsException;
import com.orchexpay.walletledger.exceptions.WalletNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @ExceptionHandler(WalletNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWalletNotFound(WalletNotFoundException ex, HttpServletRequest request) {
        log.warn("Wallet not found: {}", ex.getMessage());
        return buildResponse(request, HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    @ExceptionHandler(LedgerEntryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleLedgerEntryNotFound(LedgerEntryNotFoundException ex, HttpServletRequest request) {
        log.warn("Ledger entry not found: {}", ex.getMessage());
        return buildResponse(request, HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    @ExceptionHandler(WalletAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleWalletAlreadyExists(WalletAlreadyExistsException ex, HttpServletRequest request) {
        log.warn("Wallet already exists: {}", ex.getMessage());
        return buildResponse(request, HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(InsufficientBalanceException ex, HttpServletRequest request) {
        log.warn("Insufficient balance: {}", ex.getMessage());
        return buildResponse(request, HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable Entity", ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        log.warn("Invalid credentials attempt");
        return buildResponse(request, HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex, HttpServletRequest request) {
        log.warn("User not found: {}", ex.getMessage());
        return buildResponse(request, HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException ex, HttpServletRequest request) {
        log.warn("User already exists: {}", ex.getMessage());
        return buildResponse(request, HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity violation (duplicate or constraint): {}", ex.getMessage());
        return buildResponse(request, HttpStatus.CONFLICT, "Conflict", "User or resource already exists");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Invalid request: {}", ex.getMessage());
        return buildResponse(request, HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        log.warn("Invalid state: {}", ex.getMessage());
        return buildResponse(request, HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable Entity", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErrorResponse.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .collect(Collectors.toList());
        log.warn("Validation failed: {}", errors);
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message("Validation failed")
                .path(request.getRequestURI())
                .correlationId(request.getHeader(CORRELATION_ID_HEADER))
                .fieldErrors(errors)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBind(BindException ex, HttpServletRequest request) {
        List<ErrorResponse.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .collect(Collectors.toList());
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message("Validation failed")
                .path(request.getRequestURI())
                .correlationId(request.getHeader(CORRELATION_ID_HEADER))
                .fieldErrors(errors)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        return buildResponse(request, HttpStatus.FORBIDDEN, "Forbidden", "Access denied");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error", ex);
        return buildResponse(request, HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred");
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpServletRequest request, HttpStatus status, String error, String message) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(error)
                .message(message)
                .path(request.getRequestURI())
                .correlationId(request.getHeader(CORRELATION_ID_HEADER))
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
