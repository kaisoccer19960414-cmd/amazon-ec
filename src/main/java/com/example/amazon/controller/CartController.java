package com.example.amazon.controller;

import com.example.amazon.dto.request.OrderRequest;
import com.example.amazon.entity.CartItem;
import com.example.amazon.entity.Order;
import com.example.amazon.entity.OrderStatus;
import com.example.amazon.entity.User;
import com.example.amazon.repository.UserRepository;
import com.example.amazon.security.UserPrincipal;
import com.example.amazon.service.CartService;
import com.example.amazon.service.OrderService;
import com.example.amazon.service.TaxCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;
    private final OrderService orderService;
    private final UserRepository userRepository;
    private final TaxCalculator taxCalculator;

    @GetMapping
    public String cart(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        List<CartItem> items = cartService.getItems(principal.getUserId());
        // カートの合計は「行ごとの税込み小計」の合計にする。チェックアウト時に
        // 商品1つずつが別々のOrderになり、Order側もこの単位で税額を計算するため、
        // ここでの表示と実際の請求額が常に一致する。
        int total = items.stream()
                .mapToInt(item -> taxCalculator.includedTotal(item.getProduct().getPrice(), item.getQuantity()))
                .sum();
        model.addAttribute("items", items);
        model.addAttribute("total", total);
        return "cart";
    }

    @PostMapping("/items")
    public String addItem(@RequestParam String productId,
                          @RequestParam(defaultValue = "1") int quantity,
                          @AuthenticationPrincipal UserPrincipal principal,
                          RedirectAttributes attributes) {
        cartService.addItem(principal.getUserId(), productId, quantity);
        attributes.addFlashAttribute("message", "商品をカートに追加しました。");
        return "redirect:/cart";
    }

    @PostMapping("/items/{cartItemId}")
    public String changeQuantity(@PathVariable Long cartItemId,
                                 @RequestParam int quantity,
                                 @AuthenticationPrincipal UserPrincipal principal) {
        cartService.changeQuantity(principal.getUserId(), cartItemId, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/items/{cartItemId}/delete")
    public String removeItem(@PathVariable Long cartItemId, @AuthenticationPrincipal UserPrincipal principal) {
        cartService.removeItem(principal.getUserId(), cartItemId);
        return "redirect:/cart";
    }

    @PostMapping("/checkout")
    public String checkout(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new IllegalStateException("ログイン中のユーザーが見つかりません"));
        if (user.getSmbcToken() == null) return "redirect:/account/card";

        List<CartItem> items = cartService.getItems(user.getId());
        if (items.isEmpty()) return "redirect:/cart";

        List<Order> orders = new ArrayList<>();
        List<Long> completedItemIds = new ArrayList<>();
        for (CartItem item : items) {
            OrderRequest request = new OrderRequest();
            request.setUserId(user.getId());
            request.setProductId(item.getProduct().getId());
            request.setQuantity(item.getQuantity());
            request.setToken(user.getSmbcToken());

            Order order = orderService.placeOrder(request);
            orders.add(order);
            if (order.getStatus() != OrderStatus.COMPLETED) break;
            completedItemIds.add(item.getId());
        }
        cartService.removeItems(user.getId(), completedItemIds);
        model.addAttribute("orders", orders);
        return "cart-order-result";
    }
}
