package com.jullyscraft.entity;

import com.jullyscraft.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "affiliate_partners", indexes = {
        @Index(name = "idx_affiliate_code",  columnList = "affiliate_code", unique = true),
        @Index(name = "idx_affiliate_user",  columnList = "user_id")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AffiliatePartner extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, unique = true, length = 50)
    private String affiliateCode;

    @Column(nullable = false, length = 100)
    private String companyName;

    @Column(nullable = false, length = 200)
    private String websiteUrl;

    // Commission config
    @Column(nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal commissionPercent = BigDecimal.valueOf(5.0);

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalEarnings = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal pendingPayout = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private long totalClicks = 0;

    @Column(nullable = false)
    @Builder.Default
    private long totalConversions = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AffiliateStatus status = AffiliateStatus.PENDING;

    public enum AffiliateStatus {
        PENDING, APPROVED, SUSPENDED, REJECTED
    }
}