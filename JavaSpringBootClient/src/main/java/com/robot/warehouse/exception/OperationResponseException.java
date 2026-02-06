package com.robot.warehouse.exception;

public class OperationResponseException extends RuntimeException {
    private final String errorCode;

    public OperationResponseException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return this.errorCode;
    }
}
