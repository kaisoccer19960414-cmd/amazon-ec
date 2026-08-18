package com.example.amazon.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor
public class Product {

    /** Dellが採番した商品ID(例: PRD-000001)をそのまま保存する。Amazon側では採番しない */
    @Id
    private String id;

    @Setter
    private String name;

    @Setter
    private int price;

    /** 表示用の在庫キャッシュ。実在庫の真実はDell側が持つ */
    @Setter
    @Column(name = "cached_stock")
    private int cachedStock;

    @Setter
    @Enumerated(EnumType.STRING)
    private ProductCategory category;

    /** Dellから同期される販売停止フラグ。falseの間は一覧・購入から除外する */
    @Setter
    @Column(name = "is_active")
    private boolean isActive;

    @Setter
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Product(String id, String name, int price, int cachedStock, ProductCategory category, boolean isActive) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.cachedStock = cachedStock;
        this.category = category;
        this.isActive = isActive;
        this.updatedAt = LocalDateTime.now();
    }
}