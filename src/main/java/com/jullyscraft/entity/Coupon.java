package com.jullyscraft.entity;

import com.jullyscraft.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "coupons", indexes = {
        @Index(name = "idx_coupon_code",   columnList = "code",   unique = true),
        @Index(name = "idx_coupon_active", columnList = "active"),
        @Index(name = "idx_coupon_expiry", columnList = "expires_at")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Coupon extends BaseEntity {

    // ── Identity ──────────────────────────────────────────────────────────────
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String description;

    // ── Discount type ─────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DiscountType discountType;

    @Column(precision = 10, scale = 2)
    private BigDecimal discountValue;        // % for PERCENTAGE, flat amount for FLAT

    @Column(precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount;    // cap for PERCENTAGE type

    // ── Buy X Get Y ───────────────────────────────────────────────────────────
    private Integer buyQuantity;             // buy X
    private Integer getQuantity;             // get Y free
    private Long    buyProductId;            // null = any product
    private Long    getProductId;            // null = same as buy

    // ── Conditions ────────────────────────────────────────────────────────────
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    private Long applicableCategoryId;       // null = all categories

    // ── Validity ──────────────────────────────────────────────────────────────
    @Column(nullable = false)
    private LocalDateTime startsAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    // ── Usage limits ──────────────────────────────────────────────────────────
    private Integer maxUsageTotal;           // null = unlimited
    private Integer maxUsagePerUser;         // null = unlimited

    @Column(nullable = false)
    @Builder.Default
    private int usageCount = 0;

    // ── User-specific coupons ─────────────────────────────────────────────────
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "coupon_allowed_users",
            joinColumns        = @JoinColumn(name = "coupon_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private Set<User> allowedUsers = new HashSet<>();  // empty = public coupon

    // ── Flash sale flag ───────────────────────────────────────────────────────
    @Column(nullable = false)
    @Builder.Default
    private boolean flashSale = false;

    // ── Enums ─────────────────────────────────────────────────────────────────
    public enum DiscountType {
        PERCENTAGE,          // e.g. 20% off
        FLAT,                // e.g. ₹100 off
        BUY_X_GET_Y,         // buy 2 get 1 free
        FREE_SHIPPING        // waive shipping fee
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isStarted() {
        return LocalDateTime.now().isAfter(startsAt);
    }

    public boolean isUsageLimitReached() {
        return maxUsageTotal != null && usageCount >= maxUsageTotal;
    }

    public boolean isPublic() {
        return allowedUsers.isEmpty();
    }

    public void incrementUsage() { this.usageCount++; }
    public void decrementUsage() {
        if (this.usageCount > 0) this.usageCount--;
    }
}