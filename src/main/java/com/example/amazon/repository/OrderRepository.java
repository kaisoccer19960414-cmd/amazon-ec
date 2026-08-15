package com.example.amazon.repository;

import com.example.amazon.entity.Order;
import com.example.amazon.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {

    /**
     * orderId採番(ORD-yyyyMMdd-連番4桁)のために、
     * 指定プレフィックス(例: "ORD-20260808-")で始まる注文が何件あるかを数える。
     */
    long countByIdStartingWith(String prefix);

    /** 管理画面の注文一覧(新しい順) */
    List<Order> findAllByOrderByCreatedAtDesc();

    /** 管理画面のステータス絞り込み一覧(新しい順) */
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    /** ユーザー本人の注文履歴(新しい順)。他ユーザーの注文は含まれない */
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
}