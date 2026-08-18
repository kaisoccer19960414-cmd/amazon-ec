package com.example.amazon.dto.request;

import com.example.amazon.entity.CpuMaker;
import com.example.amazon.entity.ProductCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotNull
    private ProductCategory category;

    /**
     * フィールド名を"active"にしているのは意図的。"isActive"にすると
     * Lombokのgetter/setter名(isActive()/setActive())とJSONプロパティ名が
     * 一致せず、デシリアライズに失敗する落とし穴があるため避けている。
     */
    private boolean active;

    /** LAPTOP/DESKTOPの場合のみDellから送られてくる。他カテゴリではnull */
    private Integer ramGb;
    private Integer ssdGb;
    private CpuMaker cpuMaker;
    private Boolean hasGpu;
}