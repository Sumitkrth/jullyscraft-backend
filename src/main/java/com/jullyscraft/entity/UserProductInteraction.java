package com.jullyscraft.entity;

import com.jullyscraft.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "user_product_interactions",
        indexes = {
                @Index(name = "idx_interaction_user",    columnList = "user_id"),
                @Index(name = "idx_interaction_product", columnList = "product_id"),
                @Index(name = "idx_interaction_type",    columnList = "type")
        }
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class UserProductInteraction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")           // null = anonymous
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InteractionType type;

    // Weight used for scoring (view=1, cart=3, purchase=5, wishlist=2)
    @Column(nullable = false)
    @Builder.Default
    private int score = 1;

    @Column(length = 100)
    private String sessionId;               // for anonymous users

    public enum InteractionType {
        VIEW, CART_ADD, PURCHASE, WISHLIST, REVIEW
    }
}