package com.jullyscraft.entity;

import com.jullyscraft.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipments", indexes = {
        @Index(name = "idx_shipment_order",      columnList = "order_id"),
        @Index(name = "idx_shipment_awb",        columnList = "awb_number"),
        @Index(name = "idx_shipment_provider",   columnList = "provider"),
        @Index(name = "idx_shipment_status",     columnList = "status")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Shipment extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    // ── Provider info ─────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ShippingProvider provider;

    @Column(length = 100)
    private String providerShipmentId;     // Shiprocket shipment_id / Delhivery waybill

    @Column(length = 100)
    private String awbNumber;              // Airway Bill — universal tracking number

    @Column(length = 100)
    private String courierName;            // e.g. "Bluedart", "DTDC"

    @Column(length = 100)
    private String courierCode;            // internal code

    // ── Status ────────────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ShipmentStatus status = ShipmentStatus.PENDING;

    // ── Addresses snapshot ────────────────────────────────────────────────────
    @Column(nullable = false, length = 200)
    private String pickupLocation;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(nullable = false, length = 10)
    private String deliveryPincode;

    // ── Parcel details ────────────────────────────────────────────────────────
    @Column(nullable = false)
    @Builder.Default
    private int weightGrams = 500;

    @Column @Builder.Default private int length  = 15;
    @Column @Builder.Default private int breadth = 12;
    @Column @Builder.Default private int height  = 10;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal declaredValue;

    @Column(precision = 8, scale = 2)
    private BigDecimal shippingCharge;

    // ── Tracking ──────────────────────────────────────────────────────────────
    @Column(length = 500)
    private String trackingUrl;

    @Column
    private LocalDateTime estimatedDelivery;

    @Column
    private LocalDateTime shippedAt;

    @Column
    private LocalDateTime deliveredAt;

    @Column(columnDefinition = "TEXT")
    private String lastTrackingRaw;        // raw JSON from provider

    // ── Label ─────────────────────────────────────────────────────────────────
    @Column(length = 500)
    private String labelUrl;               // printable shipping label PDF

    // ── Enums ─────────────────────────────────────────────────────────────────
    public enum ShippingProvider {
        SHIPROCKET, DELHIVERY, BLUEDART,
        FEDEX, DHL, INDIA_POST, MANUAL
    }

    public enum ShipmentStatus {
        PENDING, BOOKED, PICKED_UP,
        IN_TRANSIT, OUT_FOR_DELIVERY,
        DELIVERED, FAILED, CANCELLED, RTO
    }
}