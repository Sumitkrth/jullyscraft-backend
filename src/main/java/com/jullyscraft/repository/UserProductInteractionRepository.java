package com.jullyscraft.repository;

import com.jullyscraft.entity.UserProductInteraction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserProductInteractionRepository
        extends JpaRepository<UserProductInteraction, Long> {

    // ── User history ──────────────────────────────────────────────────────────

    @Query("""
        SELECT i.product.id FROM UserProductInteraction i
        WHERE i.user.id = :userId
        GROUP BY i.product.id
        ORDER BY SUM(i.score) DESC
        """)
    List<Long> findTopProductIdsByUser(Long userId, Pageable pageable);

    @Query("""
        SELECT i.product.id FROM UserProductInteraction i
        WHERE i.user.id = :userId
          AND i.type = :type
        ORDER BY i.createdAt DESC
        """)
    List<Long> findProductIdsByUserAndType(
            Long userId,
            UserProductInteraction.InteractionType type,
            Pageable pageable);

    // ── Frequently bought together ────────────────────────────────────────────

    @Query("""
        SELECT oi2.product.id, COUNT(oi2.product.id) AS freq
        FROM OrderItem oi1
        JOIN OrderItem oi2 ON oi1.order.id = oi2.order.id
        WHERE oi1.product.id = :productId
          AND oi2.product.id <> :productId
          AND oi1.order.status
              = com.jullyscraft.entity.Order$OrderStatus.DELIVERED
        GROUP BY oi2.product.id
        ORDER BY freq DESC
        """)
    List<Object[]> findFrequentlyBoughtTogether(Long productId, Pageable pageable);

    // ── Trending products ─────────────────────────────────────────────────────

    @Query("""
        SELECT i.product.id, SUM(i.score) AS totalScore
        FROM UserProductInteraction i
        WHERE i.createdAt >= :since
        GROUP BY i.product.id
        ORDER BY totalScore DESC
        """)
    List<Object[]> findTrendingProductIds(LocalDateTime since, Pageable pageable);

    // ── Collaborative filtering — users who bought X also bought ─────────────

    @Query("""
        SELECT oi2.product.id, COUNT(DISTINCT oi2.order.user.id) AS userCount
        FROM OrderItem oi1
        JOIN OrderItem oi2 ON oi1.order.user.id = oi2.order.user.id
        WHERE oi1.product.id = :productId
          AND oi2.product.id <> :productId
          AND oi1.order.status
              = com.jullyscraft.entity.Order$OrderStatus.DELIVERED
        GROUP BY oi2.product.id
        ORDER BY userCount DESC
        """)
    List<Object[]> findCollaborativeProducts(Long productId, Pageable pageable);

    // ── Category affinity ─────────────────────────────────────────────────────

    @Query("""
        SELECT i.product.category.id, SUM(i.score) AS affinity
        FROM UserProductInteraction i
        WHERE i.user.id = :userId
        GROUP BY i.product.category.id
        ORDER BY affinity DESC
        """)
    List<Object[]> findCategoryAffinityByUser(Long userId, Pageable pageable);

    // ── Interaction exists check ──────────────────────────────────────────────

    boolean existsByUserIdAndProductIdAndType(
            Long userId, Long productId,
            UserProductInteraction.InteractionType type);

    long countByProductIdAndType(
            Long productId,
            UserProductInteraction.InteractionType type);
}