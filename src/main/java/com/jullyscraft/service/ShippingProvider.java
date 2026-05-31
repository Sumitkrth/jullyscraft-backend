package com.jullyscraft.service;

import com.jullyscraft.dto.request.ShipOrderRequest;
import com.jullyscraft.dto.request.ShippingRateRequest;
import com.jullyscraft.dto.response.ShippingRateResponse;
import com.jullyscraft.dto.response.TrackingResponse;
import com.jullyscraft.entity.Order;
import com.jullyscraft.entity.Shipment;

/**
 * Platform-agnostic shipping provider strategy.
 * Implement this interface to add any courier — Shiprocket, Delhivery,
 * India Post, BlueDart, FedEx, DHL, or a custom integration.
 */
public interface ShippingProvider {

    /** Unique identifier for this provider */
    Shipment.ShippingProvider getProviderType();

    /** Check if provider is enabled and configured */
    boolean isEnabled();

    /** Get available courier options + rates for a route */
    ShippingRateResponse getRates(ShippingRateRequest request);

    /**
     * Book a shipment with the provider.
     * Populates: awbNumber, courierName, trackingUrl, labelUrl,
     *            providerShipmentId, estimatedDelivery
     */
    Shipment book(Order order, ShipOrderRequest request);

    /** Get live tracking events for an AWB */
    TrackingResponse track(String awbNumber);

    /** Cancel a booked shipment */
    boolean cancel(String awbNumber);

    /** Generate / re-fetch shipping label PDF URL */
    String generateLabel(String providerShipmentId);
}