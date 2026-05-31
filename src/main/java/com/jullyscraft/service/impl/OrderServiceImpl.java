package com.jullyscraft.service.impl;

import com.jullyscraft.dto.request.OrderStatusUpdateRequest;
import com.jullyscraft.dto.request.PlaceOrderRequest;
import com.jullyscraft.dto.response.OrderResponse;
import com.jullyscraft.dto.response.OrderSummaryResponse;
import com.jullyscraft.dto.response.PageResponse;
import com.jullyscraft.entity.*;
import com.jullyscraft.exception.BadRequestException;
import com.jullyscraft.exception.ResourceNotFoundException;
import com.jullyscraft.mapper.OrderMapper;
import com.jullyscraft.repository.*;
import com.jullyscraft.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository       orderRepository;
    private final OrderItemRepository   orderItemRepository;
    private final CartRepository        cartRepository;
    private final CartItemRepository    cartItemRepository;
    private final AddressRepository     addressRepository;
    private final ProductRepository     productRepository;
    private final UserRepository        userRepository;
    private final OrderMapper           orderMapper;
    private final CartService           cartService;
    private final NotificationService   notificationService;
    private final RecommendationService recommendationService;
    private final AnalyticsService      analyticsService;

    // ── Valid status transitions ───────────────────────────────────────────────
    private static final Map<Order.OrderStatus, List<Order.OrderStatus>> TRANSITIONS = Map.of(
            Order.OrderStatus.PENDING,          List.of(Order.OrderStatus.PAID,
                    Order.OrderStatus.CANCELLED),
            Order.OrderStatus.PAID,             List.of(Order.OrderStatus.PROCESSING,
                    Order.OrderStatus.CANCELLED),
            Order.OrderStatus.PROCESSING,       List.of(Order.OrderStatus.PACKED,
                    Order.OrderStatus.CANCELLED),
            Order.OrderStatus.PACKED,           List.of(Order.OrderStatus.SHIPPED),
            Order.OrderStatus.SHIPPED,          List.of(Order.OrderStatus.OUT_FOR_DELIVERY),
            Order.OrderStatus.OUT_FOR_DELIVERY, List.of(Order.OrderStatus.DELIVERED),
            Order.OrderStatus.DELIVERED,        List.of(Order.OrderStatus.RETURNED),
            Order.OrderStatus.RETURNED,         List.of(Order.OrderStatus.REFUNDED),
            Order.OrderStatus.CANCELLED,        List.of(),
            Order.OrderStatus.REFUNDED,         List.of()
    );

    // ── Place order ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public OrderResponse placeOrder(Long userId, PlaceOrderRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new BadRequestException("Cart is empty"));

        List<CartItem> activeItems = cart.getItems().stream()
                .filter(i -> !i.isDeleted()).toList();

        if (activeItems.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        Order order = buildOrder(req, activeItems);
        order.setUser(user);
        order.setGuestEmail(null);

        Order saved = orderRepository.save(order);
        analyticsService.track(userId, null,
                AnalyticsEvent.EventType.ORDER_PLACED,
                null, saved.getId(), null, null);
        notificationService.notifyOrderPlaced(saved);
        reserveStock(activeItems);
        cartService.clearCart(userId);

        saved.getItems().forEach(item ->
                recommendationService.recordInteraction(
                        userId,
                        item.getProduct().getId(),
                        UserProductInteraction.InteractionType.PURCHASE,
                        null));

        log.info("Order placed: {} for user: {}", saved.getOrderNumber(), userId);
        return orderMapper.toResponse(
                orderRepository.findByIdWithDetails(saved.getId()).orElse(saved));
    }

    @Override
    @Transactional
    public OrderResponse placeGuestOrder(PlaceOrderRequest req) {
        if (req.getGuestEmail() == null || req.getGuestEmail().isBlank()) {
            throw new BadRequestException("Guest email is required for guest checkout");
        }
        // Guest order has no cart — items come from request
        // TODO: extend PlaceOrderRequest with inline items for guest checkout
        throw new BadRequestException("Guest checkout via direct item list — coming soon");
    }

    // ── User queries ──────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> getMyOrders(Long userId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PageResponse.of(
                orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                        .map(orderMapper::toSummary));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getMyOrder(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        return orderMapper.toResponse(
                orderRepository.findByIdWithDetails(order.getId()).orElse(order));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order", "orderNumber", orderNumber));
        return orderMapper.toResponse(
                orderRepository.findByIdWithDetails(order.getId()).orElse(order));
    }

    @Override
    @Transactional
    public void cancelOrder(Long userId, Long orderId, String reason) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.isCancellable()) {
            throw new BadRequestException(
                    "Order cannot be cancelled in status: " + order.getStatus());
        }

        transitionStatus(order, Order.OrderStatus.CANCELLED,
                "Cancelled by customer: " + reason);
        releaseStock(order.getItems());
        orderRepository.save(order);
        notificationService.notifyOrderCancelled(order, reason);
        log.info("Order cancelled: {}", order.getOrderNumber());
    }

    @Override
    @Transactional
    public void requestReturn(Long userId, Long orderId, String reason) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.isReturnable()) {
            throw new BadRequestException(
                    "Order cannot be returned in status: " + order.getStatus());
        }

        transitionStatus(order, Order.OrderStatus.RETURNED,
                "Return requested by customer: " + reason);
        orderRepository.save(order);
        log.info("Return requested for order: {}", order.getOrderNumber());
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> getAllOrders(
            Order.OrderStatus status, String from, String to, int page, int size) {

        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime fromDt = from != null ? LocalDateTime.parse(from, fmt) : null;
        LocalDateTime toDt   = to   != null ? LocalDateTime.parse(to,   fmt) : null;

        return PageResponse.of(
                orderRepository.findByFilters(status, fromDt, toDt, pageable)
                        .map(orderMapper::toSummary));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        return orderMapper.toResponse(
                orderRepository.findByIdWithDetails(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Order", "id", id)));
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatusUpdateRequest req) {
        Order order = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));

        transitionStatus(order, req.getStatus(), req.getNote());

        if (req.getTrackingId() != null) order.setTrackingId(req.getTrackingId());
        if (req.getCourierName() != null) order.setCourierName(req.getCourierName());

        if (req.getStatus() == Order.OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
        }

        Order saved = orderRepository.save(order);
        if (req.getStatus() == Order.OrderStatus.SHIPPED) {
            notificationService.notifyOrderShipped(saved);
        } else if (req.getStatus() == Order.OrderStatus.DELIVERED) {
            notificationService.notifyOrderDelivered(saved);
        } else {
            notificationService.notifyOrderStatusChange(saved);
        }
        log.info("Order {} status → {}", order.getOrderNumber(), req.getStatus());
        return orderMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void markAsPaid(Long orderId, String paymentId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
            throw new BadRequestException("Order is already marked as paid");
        }

        order.setPaymentStatus(Order.PaymentStatus.PAID);
        order.setPaymentId(paymentId);
        order.setPaidAt(LocalDateTime.now());
        transitionStatus(order, Order.OrderStatus.PAID, "Payment confirmed: " + paymentId);
        orderRepository.save(order);
        notificationService.notifyPaymentSuccess(order);
        log.info("Order {} marked as paid. paymentId: {}", order.getOrderNumber(), paymentId);
    }

    // ── Private: Build order ──────────────────────────────────────────────────

    private Order buildOrder(PlaceOrderRequest req, List<CartItem> cartItems) {

        // Resolve shipping address
        Address address = resolveShippingAddress(req);

        // Calculate totals
        BigDecimal subtotal = cartItems.stream()
                .map(CartItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal shippingFee  = calculateShipping(subtotal);
        BigDecimal tax          = calculateTax(subtotal);
        BigDecimal discount     = BigDecimal.ZERO; // TODO: wire coupon engine
        BigDecimal total        = subtotal.add(shippingFee).add(tax).subtract(discount);

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .status(Order.OrderStatus.PENDING)
                .paymentMethod(req.getPaymentMethod())
                .paymentStatus(req.getPaymentMethod() == Order.PaymentMethod.COD
                        ? Order.PaymentStatus.PENDING
                        : Order.PaymentStatus.PENDING)
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .tax(tax)
                .discount(discount)
                .totalAmount(total)
                .couponCode(req.getCouponCode())
                .shippingFullName(address.getFullName())
                .shippingPhone(address.getPhone())
                .shippingAddressLine1(address.getAddressLine1())
                .shippingAddressLine2(address.getAddressLine2())
                .shippingCity(address.getCity())
                .shippingState(address.getState())
                .shippingPincode(address.getPincode())
                .shippingCountry(address.getCountry())
                .customerNote(req.getCustomerNote())
                .build();

        // Add items
        for (CartItem ci : cartItems) {
            String variantInfo = buildVariantInfo(ci.getVariant());
            String imageUrl    = ci.getProduct().getImages().stream()
                    .filter(ProductImage::isPrimaryImage)
                    .map(ProductImage::getImageUrl)
                    .findFirst().orElse(null);

            OrderItem item = OrderItem.builder()
                    .product(ci.getProduct())
                    .productName(ci.getProduct().getName())
                    .productSku(ci.getVariant() != null
                            ? ci.getVariant().getSku()
                            : null)
                    .variantInfo(variantInfo)
                    .productImageUrl(imageUrl)
                    .quantity(ci.getQuantity())
                    .unitPrice(ci.getPriceSnapshot())
                    .lineTotal(ci.getLineTotal())
                    .build();
            order.addItem(item);
        }

        // Initial status history
        OrderStatusHistory history = OrderStatusHistory.builder()
                .fromStatus(Order.OrderStatus.PENDING)
                .toStatus(Order.OrderStatus.PENDING)
                .note("Order placed")
                .changedBy("system")
                .build();
        order.addStatusHistory(history);

        return order;
    }

    // ── Private: Helpers ──────────────────────────────────────────────────────

    private Address resolveShippingAddress(PlaceOrderRequest req) {
        if (req.getAddressId() != null) {
            return addressRepository.findById(req.getAddressId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Address", "id", req.getAddressId()));
        }
        // Build inline address from request fields
        validateInlineAddress(req);
        Address inline = new Address();
        inline.setFullName(req.getShippingFullName());
        inline.setPhone(req.getShippingPhone());
        inline.setAddressLine1(req.getShippingAddressLine1());
        inline.setAddressLine2(req.getShippingAddressLine2());
        inline.setCity(req.getShippingCity());
        inline.setState(req.getShippingState());
        inline.setPincode(req.getShippingPincode());
        inline.setCountry(req.getShippingCountry() != null
                ? req.getShippingCountry() : "India");
        return inline;
    }

    private void validateInlineAddress(PlaceOrderRequest req) {
        if (req.getShippingFullName()    == null
                || req.getShippingPhone()       == null
                || req.getShippingAddressLine1()== null
                || req.getShippingCity()        == null
                || req.getShippingState()       == null
                || req.getShippingPincode()     == null) {
            throw new BadRequestException(
                    "Shipping address is incomplete. Provide addressId or full address fields.");
        }
    }

    private void transitionStatus(Order order,
                                  Order.OrderStatus newStatus,
                                  String note) {
        List<Order.OrderStatus> allowed = TRANSITIONS.getOrDefault(
                order.getStatus(), List.of());

        if (!allowed.contains(newStatus)) {
            throw new BadRequestException(
                    "Invalid status transition: "
                            + order.getStatus() + " → " + newStatus
                            + ". Allowed: " + allowed);
        }

        String changedBy = resolveActor();
        OrderStatusHistory history = OrderStatusHistory.builder()
                .fromStatus(order.getStatus())
                .toStatus(newStatus)
                .note(note)
                .changedBy(changedBy)
                .build();
        order.addStatusHistory(history);
        order.setStatus(newStatus);
    }

    private void reserveStock(List<CartItem> items) {
        for (CartItem ci : items) {
            if (ci.getVariant() != null) {
                ci.getVariant().reserve(ci.getQuantity());
            } else {
                int updated = productRepository
                        .decrementStock(ci.getProduct().getId(), ci.getQuantity());
                if (updated == 0) {
                    throw new BadRequestException(
                            "Insufficient stock for: " + ci.getProduct().getName());
                }
            }
        }
    }

    private void releaseStock(List<OrderItem> items) {
        items.forEach(item ->
                productRepository.incrementStock(
                        item.getProduct().getId(), item.getQuantity()));
    }

    private BigDecimal calculateShipping(BigDecimal subtotal) {
        // Free shipping above ₹499
        return subtotal.compareTo(new BigDecimal("499")) >= 0
                ? BigDecimal.ZERO
                : new BigDecimal("49");
    }

    private BigDecimal calculateTax(BigDecimal subtotal) {
        // 18% GST — TODO: wire tax engine per category
        return subtotal.multiply(new BigDecimal("0.18"))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private String buildVariantInfo(ProductVariant variant) {
        if (variant == null) return null;
        StringBuilder sb = new StringBuilder();
        if (variant.getSize()     != null) sb.append("Size: ").append(variant.getSize()).append(", ");
        if (variant.getColor()    != null) sb.append("Color: ").append(variant.getColor()).append(", ");
        if (variant.getMaterial() != null) sb.append("Material: ").append(variant.getMaterial());
        return sb.toString().replaceAll(", $", "");
    }

    private static final AtomicLong SEQUENCE = new AtomicLong(0);
    private String generateOrderNumber() {
        return "JC-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-"
                + String.format("%06d", SEQUENCE.incrementAndGet());
    }

    private String resolveActor() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            return (auth != null && auth.isAuthenticated()) ? auth.getName() : "system";
        } catch (Exception e) {
            return "system";
        }
    }
}