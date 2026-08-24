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

        // 一覧・詳細画面のボタン制御をすり抜けて直接POSTされた場合の防御。
        if (product.getCachedStock() <= 0) {
            throw new IllegalArgumentException("この商品は在庫切れのため、カートに追加できません");
        }

        var existingItem = cartItemRepository.findByUserIdAndProduct_Id(userId, productId);
        // 既にカートに入っている分と合算した数量が在庫を超えないかをチェックする。
        int totalQuantity = existingItem.map(CartItem::getQuantity).orElse(0) + quantity;
        if (totalQuantity > product.getCachedStock()) {
            throw new IllegalArgumentException("在庫数(" + product.getCachedStock() + "点)を超える数量は指定できません");
        }

        existingItem.ifPresentOrElse(item -> item.addQuantity(quantity),
                () -> cartItemRepository.save(new CartItem(userId, product, quantity)));
    }

    @Transactional
    public void changeQuantity(Long userId, Long cartItemId, int quantity) {
        CartItem item = findOwnedItem(userId, cartItemId);
        if (quantity < 1) {
            cartItemRepository.delete(item);
            return;
        }
        if (quantity > item.getProduct().getCachedStock()) {
            throw new IllegalArgumentException("在庫数(" + item.getProduct().getCachedStock() + "点)を超える数量は指定できません");
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
