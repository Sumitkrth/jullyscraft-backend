package com.jullyscraft.entity;

import com.jullyscraft.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_user",   columnList = "user_id"),
        @Index(name = "idx_order_number", columnList = "order_number", unique = true),
        @Index(name = "idx_order_status", columnList = "status")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Order extends BaseEntity {

    // ── Identity ──────────────────────────────────────────────────────────────
    @Column(nullable = false, unique = true, length = 30)
    private String orderNumber;          // JC-20240521-000001

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")        // null = guest checkout
    private User user;

    // ── Status ────────────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    // ── Pricing ───────────────────────────────────────────────────────────────
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal tax = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    // ── Coupon ────────────────────────────────────────────────────────────────
    @Column(length = 50)
    private String couponCode;

    // ── Shipping address snapshot ─────────────────────────────────────────────
    @Column(nullable = false, length = 100)
    private String shippingFullName;

    @Column(nullable = false, length = 15)
    private String shippingPhone;

    @Column(nullable = false, length = 255)
    private String shippingAddressLine1;

    @Column(length = 255)
    private String shippingAddressLine2;

    @Column(nullable = false, length = 100)
    private String shippingCity;

    @Column(nullable = false, length = 100)
    private String shippingState;

    @Column(nullable = false, length = 10)
    private String shippingPincode;

    @Column(nullable = false, length = 100)
    private String shippingCountry;

    // ── Payment ───────────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.COD;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(length = 200)
    private String paymentId;           // Razorpay / Stripe ID

    @Column
    private LocalDateTime paidAt;

    // ── Shipping / tracking ───────────────────────────────────────────────────
    @Column(length = 100)
    private String trackingId;

    @Column(length = 100)
    private String courierName;

    @Column
    private LocalDateTime estimatedDelivery;

    @Column
    private LocalDateTime deliveredAt;

    // ── Notes ─────────────────────────────────────────────────────────────────
    @Column(length = 500)
    private String customerNote;

    @Column(length = 500)
    private String adminNote;

    // ── Guest ─────────────────────────────────────────────────────────────────
    @Column(length = 150)
    private String guestEmail;

    // ── Relations ─────────────────────────────────────────────────────────────
    @OneToMany(mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderStatusHistory> statusHistory = new ArrayList<>();

    // ── Enums ─────────────────────────────────────────────────────────────────
    public enum OrderStatus {
        PENDING, PAID, PROCESSING, PACKED,
        SHIPPED, OUT_FOR_DELIVERY, DELIVERED,
        CANCELLED, RETURNED, REFUNDED
    }

    public enum PaymentMethod {
        COD, RAZORPAY, STRIPE, UPI, WALLET, NET_BANKING
    }

    public enum PaymentStatus {
        PENDING, PAID, FAILED, REFUNDED, PARTIALLY_REFUNDED
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }

    public void addStatusHistory(OrderStatusHistory history) {
        history.setOrder(this);
        statusHistory.add(history);
    }

    public boolean isCancellable() {
        return status == OrderStatus.PENDING
                || status == OrderStatus.PAID
                || status == OrderStatus.PROCESSING;
    }

    public boolean isReturnable() {
        return status == OrderStatus.DELIVERED;
    }
}