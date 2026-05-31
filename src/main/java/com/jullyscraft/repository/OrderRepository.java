package com.jullyscraft.repository;

import com.jullyscraft.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // ── User queries ──────────────────────────────────────────────────────────
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<Order> findByOrderNumber(String orderNumber);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    // ── Admin queries ─────────────────────────────────────────────────────────
    Page<Order> findByStatus(Order.OrderStatus status, Pageable pageable);

    @Query("""
        SELECT o FROM Order o
        WHERE (:status IS NULL OR o.status = :status)
          AND (:from   IS NULL OR o.createdAt >= :from)
          AND (:to     IS NULL OR o.createdAt <= :to)
        ORDER BY o.createdAt DESC
        """)
    Page<Order> findByFilters(Order.OrderStatus status,
                              LocalDateTime from,
                              LocalDateTime to,
                              Pageable pageable);

    // ── Analytics ─────────────────────────────────────────────────────────────
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    long countByStatus(Order.OrderStatus status);

    @Query("""
        SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o
        WHERE o.paymentStatus = com.jullyscraft.entity.Order$PaymentStatus.PAID
          AND o.createdAt BETWEEN :from AND :to
        """)
    java.math.BigDecimal sumRevenue(LocalDateTime from, LocalDateTime to);

    @Query("""
    SELECT COUNT(o.user.id), COUNT(o.id)
    FROM Order o
    WHERE o.user IS NOT NULL
      AND o.status != com.jullyscraft.entity.Order$OrderStatus.CANCELLED
    GROUP BY o.user.id
    """)
    List<Object[]> findOrderFrequencyDistribution();

    // Fetch with items eagerly
    @Query("""
        SELECT DISTINCT o FROM Order o
        LEFT JOIN FETCH o.items
        LEFT JOIN FETCH o.statusHistory
        WHERE o.id = :id
        """)
    Optional<Order> findByIdWithDetails(Long id);
}