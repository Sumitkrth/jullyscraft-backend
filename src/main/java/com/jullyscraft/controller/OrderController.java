package com.jullyscraft.controller;

import com.jullyscraft.dto.request.PlaceOrderRequest;
import com.jullyscraft.dto.response.ApiResponse;
import com.jullyscraft.dto.response.OrderResponse;
import com.jullyscraft.dto.response.OrderSummaryResponse;
import com.jullyscraft.dto.response.PageResponse;
import com.jullyscraft.security.userdetails.UserPrincipal;
import com.jullyscraft.service.OrderService;
import com.jullyscraft.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(AppConstants.ORDER_BASE)
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Orders", description = "Place and manage orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Place order from cart")
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PlaceOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Order placed successfully",
                        orderService.placeOrder(principal.getId(), request)));
    }

    @GetMapping
    @Operation(summary = "Get my order history")
    public ResponseEntity<ApiResponse<PageResponse<OrderSummaryResponse>>> getMyOrders(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getMyOrders(principal.getId(), page, size)));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order detail")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getMyOrder(principal.getId(), orderId)));
    }

    @GetMapping("/number/{orderNumber}")
    @Operation(summary = "Track order by order number")
    public ResponseEntity<ApiResponse<OrderResponse>> getByNumber(
            @PathVariable String orderNumber) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getOrderByNumber(orderNumber)));
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel an order")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long orderId,
            @RequestParam(defaultValue = "Cancelled by customer") String reason) {
        orderService.cancelOrder(principal.getId(), orderId, reason);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully"));
    }

    @PostMapping("/{orderId}/return")
    @Operation(summary = "Request order return")
    public ResponseEntity<ApiResponse<Void>> requestReturn(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long orderId,
            @RequestParam(defaultValue = "Return requested") String reason) {
        orderService.requestReturn(principal.getId(), orderId, reason);
        return ResponseEntity.ok(ApiResponse.success("Return request submitted"));
    }
}