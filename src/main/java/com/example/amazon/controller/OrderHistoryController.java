package com.example.amazon.controller;

import com.example.amazon.entity.Order;
import com.example.amazon.repository.OrderRepository;
import com.example.amazon.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class OrderHistoryController {

    private final OrderRepository orderRepository;

    /**
     * ログイン中ユーザー本人の注文だけを表示する。
     * principal.getUserId()はSpringSecurityの認証情報から取得するため、
     * URLパラメータ等で他人のuserIdを指定してのアクセスはできない。
     */
    @GetMapping("/account/orders")
    public String orders(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(principal.getUserId());
        model.addAttribute("orders", orders);
        return "order-history";
    }
}