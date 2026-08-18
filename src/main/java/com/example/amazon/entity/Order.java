package com.example.amazon.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor
public class Order {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "product_id", nullable = false)
    private String productId;

    private int quantity;

    /** 税込みの合計金額。SMBCへ実際に請求する金額と常に一致する(TaxCalculatorで計算)。 */
    @Column(name = "total_amount", nullable = false)
    private int totalAmount;

    @Column(name = "reservation_id")
    private Long reservationId;

    @Column(name = "transaction_id")
    private Long transactionId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Order(String id, Long userId, String productId, int quantity, int totalAmount) {
        this.id = id;
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.status = OrderStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void markStockReserved(Long reservationId) {
        this.reservationId = reservationId;
        this.status = OrderStatus.STOCK_RESERVED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markStockFailed() {
        this.status = OrderStatus.STOCK_FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markCompleted(Long transactionId) {
        this.transactionId = transactionId;
        this.status = OrderStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markCompensating() {
        this.status = OrderStatus.COMPENSATING;
        this.updatedAt = LocalDateTime.now();
    }

    public void markCancelled() {
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markCompensationFailed() {
        this.status = OrderStatus.COMPENSATION_FAILED;
        this.updatedAt = LocalDateTime.now();
    }
}