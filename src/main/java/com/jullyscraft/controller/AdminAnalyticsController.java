package com.jullyscraft.controller;

import com.jullyscraft.dto.response.*;
import com.jullyscraft.entity.AnalyticsEvent;
import com.jullyscraft.service.AnalyticsService;
import com.jullyscraft.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(AppConstants.ADMIN_BASE + "/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin — Analytics", description = "Sales, conversion, behaviour, and AI insights")
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;

    // ── Sales ─────────────────────────────────────────────────────────────────

    @GetMapping("/sales")
    @Operation(summary = "Sales report — revenue, orders, AOV, time series")
    public ResponseEntity<ApiResponse<SalesReportResponse>> getSales(
            @RequestParam(required = false)        String from,
            @RequestParam(required = false)        String to,
            @RequestParam(defaultValue = "daily")  String period) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getSalesReport(from, to, period)));
    }

    // ── Conversion funnel ─────────────────────────────────────────────────────

    @GetMapping("/conversion")
    @Operation(summary = "Conversion funnel — view → cart → checkout → order")
    public ResponseEntity<ApiResponse<ConversionReportResponse>> getConversion(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getConversionReport(from, to)));
    }

    // ── User behaviour ────────────────────────────────────────────────────────

    @GetMapping("/behaviour")
    @Operation(summary = "User behaviour — sessions, devices, searches, traffic")
    public ResponseEntity<ApiResponse<UserBehaviorResponse>> getBehaviour(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getUserBehaviorReport(from, to)));
    }

    // ── Inventory ─────────────────────────────────────────────────────────────

    @GetMapping("/inventory")
    @Operation(summary = "Inventory health — stock levels, value, alerts")
    public ResponseEntity<ApiResponse<InventoryReportResponse>> getInventory() {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getInventoryReport()));
    }

    // ── Real-time ─────────────────────────────────────────────────────────────

    @GetMapping("/realtime")
    @Operation(summary = "Real-time snapshot — active users, today's orders and revenue")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getRealtime() {
        return ResponseEntity.ok(ApiResponse.success(
                java.util.Map.of(
                        "activeUsersNow",  analyticsService.getActiveUsersNow(),
                        "ordersToday",     analyticsService.getTodayOrders(),
                        "revenueToday",    analyticsService.getTodayRevenue())));
    }

    // ── AI insights ───────────────────────────────────────────────────────────

    @GetMapping("/ai-insights")
    @Operation(summary = "AI-generated business insights powered by Claude")
    public ResponseEntity<ApiResponse<AiInsightResponse>> getAiInsights(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getAiInsights(from, to)));
    }

    // ── Event tracking (for frontend) ─────────────────────────────────────────

    @PostMapping("/track")
    @Operation(summary = "Ingest a frontend analytics event")
    @PreAuthorize("permitAll()")     // override — open for frontend
    public ResponseEntity<ApiResponse<Void>> track(
            @RequestParam AnalyticsEvent.EventType type,
            @RequestParam(required = false) String  sessionId,
            @RequestParam(required = false) Long    productId,
            @RequestParam(required = false) Long    orderId,
            @RequestParam(required = false) String  deviceType,
            @RequestParam(required = false) String  metadata,
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            com.jullyscraft.security.userdetails.UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        analyticsService.track(userId, sessionId, type,
                productId, orderId, deviceType, metadata);
        return ResponseEntity.ok(ApiResponse.success("Event tracked"));
    }
}