package com.jullyscraft.entity;

import com.jullyscraft.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_product_slug",     columnList = "slug",        unique = true),
        @Index(name = "idx_product_category", columnList = "category_id"),
        @Index(name = "idx_product_active",   columnList = "active"),
        @Index(name = "idx_product_featured", columnList = "featured")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Product extends BaseEntity {

    // ── Core ──────────────────────────────────────────────────────────────────
    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 220)
    private String slug;

    @Column(nullable = false, length = 500)
    private String shortDescription;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String fullDescription;

    // ── Pricing ───────────────────────────────────────────────────────────────
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(precision = 12, scale = 2)
    private BigDecimal discountPrice;

    // ── Inventory (base — variants override per SKU) ──────────────────────────
    @Column(nullable = false)
    @Builder.Default
    private int stockQuantity = 0;

    @Column(nullable = false)
    @Builder.Default
    private int lowStockThreshold = 5;

    // ── Meta ──────────────────────────────────────────────────────────────────
    @Column(length = 100)
    private String brand;

    @Column(length = 500)
    private String tags;           // comma-separated

    @Column(length = 70)
    private String metaTitle;

    @Column(length = 160)
    private String metaDescription;

    // ── Flags ─────────────────────────────────────────────────────────────────
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean featured = false;

    @Column(nullable = false)
    @Builder.Default
    private double averageRating = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private int reviewCount = 0;

    // ── Relations ─────────────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductSpecification> specifications = new ArrayList<>();

    // ── Helpers ───────────────────────────────────────────────────────────────
    public boolean isInStock()  { return stockQuantity > 0; }
    public boolean isLowStock() { return stockQuantity > 0 && stockQuantity <= lowStockThreshold; }

    public BigDecimal getEffectivePrice() {
        return discountPrice != null ? discountPrice : price;
    }

    public int getDiscountPercent() {
        if (discountPrice == null || price.compareTo(BigDecimal.ZERO) == 0) return 0;
        return price.subtract(discountPrice)
                .multiply(BigDecimal.valueOf(100))
                .divide(price, 0, java.math.RoundingMode.HALF_UP)
                .intValue();
    }

    public void addImage(ProductImage image) {
        image.setProduct(this);
        images.add(image);
    }

    public void addVariant(ProductVariant variant) {
        variant.setProduct(this);
        variants.add(variant);
    }

    public void addSpecification(ProductSpecification spec) {
        spec.setProduct(this);
        specifications.add(spec);
    }
}