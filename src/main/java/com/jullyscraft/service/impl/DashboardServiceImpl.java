package com.jullyscraft.service.impl;

import com.jullyscraft.dto.response.*;
import com.jullyscraft.entity.Order;
import com.jullyscraft.entity.OrderItem;
import com.jullyscraft.entity.Payment;
import com.jullyscraft.mapper.OrderMapper;
import com.jullyscraft.repository.*;
import com.jullyscraft.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final OrderRepository       orderRepository;
    private final OrderItemRepository   orderItemRepository;
    private final ProductRepository     productRepository;
    private final UserRepository        userRepository;
    private final CartRepository        cartRepository;
    private final CartItemRepository    cartItemRepository;
    private final ReviewRepository      reviewRepository;
    private final PaymentRepository     paymentRepository;
    private final OrderMapper           orderMapper;

    // ── Dashboard stats ───────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "dashboard", key = "'stats'")
    public DashboardStatsResponse getStats() {
        LocalDateTime now       = LocalDateTime.now();
        LocalDateTime todayStart= now.toLocalDate().atStartOfDay();
        LocalDateTime weekStart = todayStart.minusDays(7);
        LocalDateTime monthStart= now.toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime lastMonthStart = monthStart.minusMonths(1);

        // Revenue
        BigDecimal revenueAllTime   = orderRepository.sumRevenue(
                LocalDateTime.of(2020, 1, 1, 0, 0), now);
        BigDecimal revenueToday     = orderRepository.sumRevenue(todayStart, now);
        BigDecimal revenueWeek      = orderRepository.sumRevenue(weekStart,  now);
        BigDecimal revenueMonth     = orderRepository.sumRevenue(monthStart, now);
        BigDecimal revenueLastMonth = orderRepository.sumRevenue(
                lastMonthStart, monthStart);

        double growth = calcGrowth(revenueLastMonth, revenueMonth);

        // Orders
        long totalOrders    = orderRepository.count();
        long ordersToday    = orderRepository.countByStatus(Order.OrderStatus.PENDING)
                + orderRepository.countByStatus(Order.OrderStatus.PAID);
        long ordersThisMonth= orderRepository.findByFilters(
                null, monthStart, now,
                PageRequest.of(0, 1)).getTotalElements();
        long pending        = orderRepository.countByStatus(Order.OrderStatus.PENDING);
        long processing     = orderRepository.countByStatus(Order.OrderStatus.PROCESSING);
        long shipped        = orderRepository.countByStatus(Order.OrderStatus.SHIPPED);
        long cancelled      = orderRepository.countByStatus(Order.OrderStatus.CANCELLED);
        long returned       = orderRepository.countByStatus(Order.OrderStatus.RETURNED);

        // Products
        long totalProducts  = productRepository.count();
        long activeProducts = productRepository
                .findByActiveTrueAndDeletedFalse(PageRequest.of(0, 1))
                .getTotalElements();
        long outOfStock = productRepository.findByDeletedFalse(PageRequest.of(0, 1))
                .getTotalElements(); // simplified — refine with stock query

        // Users
        long totalUsers       = userRepository.count();
        long newUsersToday    = countNewUsers(todayStart, now);
        long newUsersMonth    = countNewUsers(monthStart, now);

        // Reviews
        long pendingReviews   = reviewRepository
                .findByStatusAndDeletedFalse(
                        com.jullyscraft.entity.Review.ReviewStatus.PENDING,
                        PageRequest.of(0, 1))
                .getTotalElements();
        long spamReviews = reviewRepository
                .findBySpamFlaggedTrueAndDeletedFalse(PageRequest.of(0, 1))
                .getTotalElements();

        // Cart abandonment
        long cartsWithItems   = cartRepository.count();
        long abandonedCarts   = Math.max(0, cartsWithItems - ordersThisMonth);
        double abandonment    = cartsWithItems > 0
                ? (double) abandonedCarts / cartsWithItems * 100 : 0;

        return DashboardStatsResponse.builder()
                .totalRevenueAllTime(safeDecimal(revenueAllTime))
                .revenueToday(safeDecimal(revenueToday))
                .revenueThisWeek(safeDecimal(revenueWeek))
                .revenueThisMonth(safeDecimal(revenueMonth))
                .revenueLastMonth(safeDecimal(revenueLastMonth))
                .revenueGrowthPercent(growth)
                .totalOrders(totalOrders)
                .ordersToday(ordersToday)
                .ordersThisMonth(ordersThisMonth)
                .pendingOrders(pending)
                .processingOrders(processing)
                .shippedOrders(shipped)
                .cancelledOrders(cancelled)
                .returnedOrders(returned)
                .totalProducts(totalProducts)
                .activeProducts(activeProducts)
                .outOfStockProducts(outOfStock)
                .lowStockProducts(0L)           // TODO: wire low-stock query
                .totalUsers(totalUsers)
                .newUsersToday(newUsersToday)
                .newUsersThisMonth(newUsersMonth)
                .activeUsersThisMonth(newUsersMonth)
                .pendingReviews(pendingReviews)
                .spamFlaggedReviews(spamReviews)
                .cartsWithItems(cartsWithItems)
                .abandonedCarts(abandonedCarts)
                .cartAbandonmentRate(Math.round(abandonment * 10.0) / 10.0)
                .build();
    }

    // ── Revenue report ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public RevenueReportResponse getRevenueReport(String period,
                                                  String from, String to) {
        DateTimeFormatter fmt   = DateTimeFormatter.ISO_LOCAL_DATE;
        LocalDateTime     fromDt = from != null
                ? LocalDate.parse(from, fmt).atStartOfDay()
                : LocalDateTime.now().minusDays(30);
        LocalDateTime     toDt   = to   != null
                ? LocalDate.parse(to, fmt).atTime(23, 59, 59)
                : LocalDateTime.now();

        List<String>       labels      = new ArrayList<>();
        List<BigDecimal>   revenues    = new ArrayList<>();
        List<Long>         orderCounts = new ArrayList<>();

        // Build time-series buckets
        LocalDateTime cursor = fromDt;
        DateTimeFormatter labelFmt = switch (period) {
            case "weekly"  -> DateTimeFormatter.ofPattern("yyyy-'W'ww");
            case "monthly" -> DateTimeFormatter.ofPattern("yyyy-MM");
            default        -> DateTimeFormatter.ofPattern("yyyy-MM-dd");
        };

        while (!cursor.isAfter(toDt)) {
            LocalDateTime bucketEnd = switch (period) {
                case "weekly"  -> cursor.plusWeeks(1);
                case "monthly" -> cursor.plusMonths(1);
                default        -> cursor.plusDays(1);
            };
            bucketEnd = bucketEnd.isAfter(toDt) ? toDt : bucketEnd;

            labels.add(cursor.format(labelFmt));
            BigDecimal rev = orderRepository.sumRevenue(cursor, bucketEnd);
            revenues.add(safeDecimal(rev));
            orderCounts.add(orderRepository.findByFilters(
                            null, cursor, bucketEnd, PageRequest.of(0, 1))
                    .getTotalElements());

            cursor = switch (period) {
                case "weekly"  -> cursor.plusWeeks(1);
                case "monthly" -> cursor.plusMonths(1);
                default        -> cursor.plusDays(1);
            };
        }

        BigDecimal total = revenues.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalOrd = orderCounts.stream().mapToLong(Long::longValue).sum();
        BigDecimal aov = totalOrd > 0
                ? total.divide(BigDecimal.valueOf(totalOrd), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return RevenueReportResponse.builder()
                .period(period)
                .labels(labels)
                .revenueData(revenues)
                .orderCountData(orderCounts)
                .totalRevenue(total)
                .totalOrders(totalOrd)
                .averageOrderValue(aov)
                .revenueByCategory(Map.of())    // TODO: join order_items → products → categories
                .revenueByPaymentMethod(Map.of()) // TODO: join payments
                .build();
    }

    // ── Top products ──────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "dashboard", key = "'top-products:' + #limit")
    public List<TopProductResponse> getTopProducts(int limit,
                                                   String from, String to) {
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        LocalDateTime fromDt  = from != null
                ? LocalDate.parse(from, fmt).atStartOfDay()
                : LocalDateTime.now().minusDays(30);
        LocalDateTime toDt = to != null
                ? LocalDate.parse(to, fmt).atTime(23, 59, 59)
                : LocalDateTime.now();

        // Aggregate order items in the period
        List<OrderItem> items = orderItemRepository.findAll().stream()
                .filter(i -> i.getOrder().getCreatedAt().isAfter(fromDt)
                        && i.getOrder().getCreatedAt().isBefore(toDt)
                        && i.getOrder().getStatus()
                        != Order.OrderStatus.CANCELLED)
                .toList();

        // Group by product
        Map<Long, List<OrderItem>> byProduct = items.stream()
                .collect(Collectors.groupingBy(i -> i.getProduct().getId()));

        return byProduct.entrySet().stream()
                .map(e -> {
                    Long productId = e.getKey();
                    List<OrderItem> pItems = e.getValue();

                    long unitsSold = pItems.stream()
                            .mapToLong(OrderItem::getQuantity).sum();
                    BigDecimal revenue = pItems.stream()
                            .map(OrderItem::getLineTotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return productRepository.findById(productId)
                            .map(p -> TopProductResponse.builder()
                                    .productId(p.getId())
                                    .productName(p.getName())
                                    .productSlug(p.getSlug())
                                    .primaryImageUrl(p.getImages().stream()
                                            .filter(com.jullyscraft.entity.ProductImage::isPrimaryImage)
                                            .map(com.jullyscraft.entity.ProductImage::getImageUrl)
                                            .findFirst().orElse(null))
                                    .categoryName(p.getCategory().getName())
                                    .unitsSold(unitsSold)
                                    .revenue(revenue)
                                    .averageRating(p.getAverageRating())
                                    .reviewCount(p.getReviewCount())
                                    .currentStock(p.getStockQuantity())
                                    .build())
                            .orElse(null);
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingLong(
                        TopProductResponse::getUnitsSold).reversed())
                .limit(limit)
                .toList();
    }

    // ── Customer analytics ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "dashboard", key = "'customer-analytics'")
    public CustomerAnalyticsResponse getCustomerAnalytics() {
        long total    = userRepository.count();
        LocalDateTime monthStart = LocalDateTime.now()
                .toLocalDate().withDayOfMonth(1).atStartOfDay();
        long newThisMonth = countNewUsers(monthStart, LocalDateTime.now());

        // Returning = users with more than 1 order
        List<Object[]> orderFreq = getOrderFrequency();
        long returning = orderFreq.stream()
                .filter(row -> ((Number) row[1]).longValue() > 1)
                .mapToLong(row -> ((Number) row[0]).longValue())
                .sum();

        double retention = total > 0
                ? Math.round((double) returning / total * 1000.0) / 10.0
                : 0;

        Map<Integer, Long> freqDist = new LinkedHashMap<>();
        orderFreq.forEach(row -> freqDist.put(
                ((Number) row[1]).intValue(),
                ((Number) row[0]).longValue()));

        return CustomerAnalyticsResponse.builder()
                .totalCustomers(total)
                .newCustomersThisMonth(newThisMonth)
                .returningCustomers(returning)
                .retentionRate(retention)
                .averageLifetimeValue(BigDecimal.ZERO) // TODO: sum(orders)/users
                .customersByCity(Map.of())             // TODO: join addresses
                .topCities(List.of())
                .orderFrequencyDistribution(freqDist)
                .build();
    }

    // ── Recent orders ─────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> getRecentOrders(int limit) {
        return orderRepository.findAll(
                        PageRequest.of(0, limit,
                                Sort.by("createdAt").descending()))
                .getContent()
                .stream()
                .map(orderMapper::toSummary)
                .toList();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private long countNewUsers(LocalDateTime from, LocalDateTime to) {
        // Spring Data derived query — add to UserRepository
        return userRepository.countByCreatedAtBetween(from, to);
    }

    private List<Object[]> getOrderFrequency() {
        // Add this query to OrderRepository
        return orderRepository.findOrderFrequencyDistribution();
    }

    private double calcGrowth(BigDecimal prev, BigDecimal curr) {
        if (prev == null || prev.compareTo(BigDecimal.ZERO) == 0) return 0;
        return curr.subtract(prev)
                .multiply(BigDecimal.valueOf(100))
                .divide(prev, 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private BigDecimal safeDecimal(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }
}