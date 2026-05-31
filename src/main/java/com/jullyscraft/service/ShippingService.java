package com.jullyscraft.service;

import com.jullyscraft.dto.request.ShipOrderRequest;
import com.jullyscraft.dto.request.ShippingRateRequest;
import com.jullyscraft.dto.response.PageResponse;
import com.jullyscraft.dto.response.ShipmentResponse;
import com.jullyscraft.dto.response.ShippingRateResponse;
import com.jullyscraft.dto.response.TrackingResponse;
import com.jullyscraft.entity.Shipment;

public interface ShippingService {

    // ── Rates & availability ──────────────────────────────────────────────────
    ShippingRateResponse         getRates(ShippingRateRequest request);
    ShippingRateResponse         getAllProviderRates(ShippingRateRequest request);

    // ── Shipment lifecycle ────────────────────────────────────────────────────
    ShipmentResponse             shipOrder(ShipOrderRequest request);
    TrackingResponse             trackByAwb(String awbNumber);
    TrackingResponse             trackByOrderId(Long orderId);
    ShipmentResponse             getShipmentByOrderId(Long orderId);
    boolean                      cancelShipment(Long orderId);
    ShipmentResponse             generateLabel(Long orderId);

    // ── Admin ─────────────────────────────────────────────────────────────────
    PageResponse<ShipmentResponse> getAllShipments(int page, int size);
    PageResponse<ShipmentResponse> getShipmentsByStatus(
            Shipment.ShipmentStatus status, int page, int size);
    ShipmentResponse             updateTrackingManually(Long orderId,
                                                        String awbNumber,
                                                        String courierName,
                                                        String trackingUrl);
}