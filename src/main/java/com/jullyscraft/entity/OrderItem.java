package com.jullyscraft.entity;

import com.jullyscraft.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items", indexes = {
        @Index(name = "idx_order_item_order",   columnList = "order_id"),
        @Index(name = "idx_order_item_product", columnList = "product_id")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Snapshots — product may change after order
    @Column(nullable = false, length = 200)
    private String productName;

    @Column(length = 100)
    private String productSku;

    @Column(length = 100)
    private String variantInfo;     // e.g. "Size: M, Color: Red"

    @Column(length = 500)
    private String productImageUrl;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;
}