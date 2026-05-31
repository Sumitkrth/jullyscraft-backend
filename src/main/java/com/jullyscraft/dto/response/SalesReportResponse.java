package com.jullyscraft.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class SalesReportResponse {

    private String             period;
    private BigDecimal         totalRevenue;
    private long               totalOrders;
    private BigDecimal         averageOrderValue;
    private BigDecimal         totalDiscount;
    private BigDecimal         totalRefunds;
    private BigDecimal         netRevenue;

    // ── Time series ───────────────────────────────────────────────────────────
    private List<String>       labels;
    private List<BigDecimal>   dailyRevenue;
    private List<Long>         dailyOrders;

    // ── Breakdowns ────────────────────────────────────────────────────────────
    private Map<String, BigDecimal> revenueByCategory;
    private Map<String, BigDecimal> revenueByPaymentMethod;
    private Map<String, Long>       ordersByStatus;
    private List<TopProductResponse> topProducts;
}