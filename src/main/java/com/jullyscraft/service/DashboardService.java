package com.jullyscraft.service;

import com.jullyscraft.dto.response.*;

import java.util.List;

public interface DashboardService {
    DashboardStatsResponse    getStats();
    RevenueReportResponse     getRevenueReport(String period, String from, String to);
    List<TopProductResponse>  getTopProducts(int limit, String from, String to);
    CustomerAnalyticsResponse getCustomerAnalytics();
    List<OrderSummaryResponse> getRecentOrders(int limit);
}