package com.example.amazon.dto.external;

public record DellReserveRequest(String orderId, String productId, int quantity) {
}