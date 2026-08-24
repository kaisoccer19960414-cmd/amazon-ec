package com.example.amazon.service;

import com.example.amazon.entity.CartItem;
import com.example.amazon.entity.Product;
import com.example.amazon.entity.ProductCategory;
import com.example.amazon.repository.CartItemRepository;
import com.example.amazon.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartService cartService;

    @Test
    void 新しい商品をカートに追加する() {
        Product product = new Product("PRD-000001", "テスト商品", 1000, 3, ProductCategory.ACCESSORY, true);
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(cartItemRepository.findByUserIdAndProduct_Id(1L, product.getId())).thenReturn(Optional.empty());

        cartService.addItem(1L, product.getId(), 2);

        verify(cartItemRepository).save(argThat(item ->
                item.getUserId().equals(1L) && item.getProduct().equals(product) && item.getQuantity() == 2));
    }

    @Test
    void 販売停止中の商品はカートに追加できない() {
        Product product = new Product("PRD-000001", "テスト商品", 1000, 3, ProductCategory.ACCESSORY, false);
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.addItem(1L, product.getId(), 1))
                .isInstanceOf(IllegalArgumentException.class);

        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void 在庫切れの商品はカートに追加できない() {
        Product product = new Product("PRD-000001", "テスト商品", 1000, 0, ProductCategory.ACCESSORY, true);
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.addItem(1L, product.getId(), 1))
                .isInstanceOf(IllegalArgumentException.class);

        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void 不正な数量では商品を追加できない() {
        assertThatThrownBy(() -> cartService.addItem(1L, "PRD-000001", 0))
                .isInstanceOf(IllegalArgumentException.class);

        verify(productRepository, never()).findById(anyString());
    }

    @Test
    void 在庫を超える数量はカートに追加できない() {
        Product product = new Product("PRD-000001", "テスト商品", 1000, 3, ProductCategory.ACCESSORY, true);
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(cartItemRepository.findByUserIdAndProduct_Id(1L, product.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(1L, product.getId(), 4))
                .isInstanceOf(IllegalArgumentException.class);

        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void 既存の数量と合算して在庫を超える場合はカートに追加できない() {
        Product product = new Product("PRD-000001", "テスト商品", 1000, 3, ProductCategory.ACCESSORY, true);
        CartItem existing = new CartItem(1L, product, 2);
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(cartItemRepository.findByUserIdAndProduct_Id(1L, product.getId())).thenReturn(Optional.of(existing));

        // 既に2個入っている状態でさらに2個(合計4個)追加しようとすると、在庫3個を超える
        assertThatThrownBy(() -> cartService.addItem(1L, product.getId(), 2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 在庫を超える数量には更新できない() {
        Product product = new Product("PRD-000001", "テスト商品", 1000, 3, ProductCategory.ACCESSORY, true);
        CartItem item = new CartItem(1L, product, 1);
        when(cartItemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> cartService.changeQuantity(1L, 10L, 4))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
