package com.jullyscraft.service.impl;

import com.jullyscraft.config.ShippingConfig;
import com.jullyscraft.dto.request.ShipOrderRequest;
import com.jullyscraft.dto.request.ShippingRateRequest;
import com.jullyscraft.dto.response.*;
import com.jullyscraft.entity.Order;
import com.jullyscraft.entity.Shipment;
import com.jullyscraft.exception.BadRequestException;
import com.jullyscraft.exception.ResourceNotFoundException;
import com.jullyscraft.mapper.ShipmentMapper;
import com.jullyscraft.repository.OrderRepository;
import com.jullyscraft.repository.ShipmentRepository;
import com.jullyscraft.service.NotificationService;
import com.jullyscraft.service.OrderService;
import com.jullyscraft.service.ShippingProvider;
import com.jullyscraft.service.ShippingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ShippingServiceImpl implements ShippingService {

    // All ShippingProvider beans auto-collected via Spring
    private final Map<Shipment.ShippingProvider, ShippingProvider> providers;

    private final ShipmentRepository shipmentRepository;
    private final OrderRepository    orderRepository;
    private final ShipmentMapper     shipmentMapper;
    private final ShippingConfig     config;
    private final NotificationService notificationService;
    private final OrderService        orderService;

    public ShippingServiceImpl(
            List<ShippingProvider> providerList,      // Spring injects all implementations
            ShipmentRepository shipmentRepository,
            OrderRepository orderRepository,
            ShipmentMapper shipmentMapper,
            ShippingConfig config,
            NotificationService notificationService,
            OrderService orderService) {

        // Build provider map for O(1) lookup
        this.providers = providerList.stream()
                .collect(Collectors.toMap(
                        ShippingProvider::getProviderType,
                        p -> p));

        this.shipmentRepository  = shipmentRepository;
        this.orderRepository     = orderRepository;
        this.shipmentMapper      = shipmentMapper;
        this.config              = config;
        this.notificationService = notificationService;
        this.orderService        = orderService;

        log.info("ShippingService initialized with providers: {}",
                this.providers.keySet());
    }

    // ── Rates ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ShippingRateResponse getRates(ShippingRateRequest request) {
        ShippingProvider provider = resolveProvider(null);
        return provider.getRates(request);
    }

    @Override
    @Transactional(readOnly = true)
    public ShippingRateResponse getAllProviderRates(ShippingRateRequest request) {
        List<ShippingRateResponse.CourierOption> allOptions = new ArrayList<>();

        // Query every enabled provider and merge options
        providers.values().stream()
                .filter(ShippingProvider::isEnabled)
                .forEach(p -> {
                    try {
                        ShippingRateResponse r = p.getRates(request);
                        allOptions.addAll(r.getOptions());
                    } catch (Exception e) {
                        log.warn("Provider {} rate fetch failed: {}",
                                p.getProviderType(), e.getMessage());
                    }
                });

        // Sort by rate ascending
        allOptions.sort(Comparator.comparing(
                ShippingRateResponse.CourierOption::getRate));

        BigDecimal best = allOptions.isEmpty()
                ? config.getDefaultShippingFee()
                : allOptions.get(0).getRate();

        return ShippingRateResponse.builder()
                .pickupPincode(request.getPickupPincode())
                .deliveryPincode(request.getDeliveryPincode())
                .options(allOptions)
                .recommendedRate(best)
                .build();
    }

    // ── Book shipment ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ShipmentResponse shipOrder(ShipOrderRequest request) {
        if (shipmentRepository.existsByOrderId(request.getOrderId())) {
            throw new BadRequestException(
                    "Shipment already exists for order: " + request.getOrderId());
        }

        Order order = orderRepository
                .findByIdWithDetails(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order", "id", request.getOrderId()));

        if (order.getStatus() != Order.OrderStatus.PROCESSING
                && order.getStatus() != Order.OrderStatus.PAID) {
            throw new BadRequestException(
                    "Order must be PAID or PROCESSING to ship. Current: "
                            + order.getStatus());
        }

        ShippingProvider provider = resolveProvider(request.getProvider());
        Shipment shipment = provider.book(order, request);

        Shipment saved = shipmentRepository.save(shipment);

        // Update order status to SHIPPED
        com.jullyscraft.dto.request.OrderStatusUpdateRequest statusReq =
                new com.jullyscraft.dto.request.OrderStatusUpdateRequest();
        statusReq.setStatus(Order.OrderStatus.SHIPPED);
        statusReq.setTrackingId(saved.getAwbNumber());
        statusReq.setCourierName(saved.getCourierName());
        statusReq.setNote("Shipped via " + saved.getCourierName());
        orderService.updateOrderStatus(order.getId(), statusReq);

        // Notify customer
        notificationService.notifyOrderShipped(order);

        log.info("Order {} shipped via {} AWB: {}",
                order.getOrderNumber(),
                saved.getProvider(),
                saved.getAwbNumber());

        return shipmentMapper.toResponse(saved);
    }

    // ── Tracking ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public TrackingResponse trackByAwb(String awbNumber) {
        Shipment shipment = shipmentRepository
                .findByAwbNumber(awbNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Shipment", "awbNumber", awbNumber));

        ShippingProvider provider = resolveProvider(shipment.getProvider());
        return provider.track(awbNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public TrackingResponse trackByOrderId(Long orderId) {
        Shipment shipment = findByOrderId(orderId);
        ShippingProvider provider = resolveProvider(shipment.getProvider());
        TrackingResponse tracking = provider.track(shipment.getAwbNumber());

        // Sync status back
        syncShipmentStatus(shipment, tracking.getStatus());

        return tracking;
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentResponse getShipmentByOrderId(Long orderId) {
        return shipmentMapper.toResponse(findByOrderId(orderId));
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public boolean cancelShipment(Long orderId) {
        Shipment shipment = findByOrderId(orderId);

        if (shipment.getStatus() == Shipment.ShipmentStatus.DELIVERED) {
            throw new BadRequestException(
                    "Cannot cancel a delivered shipment");
        }

        ShippingProvider provider = resolveProvider(shipment.getProvider());
        boolean cancelled = provider.cancel(shipment.getAwbNumber());

        if (cancelled) {
            shipment.setStatus(Shipment.ShipmentStatus.CANCELLED);
            shipmentRepository.save(shipment);
            log.info("Shipment cancelled for order {}", orderId);
        }

        return cancelled;
    }

    // ── Label ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ShipmentResponse generateLabel(Long orderId) {
        Shipment shipment = findByOrderId(orderId);
        ShippingProvider provider = resolveProvider(shipment.getProvider());

        String labelUrl = provider.generateLabel(shipment.getProviderShipmentId());
        shipment.setLabelUrl(labelUrl);
        return shipmentMapper.toResponse(shipmentRepository.save(shipment));
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ShipmentResponse> getAllShipments(int page, int size) {
        return PageResponse.of(
                shipmentRepository.findAll(
                                PageRequest.of(page, size,
                                        Sort.by("createdAt").descending()))
                        .map(shipmentMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ShipmentResponse> getShipmentsByStatus(
            Shipment.ShipmentStatus status, int page, int size) {
        return PageResponse.of(
                shipmentRepository.findByStatus(status,
                                PageRequest.of(page, size,
                                        Sort.by("createdAt").descending()))
                        .map(shipmentMapper::toResponse));
    }

    @Override
    @Transactional
    public ShipmentResponse updateTrackingManually(Long orderId,
                                                   String awbNumber,
                                                   String courierName,
                                                   String trackingUrl) {
        Shipment shipment = findByOrderId(orderId);
        shipment.setAwbNumber(awbNumber);
        shipment.setCourierName(courierName);
        shipment.setTrackingUrl(trackingUrl);
        shipment.setStatus(Shipment.ShipmentStatus.BOOKED);
        return shipmentMapper.toResponse(shipmentRepository.save(shipment));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private ShippingProvider resolveProvider(Shipment.ShippingProvider type) {
        // 1. Use explicitly requested provider
        if (type != null) {
            ShippingProvider p = providers.get(type);
            if (p != null && p.isEnabled()) return p;
            throw new BadRequestException(
                    "Provider not available: " + type);
        }

        // 2. Use configured default
        try {
            Shipment.ShippingProvider defaultType =
                    Shipment.ShippingProvider.valueOf(
                            config.getDefaultProvider());
            ShippingProvider p = providers.get(defaultType);
            if (p != null && p.isEnabled()) return p;
        } catch (Exception ignored) {}

        // 3. Use first enabled provider
        return providers.values().stream()
                .filter(ShippingProvider::isEnabled)
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "No shipping provider is configured and enabled"));
    }

    private Shipment findByOrderId(Long orderId) {
        return shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Shipment", "orderId", orderId));
    }

    @Transactional
    public void syncShipmentStatus(Shipment shipment,
                                   Shipment.ShipmentStatus newStatus) {
        if (shipment.getStatus() != newStatus) {
            shipment.setStatus(newStatus);
            shipmentRepository.save(shipment);
        }
    }
}