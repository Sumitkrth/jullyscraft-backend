package com.jullyscraft.repository;

import com.jullyscraft.entity.AnalyticsEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnalyticsEventRepository
        extends JpaRepository<AnalyticsEvent, Long> {

    // ── Event counts ──────────────────────────────────────────────────────────

    long countByEventTypeAndCreatedAtBetween(
            AnalyticsEvent.EventType type,
            LocalDateTime from, LocalDateTime to);

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    // ── Funnel: checkout conversion ───────────────────────────────────────────

    @Query("""
        SELECT e.eventType, COUNT(DISTINCT e.sessionId)
        FROM AnalyticsEvent e
        WHERE e.eventType IN (
            com.jullyscraft.entity.AnalyticsEvent$EventType.PRODUCT_VIEW,
            com.jullyscraft.entity.AnalyticsEvent$EventType.ADD_TO_CART,
            com.jullyscraft.entity.AnalyticsEvent$EventType.CHECKOUT_START,
            com.jullyscraft.entity.AnalyticsEvent$EventType.ORDER_PLACED
        )
        AND e.createdAt BETWEEN :from AND :to
        GROUP BY e.eventType
        """)
    List<Object[]> findFunnelData(LocalDateTime from, LocalDateTime to);

    // ── Top viewed products ───────────────────────────────────────────────────

    @Query("""
        SELECT e.productId, COUNT(e.productId) AS views
        FROM AnalyticsEvent e
        WHERE e.eventType =
            com.jullyscraft.entity.AnalyticsEvent$EventType.PRODUCT_VIEW
          AND e.productId IS NOT NULL
          AND e.createdAt BETWEEN :from AND :to
        GROUP BY e.productId
        ORDER BY views DESC
        """)
    List<Object[]> findTopViewedProducts(
            LocalDateTime from, LocalDateTime to, Pageable pageable);

    // ── Search queries ────────────────────────────────────────────────────────

    @Query("""
        SELECT e.metadata, COUNT(e.metadata) AS freq
        FROM AnalyticsEvent e
        WHERE e.eventType =
            com.jullyscraft.entity.AnalyticsEvent$EventType.SEARCH
          AND e.createdAt BETWEEN :from AND :to
        GROUP BY e.metadata
        ORDER BY freq DESC
        """)
    List<Object[]> findTopSearchQueries(
            LocalDateTime from, LocalDateTime to, Pageable pageable);

    // ── Device breakdown ──────────────────────────────────────────────────────

    @Query("""
        SELECT e.deviceType, COUNT(e.deviceType)
        FROM AnalyticsEvent e
        WHERE e.createdAt BETWEEN :from AND :to
          AND e.deviceType IS NOT NULL
        GROUP BY e.deviceType
        """)
    List<Object[]> findDeviceBreakdown(LocalDateTime from, LocalDateTime to);

    // ── Active sessions (real-time) ───────────────────────────────────────────

    @Query("""
        SELECT COUNT(DISTINCT e.sessionId)
        FROM AnalyticsEvent e
        WHERE e.createdAt >= :since
        """)
    long countActiveSessions(LocalDateTime since);

    // ── Daily event counts for chart ──────────────────────────────────────────

    @Query("""
        SELECT CAST(e.createdAt AS date), COUNT(e.id)
        FROM AnalyticsEvent e
        WHERE e.eventType = :type
          AND e.createdAt BETWEEN :from AND :to
        GROUP BY CAST(e.createdAt AS date)
        ORDER BY CAST(e.createdAt AS date)
        """)
    List<Object[]> findDailyEventCounts(
            AnalyticsEvent.EventType type,
            LocalDateTime from, LocalDateTime to);

    // ── Bounce rate: single-page sessions ─────────────────────────────────────

    @Query("""
    SELECT COUNT(DISTINCT e.sessionId)
    FROM AnalyticsEvent e
    WHERE e.createdAt BETWEEN :from AND :to
    GROUP BY e.sessionId
    HAVING COUNT(e.id) = 1
    """)
    List<Long> countBounceSessionsRaw(LocalDateTime from, LocalDateTime to);
}