package com.example.amazon.exception;

import lombok.Getter;

@Getter
public class DellReservationException extends RuntimeException {

    private final String errorCode;

    public DellReservationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
