package com.jullyscraft.entity;

import com.jullyscraft.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "referral_codes", indexes = {
        @Index(name = "idx_referral_code",  columnList = "code",    unique = true),
        @Index(name = "idx_referral_owner", columnList = "owner_id")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ReferralCode extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    // Reward config
    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal referrerReward = BigDecimal.valueOf(100);   // ₹ credit to referrer

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal refereeDiscount = BigDecimal.valueOf(50);   // ₹ discount for new user

    @Column(nullable = false)
    @Builder.Default
    private int timesUsed = 0;

    @Column
    private Integer maxUses;          // null = unlimited

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column
    private LocalDateTime expiresAt;

    public boolean isValid() {
        return active
                && !isDeleted()
                && (expiresAt == null || LocalDateTime.now().isBefore(expiresAt))
                && (maxUses == null || timesUsed < maxUses);
    }
}