package com.example.amazon.controller;

import com.example.amazon.entity.Order;
import com.example.amazon.entity.OrderStatus;
import com.example.amazon.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AdminOrderController {

    /** 注文管理画面の1ページあたりの件数 */
    private static final int PAGE_SIZE = 30;

    private final OrderRepository orderRepository;

    @GetMapping("/admin/orders")
    public String orders(@RequestParam(required = false) OrderStatus status,
                         @RequestParam(required = false) String keyword,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {
        PageRequest pageRequest = PageRequest.of(page, PAGE_SIZE);
        boolean hasKeyword = keyword != null && !keyword.isBlank();

        Page<Order> orders;
        if (status != null && hasKeyword) {
            orders = orderRepository.findByStatusAndIdContainingOrderByCreatedAtDesc(status, keyword, pageRequest);
        } else if (status != null) {
            orders = orderRepository.findByStatusOrderByCreatedAtDesc(status, pageRequest);
        } else if (hasKeyword) {
            orders = orderRepository.findByIdContainingOrderByCreatedAtDesc(keyword, pageRequest);
        } else {
            orders = orderRepository.findAllByOrderByCreatedAtDesc(pageRequest);
        }

        model.addAttribute("orders", orders.getContent());
        model.addAttribute("page", orders);
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("keyword", keyword);
        return "admin/orders";
    }
}