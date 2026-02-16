package com.robot.warehouse.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.robot.warehouse.dto.ErrorDetails;
import com.robot.warehouse.exception.OperationResponseException;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice()
@Slf4j()
public class GlobalExceptionHandler {

    // ========================================================================
    // Exception Handlers - Unified Error Handling
    // ========================================================================

    /**
     * Handle custom OperationResponseException (business logic errors)
     */
    @ExceptionHandler(OperationResponseException.class)
    public ResponseEntity<ErrorDetails> handleOperationResponseException(OperationResponseException ex) {
        log.warn("Operation failed: {} - {}", ex.getErrorCode(), ex.getMessage());

        ErrorDetails error = new ErrorDetails();
        error.setSuccess(false);
        error.setMessage(ex.getMessage());
        error.setErrorCode(ex.getErrorCode());
        error.setTimestamp(java.time.LocalDateTime.now());

        if (error.getErrorCode() == "SERVICE_UNAVAILABLE") {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
        } else {
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Handle generic RuntimeException (WCF faults, unexpected errors)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorDetails> handleRuntimeException(RuntimeException ex) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        ErrorDetails error = ErrorDetails.builder()
                .success(false)
                .message(ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred")
                .errorCode("INTERNAL_ERROR")
                .timestamp(java.time.LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
