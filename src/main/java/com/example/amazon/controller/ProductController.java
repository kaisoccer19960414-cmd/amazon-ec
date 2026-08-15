package com.example.amazon.controller;

import com.example.amazon.entity.Order;
import com.example.amazon.entity.Product;
import com.example.amazon.entity.User;
import com.example.amazon.dto.request.OrderRequest;
import com.example.amazon.repository.ProductRepository;
import com.example.amazon.repository.UserRepository;
import com.example.amazon.security.UserPrincipal;
import com.example.amazon.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;

    @GetMapping("/products")
    public String products(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new IllegalStateException("ログイン中のユーザーが見つかりません"));

        List<Product> products = productRepository.findAll();

        model.addAttribute("products", products);
        model.addAttribute("cardRegistered", user.getSmbcToken() != null);
        return "products";
    }

    @PostMapping("/products/{productId}/purchase")
    public String purchase(@PathVariable String productId,
                           @RequestParam(defaultValue = "1") int quantity,
                           @AuthenticationPrincipal UserPrincipal principal,
                           Model model) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new IllegalStateException("ログイン中のユーザーが見つかりません"));

        if (user.getSmbcToken() == null) {
            return "redirect:/account/card";
        }

        OrderRequest request = new OrderRequest();
        request.setUserId(user.getId());
        request.setProductId(productId);
        request.setQuantity(quantity);
        request.setToken(user.getSmbcToken());

        Order order = orderService.placeOrder(request);

        model.addAttribute("order", order);
        return "order-result";
    }
}