package com.example.amazon.dto.response;

import com.example.amazon.entity.OrderStatus;
import lombok.Getter;

@Getter
public class OrderResponse {

    private final String orderId;
    private final OrderStatus status;

    public OrderResponse(String orderId, OrderStatus status) {
        this.orderId = orderId;
        this.status = status;
    }
}
