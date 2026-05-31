package com.jullyscraft.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class DashboardStatsResponse {

    // ── Revenue ───────────────────────────────────────────────────────────────
    private BigDecimal totalRevenueAllTime;
    private BigDecimal revenueToday;
    private BigDecimal revenueThisWeek;
    private BigDecimal revenueThisMonth;
    private BigDecimal revenueLastMonth;
    private double     revenueGrowthPercent;     // month-over-month

    // ── Orders ────────────────────────────────────────────────────────────────
    private long totalOrders;
    private long ordersToday;
    private long ordersThisMonth;
    private long pendingOrders;
    private long processingOrders;
    private long shippedOrders;
    private long cancelledOrders;
    private long returnedOrders;

    // ── Products ──────────────────────────────────────────────────────────────
    private long totalProducts;
    private long activeProducts;
    private long outOfStockProducts;
    private long lowStockProducts;

    // ── Users ─────────────────────────────────────────────────────────────────
    private long totalUsers;
    private long newUsersToday;
    private long newUsersThisMonth;
    private long activeUsersThisMonth;

    // ── Reviews ───────────────────────────────────────────────────────────────
    private long pendingReviews;
    private long spamFlaggedReviews;

    // ── Cart abandonment ──────────────────────────────────────────────────────
    private long   cartsWithItems;
    private long   abandonedCarts;
    private double cartAbandonmentRate;
}