package com.example.amazon.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * LAPTOP/DESKTOPカテゴリの商品だけが持つスペック情報。
 * Dellから同期されてくる内容をそのまま保持する(Amazon側では加工しない)。
 */
@Entity
@Table(name = "pc_specs")
@Getter
@NoArgsConstructor
public class PcSpec {

    @Id
    @Column(name = "product_id")
    private String productId;

    @Setter
    @Column(name = "ram_gb")
    private int ramGb;

    @Setter
    @Column(name = "ssd_gb")
    private int ssdGb;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "cpu_maker")
    private CpuMaker cpuMaker;

    @Setter
    @Column(name = "has_gpu")
    private Boolean hasGpu;

    public PcSpec(String productId, int ramGb, int ssdGb, CpuMaker cpuMaker, Boolean hasGpu) {
        this.productId = productId;
        this.ramGb = ramGb;
        this.ssdGb = ssdGb;
        this.cpuMaker = cpuMaker;
        this.hasGpu = hasGpu;
    }
}