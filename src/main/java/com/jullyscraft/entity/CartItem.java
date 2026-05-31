package com.jullyscraft.entity;

import com.jullyscraft.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "cart_items", indexes = {
        @Index(name = "idx_cart_item_cart",    columnList = "cart_id"),
        @Index(name = "idx_cart_item_product", columnList = "product_id"),
        @Index(name = "idx_cart_item_variant", columnList = "variant_id")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CartItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Null = base product (no variant selected)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @Column(nullable = false)
    private int quantity;

    // Snapshot price at time of add — protects against price changes
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal priceSnapshot;

    // ── Helpers ───────────────────────────────────────────────────────────────

    public BigDecimal getLineTotal() {
        return priceSnapshot.multiply(BigDecimal.valueOf(quantity));
    }

    public boolean isSameItem(Long productId, Long variantId) {
        boolean sameProduct = this.product.getId().equals(productId);
        boolean sameVariant = (variantId == null && this.variant == null)
                || (variantId != null && this.variant != null
                && this.variant.getId().equals(variantId));
        return sameProduct && sameVariant;
    }
}