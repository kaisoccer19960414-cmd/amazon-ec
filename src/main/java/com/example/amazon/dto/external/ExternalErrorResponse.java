package com.example.amazon.dto.external;

public record ExternalErrorResponse(String errorCode, String message, String orderId) {
}
