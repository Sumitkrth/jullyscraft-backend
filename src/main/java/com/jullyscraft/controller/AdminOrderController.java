package com.jullyscraft.controller;

import com.jullyscraft.dto.request.OrderStatusUpdateRequest;
import com.jullyscraft.dto.response.ApiResponse;
import com.jullyscraft.dto.response.OrderResponse;
import com.jullyscraft.dto.response.OrderSummaryResponse;
import com.jullyscraft.dto.response.PageResponse;
import com.jullyscraft.entity.Order;
import com.jullyscraft.service.OrderService;
import com.jullyscraft.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(AppConstants.ADMIN_BASE + "/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin — Orders", description = "Admin order management & status control")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "Get all orders with filters")
    public ResponseEntity<ApiResponse<PageResponse<OrderSummaryResponse>>> getAll(
            @RequestParam(required = false)    Order.OrderStatus status,
            @RequestParam(required = false)    String from,
            @RequestParam(required = false)    String to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getAllOrders(status, from, to, page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order detail by ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderById(id)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update order status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Order status updated",
                orderService.updateOrderStatus(id, request)));
    }

    @PatchMapping("/{id}/mark-paid")
    @Operation(summary = "Mark order as paid (manual / COD confirmed)")
    public ResponseEntity<ApiResponse<Void>> markPaid(
            @PathVariable Long id,
            @RequestParam String paymentId) {
        orderService.markAsPaid(id, paymentId);
        return ResponseEntity.ok(ApiResponse.success("Order marked as paid"));
    }
}