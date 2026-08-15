package com.example.amazon.dto.external;

public record SmbcPayRequest(String orderId, String token, int amount) {
}
