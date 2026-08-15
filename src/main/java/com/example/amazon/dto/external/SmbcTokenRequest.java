package com.example.amazon.dto.external;

public record SmbcTokenRequest(String cardNumber, String expiryMonth, String expiryYear, String cvv) {
}