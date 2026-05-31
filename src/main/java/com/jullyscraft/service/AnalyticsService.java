package com.jullyscraft.service;

import com.jullyscraft.dto.response.*;
import com.jullyscraft.entity.AnalyticsEvent;

public interface AnalyticsService {

    // ── Event ingestion ───────────────────────────────────────────────────────
    void track(Long userId, String sessionId,
               AnalyticsEvent.EventType type,
               Long productId, Long orderId,
               String deviceType, String metadata);

    // ── Reports ───────────────────────────────────────────────────────────────
    SalesReportResponse      getSalesReport(String from, String to, String period);
    ConversionReportResponse getConversionReport(String from, String to);
    UserBehaviorResponse     getUserBehaviorReport(String from, String to);
    InventoryReportResponse  getInventoryReport();

    // ── Real-time ─────────────────────────────────────────────────────────────
    long getActiveUsersNow();
    long getTodayOrders();
    long getTodayRevenue();

    // ── AI insights ───────────────────────────────────────────────────────────
    AiInsightResponse getAiInsights(String from, String to);
}