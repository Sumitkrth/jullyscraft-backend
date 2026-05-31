package com.jullyscraft.repository;

import com.jullyscraft.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // ── Public ────────────────────────────────────────────────────────────────
    Page<Review> findByProductIdAndStatusAndDeletedFalse(
            Long productId, Review.ReviewStatus status, Pageable pageable);

    Optional<Review> findByProductIdAndUserIdAndDeletedFalse(
            Long productId, Long userId);

    boolean existsByProductIdAndUserIdAndDeletedFalse(
            Long productId, Long userId);

    // ── Verified purchase check ───────────────────────────────────────────────
    @Query("""
        SELECT COUNT(oi) > 0 FROM OrderItem oi
        WHERE oi.product.id = :productId
          AND oi.order.user.id = :userId
          AND oi.order.status
              = com.jullyscraft.entity.Order$OrderStatus.DELIVERED
        """)
    boolean hasVerifiedPurchase(Long productId, Long userId);

    // ── Rating aggregate ──────────────────────────────────────────────────────
    @Query("""
        SELECT AVG(r.rating) FROM Review r
        WHERE r.product.id = :productId
          AND r.status = com.jullyscraft.entity.Review$ReviewStatus.APPROVED
          AND r.deleted = false
        """)
    Double findAverageRating(Long productId);

    @Query("""
        SELECT COUNT(r) FROM Review r
        WHERE r.product.id = :productId
          AND r.status = com.jullyscraft.entity.Review$ReviewStatus.APPROVED
          AND r.deleted = false
        """)
    long countApproved(Long productId);

    @Query("""
        SELECT r.rating, COUNT(r) FROM Review r
        WHERE r.product.id = :productId
          AND r.status = com.jullyscraft.entity.Review$ReviewStatus.APPROVED
          AND r.deleted = false
        GROUP BY r.rating
        ORDER BY r.rating DESC
        """)
    java.util.List<Object[]> findRatingDistribution(Long productId);

    // ── Admin ─────────────────────────────────────────────────────────────────
    Page<Review> findByStatusAndDeletedFalse(
            Review.ReviewStatus status, Pageable pageable);

    Page<Review> findBySpamFlaggedTrueAndDeletedFalse(Pageable pageable);

    Page<Review> findByReportCountGreaterThanAndDeletedFalse(
            int threshold, Pageable pageable);

    // ── Helpful votes ─────────────────────────────────────────────────────────
    @Modifying
    @Query("UPDATE Review r SET r.helpfulCount = r.helpfulCount + 1 WHERE r.id = :id")
    void incrementHelpful(Long id);

    @Modifying
    @Query("UPDATE Review r SET r.reportCount = r.reportCount + 1 WHERE r.id = :id")
    void incrementReportCount(Long id);
}