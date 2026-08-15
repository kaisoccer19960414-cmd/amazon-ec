package com.example.amazon.exception;

import lombok.Getter;

@Getter
public class SmbcPaymentException extends RuntimeException {

    private final String errorCode;

    public SmbcPaymentException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
