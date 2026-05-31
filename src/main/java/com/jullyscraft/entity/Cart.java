package com.jullyscraft.entity;

import com.jullyscraft.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts", indexes = {
        @Index(name = "idx_cart_user", columnList = "user_id", unique = true)
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Cart extends BaseEntity {

    // One cart per user — created lazily on first add
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToMany(mappedBy = "cart",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    // ── Computed helpers ──────────────────────────────────────────────────────

    public BigDecimal getSubtotal() {
        return items.stream()
                .filter(i -> !i.isDeleted())
                .map(CartItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int getTotalItems() {
        return items.stream()
                .filter(i -> !i.isDeleted())
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    public void addItem(CartItem item) {
        item.setCart(this);
        items.add(item);
    }
}