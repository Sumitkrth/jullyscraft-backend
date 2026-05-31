package com.jullyscraft.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class RevenueReportResponse {
    private String             period;          // daily | weekly | monthly
    private List<String>       labels;          // date labels
    private List<BigDecimal>   revenueData;     // revenue per label
    private List<Long>         orderCountData;  // orders per label
    private BigDecimal         totalRevenue;
    private long               totalOrders;
    private BigDecimal         averageOrderValue;
    private Map<String, BigDecimal> revenueByCategory;
    private Map<String, BigDecimal> revenueByPaymentMethod;
}