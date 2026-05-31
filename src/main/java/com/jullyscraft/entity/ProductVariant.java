package com.jullyscraft.entity;

import com.jullyscraft.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "product_variants", indexes = {
        @Index(name = "idx_variant_product", columnList = "product_id"),
        @Index(name = "idx_variant_sku",     columnList = "sku", unique = true)
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ProductVariant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    // Optional variant attributes (size, color, etc.)
    @Column(length = 80)
    private String size;

    @Column(length = 80)
    private String color;

    @Column(length = 80)
    private String material;

    // Variant can override base price; null = use product price
    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @Column(precision = 12, scale = 2)
    private BigDecimal discountPrice;

    @Column(nullable = false)
    @Builder.Default
    private int stockQuantity = 0;

    @Column(nullable = false)
    @Builder.Default
    private int lowStockThreshold = 5;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    // ── Helpers ───────────────────────────────────────────────────────────────
    public boolean isInStock()   { return stockQuantity > 0; }
    public boolean isLowStock()  { return stockQuantity > 0 && stockQuantity <= lowStockThreshold; }

    public void reserve(int qty) {
        if (stockQuantity < qty) throw new IllegalStateException("Insufficient stock");
        stockQuantity -= qty;
    }

    public void release(int qty) { stockQuantity += qty; }
}