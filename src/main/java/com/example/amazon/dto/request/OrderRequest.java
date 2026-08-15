package com.example.amazon.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderRequest {

    @NotNull
    private Long userId;

    @NotBlank
    private String productId;

    @Min(1)
    private int quantity;

    @NotBlank
    private String token;
}