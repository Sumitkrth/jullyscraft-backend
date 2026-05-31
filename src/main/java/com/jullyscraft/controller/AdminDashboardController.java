package com.jullyscraft.controller;

import com.jullyscraft.dto.response.*;
import com.jullyscraft.service.DashboardService;
import com.jullyscraft.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(AppConstants.ADMIN_BASE + "/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin — Dashboard", description = "Analytics, revenue reports, and business insights")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @Operation(summary = "Get overall platform KPIs")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getStats() {
        return ResponseEntity.ok(
                ApiResponse.success(dashboardService.getStats()));
    }

    @GetMapping("/revenue")
    @Operation(summary = "Revenue report — daily / weekly / monthly breakdown")
    public ResponseEntity<ApiResponse<RevenueReportResponse>> getRevenue(
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(required = false)        String from,
            @RequestParam(required = false)        String to) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getRevenueReport(period, from, to)));
    }

    @GetMapping("/top-products")
    @Operation(summary = "Top selling products by units sold")
    public ResponseEntity<ApiResponse<List<TopProductResponse>>> getTopProducts(
            @RequestParam(defaultValue = "10")     int    limit,
            @RequestParam(required = false)        String from,
            @RequestParam(required = false)        String to) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getTopProducts(limit, from, to)));
    }

    @GetMapping("/customers")
    @Operation(summary = "Customer retention and analytics")
    public ResponseEntity<ApiResponse<CustomerAnalyticsResponse>> getCustomers() {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getCustomerAnalytics()));
    }

    @GetMapping("/recent-orders")
    @Operation(summary = "Latest orders across the platform")
    public ResponseEntity<ApiResponse<List<OrderSummaryResponse>>> getRecentOrders(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getRecentOrders(limit)));
    }
}