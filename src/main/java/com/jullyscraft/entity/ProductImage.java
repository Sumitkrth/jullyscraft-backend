package com.jullyscraft.entity;

import com.jullyscraft.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "product_images", indexes = {
        @Index(name = "idx_product_image_product", columnList = "product_id")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ProductImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean primaryImage = false;

    @Column(nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    @Column(length = 200)
    private String altText;
}