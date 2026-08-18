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
    void 不正な数量では商品を追加できない() {
        assertThatThrownBy(() -> cartService.addItem(1L, "PRD-000001", 0))
                .isInstanceOf(IllegalArgumentException.class);

        verify(productRepository, never()).findById(anyString());
    }
}
