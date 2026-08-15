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
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Product(String id, String name, int price, int cachedStock) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.cachedStock = cachedStock;
        this.updatedAt = LocalDateTime.now();
    }
}