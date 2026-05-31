package com.jullyscraft.entity;

import com.jullyscraft.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "reviews",
        uniqueConstraints = @UniqueConstraint(
                name  = "uq_review_user_product",
                columnNames = {"user_id", "product_id"}
        ),
        indexes = {
                @Index(name = "idx_review_product", columnList = "product_id"),
                @Index(name = "idx_review_user",    columnList = "user_id"),
                @Index(name = "idx_review_status",  columnList = "status")
        }
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Review extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int rating;                  // 1–5

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    // ── Verified purchase ─────────────────────────────────────────────────────
    @Column(nullable = false)
    @Builder.Default
    private boolean verifiedPurchase = false;

    // ── Moderation ────────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.PENDING;

    @Column(length = 500)
    private String moderationNote;

    // ── AI analysis ───────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Sentiment sentiment;

    @Column(nullable = false)
    @Builder.Default
    private boolean spamFlagged = false;

    @Column(length = 500)
    private String spamReason;

    // ── Reports ───────────────────────────────────────────────────────────────
    @Column(nullable = false)
    @Builder.Default
    private int reportCount = 0;

    // ── Helpful votes ─────────────────────────────────────────────────────────
    @Column(nullable = false)
    @Builder.Default
    private int helpfulCount = 0;

    // ── Enums ─────────────────────────────────────────────────────────────────
    public enum ReviewStatus { PENDING, APPROVED, REJECTED }
    public enum Sentiment    { POSITIVE, NEUTRAL, NEGATIVE }
}