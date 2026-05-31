package com.jullyscraft.entity;

import com.jullyscraft.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "product_specifications", indexes = {
        @Index(name = "idx_spec_product", columnList = "product_id")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ProductSpecification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 100)
    private String specKey;

    @Column(nullable = false, length = 500)
    private String specValue;

    @Column(nullable = false)
    @Builder.Default
    private int displayOrder = 0;
}