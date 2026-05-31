package com.jullyscraft.entity;

import com.jullyscraft.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "wishlist_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_wishlist_user_product",
                columnNames = {"user_id", "product_id"}
        ),
        indexes = {
                @Index(name = "idx_wishlist_user",    columnList = "user_id"),
                @Index(name = "idx_wishlist_product", columnList = "product_id")
        }
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class WishlistItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
}