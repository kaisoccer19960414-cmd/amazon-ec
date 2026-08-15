package com.example.amazon.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductSyncRequest {

    @NotBlank
    private String productId;

    @NotBlank
    private String name;

    @Min(0)
    private int price;

    @Min(0)
    private int stock;
}