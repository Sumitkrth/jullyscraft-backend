package com.jullyscraft.entity;

import com.jullyscraft.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "coupon_usages",
        uniqueConstraints = @UniqueConstraint(
                name       = "uq_coupon_usage_order",
                columnNames = {"coupon_id", "order_id"}
        ),
        indexes = {
                @Index(name = "idx_coupon_usage_user",   columnList = "user_id"),
                @Index(name = "idx_coupon_usage_coupon", columnList = "coupon_id")
        }
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CouponUsage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")            // null = guest
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, precision = 10, scale = 2)
    private java.math.BigDecimal discountApplied;
}