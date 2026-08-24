package com.example.amazon.controller;

import com.example.amazon.entity.Order;
import com.example.amazon.entity.Product;
import com.example.amazon.repository.OrderRepository;
import com.example.amazon.repository.ProductRepository;
import com.example.amazon.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class OrderHistoryController {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    /**
     * ログイン中ユーザー本人の注文だけを表示する。
     * principal.getUserId()はSpringSecurityの認証情報から取得するため、
     * URLパラメータ等で他人のuserIdを指定してのアクセスはできない。
     */
    @GetMapping("/account/orders")
    public String orders(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(principal.getUserId());

        // 「もう一度買う」の画像・商品名表示のため、注文に含まれる商品をまとめて取得する。
        // 注文の件数だけDBに問い合わせないよう、1回のクエリでまとめて引く。
        List<String> productIds = orders.stream().map(Order::getProductId).distinct().toList();
        Map<String, Product> productsById = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        model.addAttribute("orders", orders);
        model.addAttribute("productsById", productsById);
        return "order-history";
    }
}