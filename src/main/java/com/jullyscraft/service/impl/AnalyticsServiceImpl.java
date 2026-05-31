package com.jullyscraft.service.impl;

import com.jullyscraft.dto.response.*;
import com.jullyscraft.entity.AnalyticsEvent;
import com.jullyscraft.entity.Order;
import com.jullyscraft.entity.User;
import com.jullyscraft.repository.*;
import com.jullyscraft.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final AnalyticsEventRepository eventRepository;
    private final OrderRepository          orderRepository;
    private final ProductRepository        productRepository;
    private final UserRepository           userRepository;
    private final AiInsightEngine          aiEngine;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String REALTIME_KEY = "analytics:realtime:";

    // ── Event ingestion ───────────────────────────────────────────────────────

    @Override
    @Async("notificationExecutor")
    @Transactional
    public void track(Long userId, String sessionId,
                      AnalyticsEvent.EventType type,
                      Long productId, Long orderId,
                      String deviceType, String metadata) {
        try {
            AnalyticsEvent event = AnalyticsEvent.builder()
                    .eventType(type)
                    .sessionId(sessionId)
                    .productId(productId)
                    .orderId(orderId)
                    .deviceType(deviceType)
                    .metadata(metadata)
                    .build();

            if (userId != null) {
                event.setUser(User.builder().build());
            }

            eventRepository.save(event);

            // Real-time counters in Redis
            String key = REALTIME_KEY + type.name().toLowerCase();
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, 24, TimeUnit.HOURS);

            // Active session tracking
            if (sessionId != null) {
                redisTemplate.opsForValue().set(
                        "session:active:" + sessionId,
                        System.currentTimeMillis(),
                        5, TimeUnit.MINUTES);
            }

        } catch (Exception e) {
            log.error("Analytics track failed: {}", e.getMessage());
        }
    }

    // ── Sales report ──────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public SalesReportResponse getSalesReport(String from, String to,
                                              String period) {
        LocalDateTime fromDt = parseDt(from, LocalDateTime.now().minusDays(30));
        LocalDateTime toDt   = parseDt(to,   LocalDateTime.now());

        // Revenue & orders
        BigDecimal totalRevenue = orderRepository.sumRevenue(fromDt, toDt);
        long       totalOrders  = orderRepository.findByFilters(
                null, fromDt, toDt, PageRequest.of(0, 1)).getTotalElements();

        BigDecimal aov = totalOrders > 0
                ? safeDecimal(totalRevenue).divide(
                BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Refunds
        BigDecimal refunds = orderRepository.sumRevenue(fromDt, toDt); // TODO: refund-specific query
        BigDecimal net     = safeDecimal(totalRevenue);

        // Time series
        List<String>     labels     = new ArrayList<>();
        List<BigDecimal> dailyRev   = new ArrayList<>();
        List<Long>       dailyOrd   = new ArrayList<>();

        LocalDateTime cursor = fromDt;
        DateTimeFormatter lf = period.equals("monthly")
                ? DateTimeFormatter.ofPattern("yyyy-MM")
                : DateTimeFormatter.ofPattern("MM-dd");

        while (!cursor.isAfter(toDt)) {
            LocalDateTime next = period.equals("monthly")
                    ? cursor.plusMonths(1) : cursor.plusDays(1);
            LocalDateTime end  = next.isAfter(toDt) ? toDt : next;

            labels.add(cursor.format(lf));
            BigDecimal rev = orderRepository.sumRevenue(cursor, end);
            dailyRev.add(safeDecimal(rev));
            dailyOrd.add(orderRepository.findByFilters(
                            null, cursor, end, PageRequest.of(0, 1))
                    .getTotalElements());

            cursor = next;
        }

        // Order status breakdown
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (Order.OrderStatus s : Order.OrderStatus.values()) {
            byStatus.put(s.name(), orderRepository.countByStatus(s));
        }

        return SalesReportResponse.builder()
                .period(period)
                .totalRevenue(safeDecimal(totalRevenue))
                .totalOrders(totalOrders)
                .averageOrderValue(aov)
                .totalDiscount(BigDecimal.ZERO)     // TODO: coupon usage sum
                .totalRefunds(BigDecimal.ZERO)       // TODO: refund sum
                .netRevenue(net)
                .labels(labels)
                .dailyRevenue(dailyRev)
                .dailyOrders(dailyOrd)
                .revenueByCategory(Map.of())         // TODO: join order_items
                .revenueByPaymentMethod(Map.of())
                .ordersByStatus(byStatus)
                .topProducts(List.of())
                .build();
    }

    // ── Conversion report ─────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ConversionReportResponse getConversionReport(String from, String to) {
        LocalDateTime fromDt = parseDt(from, LocalDateTime.now().minusDays(30));
        LocalDateTime toDt   = parseDt(to,   LocalDateTime.now());

        // Funnel data from analytics events
        Map<String, Long> funnelMap = new HashMap<>();
        eventRepository.findFunnelData(fromDt, toDt)
                .forEach(row -> funnelMap.put(row[0].toString(),
                        ((Number) row[1]).longValue()));

        long views        = funnelMap.getOrDefault(
                AnalyticsEvent.EventType.PRODUCT_VIEW.name(), 0L);
        long cartAdds     = funnelMap.getOrDefault(
                AnalyticsEvent.EventType.ADD_TO_CART.name(), 0L);
        long checkouts    = funnelMap.getOrDefault(
                AnalyticsEvent.EventType.CHECKOUT_START.name(), 0L);
        long orders       = funnelMap.getOrDefault(
                AnalyticsEvent.EventType.ORDER_PLACED.name(), 0L);

        // Rates
        double v2c  = rate(cartAdds,  views);
        double c2ch = rate(checkouts, cartAdds);
        double ch2o = rate(orders,    checkouts);
        double overall = rate(orders, views);

        // Sessions
        long totalSessions = eventRepository.countActiveSessions(fromDt);
        long bounces = eventRepository.countBounceSessionsRaw(fromDt, toDt).size();
        double bounceRate  = rate(bounces, totalSessions);

        // Cart abandonment
        long cartAbandons  = Math.max(0, cartAdds - orders);
        double abandonment = rate(cartAbandons, cartAdds);

        // Device breakdown
        Map<String, Long> devices = new LinkedHashMap<>();
        eventRepository.findDeviceBreakdown(fromDt, toDt)
                .forEach(row -> devices.put(
                        row[0] != null ? row[0].toString() : "unknown",
                        ((Number) row[1]).longValue()));

        return ConversionReportResponse.builder()
                .productViews(views)
                .addToCartEvents(cartAdds)
                .checkoutStarts(checkouts)
                .ordersPlaced(orders)
                .viewToCartRate(v2c)
                .cartToCheckoutRate(c2ch)
                .checkoutToOrderRate(ch2o)
                .overallConversionRate(overall)
                .totalSessions(totalSessions)
                .bounceSessions(bounces)
                .bounceRate(bounceRate)
                .deviceBreakdown(devices)
                .cartAbandonments(cartAbandons)
                .cartAbandonmentRate(abandonment)
                .build();
    }

    // ── User behavior report ─────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UserBehaviorResponse getUserBehaviorReport(String from, String to) {
        LocalDateTime fromDt = parseDt(from, LocalDateTime.now().minusDays(7));
        LocalDateTime toDt   = parseDt(to,   LocalDateTime.now());
        LocalDateTime monthStart = LocalDateTime.now()
                .toLocalDate().withDayOfMonth(1).atStartOfDay();

        long pageViews  = eventRepository.countByEventTypeAndCreatedAtBetween(
                AnalyticsEvent.EventType.PAGE_VIEW, fromDt, toDt);
        long sessions   = eventRepository.countActiveSessions(fromDt);
        long activeNow  = getActiveUsersNow();
        long activeToday= eventRepository.countActiveSessions(
                LocalDateTime.now().toLocalDate().atStartOfDay());

        // Top searches
        List<String> topSearches = eventRepository.findTopSearchQueries(
                        fromDt, toDt, PageRequest.of(0, 10))
                .stream()
                .map(row -> row[0] != null ? row[0].toString() : "")
                .filter(s -> !s.isBlank())
                .toList();

        // Top viewed products
        List<Long> topViewed = eventRepository.findTopViewedProducts(
                        fromDt, toDt, PageRequest.of(0, 10))
                .stream()
                .map(row -> ((Number) row[0]).longValue())
                .toList();

        // Device breakdown
        Map<String, Long> devices = new LinkedHashMap<>();
        eventRepository.findDeviceBreakdown(fromDt, toDt)
                .forEach(row -> devices.put(
                        row[0] != null ? row[0].toString() : "unknown",
                        ((Number) row[1]).longValue()));

        // New vs returning
        long newUsers       = userRepository.countByCreatedAtBetween(fromDt, toDt);
        long totalUsers     = userRepository.count();
        long returningUsers = Math.max(0, totalUsers - newUsers);
        double newRate      = rate(newUsers, totalUsers);

        return UserBehaviorResponse.builder()
                .totalPageViews(pageViews)
                .uniqueSessions(sessions)
                .activeUsersNow(activeNow)
                .activeUsersToday(activeToday)
                .avgSessionDuration(4.5)            // TODO: compute from session events
                .topSearchQueries(topSearches)
                .topViewedProductIds(topViewed)
                .deviceBreakdown(devices)
                .hourlyTraffic(Map.of())             // TODO: hourly bucket query
                .newUsersCount(newUsers)
                .returningUsersCount(returningUsers)
                .newUserRate(newRate)
                .build();
    }

    // ── Inventory report ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public InventoryReportResponse getInventoryReport() {
        long totalProducts = productRepository
                .findByDeletedFalse(PageRequest.of(0, 1)).getTotalElements();
        long active = productRepository
                .findByActiveTrueAndDeletedFalse(PageRequest.of(0, 1))
                .getTotalElements();

        // Out of stock
        List<InventoryReportResponse.StockAlert> outOfStock =
                productRepository.findAll().stream()
                        .filter(p -> !p.isDeleted() && p.isActive()
                                && p.getStockQuantity() == 0)
                        .map(p -> InventoryReportResponse.StockAlert.builder()
                                .productId(p.getId())
                                .productName(p.getName())
                                .currentStock(p.getStockQuantity())
                                .threshold(p.getLowStockThreshold())
                                .categoryName(p.getCategory().getName())
                                .build())
                        .toList();

        // Low stock
        List<InventoryReportResponse.StockAlert> lowStock =
                productRepository.findAll().stream()
                        .filter(p -> !p.isDeleted() && p.isActive()
                                && p.getStockQuantity() > 0
                                && p.getStockQuantity() <= p.getLowStockThreshold())
                        .map(p -> InventoryReportResponse.StockAlert.builder()
                                .productId(p.getId())
                                .productName(p.getName())
                                .currentStock(p.getStockQuantity())
                                .threshold(p.getLowStockThreshold())
                                .categoryName(p.getCategory().getName())
                                .build())
                        .toList();

        // Inventory value
        BigDecimal totalValue = productRepository.findAll().stream()
                .filter(p -> !p.isDeleted() && p.isActive())
                .map(p -> p.getEffectivePrice()
                        .multiply(BigDecimal.valueOf(p.getStockQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return InventoryReportResponse.builder()
                .totalSkus(totalProducts)
                .inStockSkus(active - outOfStock.size())
                .outOfStockSkus(outOfStock.size())
                .lowStockSkus(lowStock.size())
                .totalInventoryValue(totalValue)
                .outOfStockProducts(outOfStock)
                .lowStockProducts(lowStock)
                .build();
    }

    // ── Real-time ─────────────────────────────────────────────────────────────

    @Override
    public long getActiveUsersNow() {
        try {
            // Count active session keys in Redis (last 5 min)
            Set<String> keys = redisTemplate.keys("session:active:*");
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            return eventRepository.countActiveSessions(
                    LocalDateTime.now().minusMinutes(5));
        }
    }

    @Override
    public long getTodayOrders() {
        LocalDateTime start = LocalDateTime.now()
                .toLocalDate().atStartOfDay();
        return orderRepository.findByFilters(
                        null, start, LocalDateTime.now(), PageRequest.of(0, 1))
                .getTotalElements();
    }

    @Override
    public long getTodayRevenue() {
        LocalDateTime start = LocalDateTime.now()
                .toLocalDate().atStartOfDay();
        BigDecimal rev = orderRepository.sumRevenue(start, LocalDateTime.now());
        return rev != null ? rev.longValue() : 0L;
    }

    // ── AI insights ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AiInsightResponse getAiInsights(String from, String to) {
        SalesReportResponse    sales      = getSalesReport(from, to, "daily");
        ConversionReportResponse conversion = getConversionReport(from, to);
        UserBehaviorResponse   behavior   = getUserBehaviorReport(from, to);
        return aiEngine.generateInsights(sales, conversion, behavior);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private LocalDateTime parseDt(String s, LocalDateTime fallback) {
        if (s == null || s.isBlank()) return fallback;
        try {
            return LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE)
                    .atStartOfDay();
        } catch (Exception e) {
            return fallback;
        }
    }

    private BigDecimal safeDecimal(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }

    private double rate(long numerator, long denominator) {
        if (denominator == 0) return 0.0;
        return Math.round(
                (double) numerator / denominator * 10000.0) / 100.0;
    }
}