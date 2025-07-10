package com.pfe.DFinancialStatement.error_messages.exception;

public class CustomException extends RuntimeException {
    private final String errorCode;

    public CustomException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
