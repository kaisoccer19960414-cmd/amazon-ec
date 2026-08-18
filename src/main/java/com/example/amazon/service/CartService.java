package com.example.amazon.service;

import com.example.amazon.entity.CartItem;
import com.example.amazon.entity.Product;
import com.example.amazon.repository.CartItemRepository;
import com.example.amazon.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<CartItem> getItems(Long userId) {
        return cartItemRepository.findByUserIdOrderByCreatedAtAsc(userId);
    }

    @Transactional
    public void addItem(Long userId, String productId, int quantity) {
        if (quantity < 1) throw new IllegalArgumentException("数量は1以上で指定してください");

        Product product = productRepository.findById(productId)
                .filter(Product::isActive)
                .orElseThrow(() -> new IllegalArgumentException("商品が見つからないか、販売停止中です"));

        cartItemRepository.findByUserIdAndProduct_Id(userId, productId)
                .ifPresentOrElse(item -> item.addQuantity(quantity),
                        () -> cartItemRepository.save(new CartItem(userId, product, quantity)));
    }

    @Transactional
    public void changeQuantity(Long userId, Long cartItemId, int quantity) {
        CartItem item = findOwnedItem(userId, cartItemId);
        if (quantity < 1) {
            cartItemRepository.delete(item);
            return;
        }
        item.changeQuantity(quantity);
    }

    @Transactional
    public void removeItem(Long userId, Long cartItemId) {
        cartItemRepository.delete(findOwnedItem(userId, cartItemId));
    }

    @Transactional
    public void removeItems(Long userId, List<Long> cartItemIds) {
        cartItemIds.forEach(id -> cartItemRepository.delete(findOwnedItem(userId, id)));
    }

    private CartItem findOwnedItem(Long userId, Long cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("カートの商品が見つかりません"));
        if (!item.getUserId().equals(userId)) {
            throw new IllegalArgumentException("このカート商品には操作できません");
        }
        return item;
    }
}
