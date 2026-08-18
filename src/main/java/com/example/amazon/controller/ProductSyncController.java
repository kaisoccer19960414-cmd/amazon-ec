package com.example.amazon.controller;

import com.example.amazon.dto.request.ProductSyncRequest;
import com.example.amazon.entity.PcSpec;
import com.example.amazon.entity.Product;
import com.example.amazon.entity.ProductCategory;
import com.example.amazon.repository.PcSpecRepository;
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

@RestController
@RequiredArgsConstructor
public class ProductSyncController {

    private final ProductRepository productRepository;
    private final PcSpecRepository pcSpecRepository;

    @Value("${products.sync.api-key}")
    private String expectedApiKey;

    @PostMapping("/products/sync")
    public ResponseEntity<Map<String, Boolean>> sync(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @Valid @RequestBody ProductSyncRequest request) {

        if (apiKey == null || !apiKey.equals(expectedApiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false));
        }

        Product product = productRepository.findById(request.getProductId()).orElse(null);

        if (product == null) {
            product = new Product(request.getProductId(), request.getName(), request.getPrice(),
                    request.getStock(), request.getCategory(), request.isActive());
        } else {
            product.setName(request.getName());
            product.setPrice(request.getPrice());
            product.setCachedStock(request.getStock());
            product.setCategory(request.getCategory());
            product.setActive(request.isActive());
            product.setUpdatedAt(LocalDateTime.now());
        }
        productRepository.save(product);

        boolean isPc = request.getCategory() == ProductCategory.LAPTOP
                || request.getCategory() == ProductCategory.DESKTOP;

        if (isPc && request.getRamGb() != null && request.getSsdGb() != null && request.getCpuMaker() != null) {
            PcSpec pcSpec = pcSpecRepository.findById(request.getProductId()).orElse(null);
            if (pcSpec == null) {
                pcSpec = new PcSpec(request.getProductId(), request.getRamGb(), request.getSsdGb(),
                        request.getCpuMaker(), request.getHasGpu());
            } else {
                pcSpec.setRamGb(request.getRamGb());
                pcSpec.setSsdGb(request.getSsdGb());
                pcSpec.setCpuMaker(request.getCpuMaker());
                pcSpec.setHasGpu(request.getHasGpu());
            }
            pcSpecRepository.save(pcSpec);
        }

        return ResponseEntity.ok(Map.of("success", true));
    }
}