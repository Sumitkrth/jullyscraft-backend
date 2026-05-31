package com.jullyscraft.service;

import com.jullyscraft.dto.request.OrderStatusUpdateRequest;
import com.jullyscraft.dto.request.PlaceOrderRequest;
import com.jullyscraft.dto.response.OrderResponse;
import com.jullyscraft.dto.response.OrderSummaryResponse;
import com.jullyscraft.dto.response.PageResponse;
import com.jullyscraft.entity.Order;

public interface OrderService {

    // ── User ──────────────────────────────────────────────────────────────────
    OrderResponse              placeOrder(Long userId, PlaceOrderRequest request);
    OrderResponse              placeGuestOrder(PlaceOrderRequest request);
    PageResponse<OrderSummaryResponse> getMyOrders(Long userId, int page, int size);
    OrderResponse              getMyOrder(Long userId, Long orderId);
    OrderResponse              getOrderByNumber(String orderNumber);
    void                       cancelOrder(Long userId, Long orderId, String reason);
    void                       requestReturn(Long userId, Long orderId, String reason);

    // ── Admin ─────────────────────────────────────────────────────────────────
    PageResponse<OrderSummaryResponse> getAllOrders(Order.OrderStatus status,
                                                    String from, String to,
                                                    int page, int size);
    OrderResponse              getOrderById(Long id);
    OrderResponse              updateOrderStatus(Long id, OrderStatusUpdateRequest request);
    void                       markAsPaid(Long orderId, String paymentId);
}