package com.robot.warehouse.exception;

public class OperationResponseException extends RuntimeException {
    private final String errorCode;
    private final String wcfStackTrace;

    public OperationResponseException(String errorCode, String message) {
        this(errorCode, null, message);
    }

    public OperationResponseException(String errorCode, String wcfStackTrace, String message) {
        super(message);
        this.errorCode = errorCode;
        this.wcfStackTrace = wcfStackTrace;
    }

    public String getErrorCode() {
        return this.errorCode;
    }

    public String getWcfStackTrace() {
        return this.wcfStackTrace;
    }
}
