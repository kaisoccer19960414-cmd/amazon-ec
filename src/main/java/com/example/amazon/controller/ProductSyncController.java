package com.example.amazon.controller;

import com.example.amazon.dto.request.ProductSyncRequest;
import com.example.amazon.entity.Product;
import com.example.amazon.repository.ProductRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Dellが商品を登録・更新した際に、その場でAmazon側の在庫キャッシュへ
 * 反映するための受け口。人間のログインセッションではなく、
 * Dellだけが知っているAPIキーで本人確認を行う(サーバー間通信のための認証)。
 */
@RestController
@RequiredArgsConstructor
public class ProductSyncController {

    private final ProductRepository productRepository;

    @Value("${products.sync.api-key}")
    private String expectedApiKey;

    @PostMapping("/products/sync")
    public ResponseEntity<Map<String, Boolean>> sync(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @Valid @RequestBody ProductSyncRequest request) {

        if (apiKey == null || !apiKey.equals(expectedApiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false));
        }

        Product product = productRepository.findById(request.getProductId())
                .orElse(null);

        if (product == null) {
            product = new Product(request.getProductId(), request.getName(), request.getPrice(), request.getStock());
        } else {
            product.setName(request.getName());
            product.setPrice(request.getPrice());
            product.setCachedStock(request.getStock());
            product.setUpdatedAt(LocalDateTime.now());
        }

        productRepository.save(product);
        return ResponseEntity.ok(Map.of("success", true));
    }
}