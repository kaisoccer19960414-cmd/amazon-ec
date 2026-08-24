package com.example.amazon.controller;

import com.example.amazon.entity.Order;
import com.example.amazon.entity.Product;
import com.example.amazon.entity.User;
import com.example.amazon.dto.request.OrderRequest;
import com.example.amazon.repository.ProductRepository;
import com.example.amazon.repository.PcSpecRepository;
import com.example.amazon.repository.UserRepository;
import com.example.amazon.security.UserPrincipal;
import com.example.amazon.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class ProductController {

    /** 商品一覧の1ページあたりの件数 */
    private static final int PAGE_SIZE = 20;

    private final ProductRepository productRepository;
    private final PcSpecRepository pcSpecRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;

    @GetMapping("/products")
    public String products(@AuthenticationPrincipal UserPrincipal principal,
                           @RequestParam(defaultValue = "0") int page,
                           Model model) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new IllegalStateException("ログイン中のユーザーが見つかりません"));

        Page<Product> productPage = productRepository.findByIsActiveTrue(PageRequest.of(page, PAGE_SIZE));

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("page", productPage);
        model.addAttribute("cardRegistered", user.getSmbcToken() != null);
        model.addAttribute("username", principal.getUsername());
        return "products";
    }

    @GetMapping("/products/{productId}")
    public String productDetail(@PathVariable String productId, Model model) {
        Product product = productRepository.findById(productId)
                .filter(Product::isActive)
                .orElseThrow(() -> new IllegalArgumentException("商品が見つからないか、販売停止中です"));

        model.addAttribute("product", product);
        pcSpecRepository.findById(productId).ifPresent(spec -> model.addAttribute("spec", spec));
        return "product-detail";
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
