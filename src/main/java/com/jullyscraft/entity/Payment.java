package com.jullyscraft.entity;

import com.jullyscraft.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_order",      columnList = "order_id"),
        @Index(name = "idx_payment_gateway_id", columnList = "gateway_payment_id"),
        @Index(name = "idx_payment_status",     columnList = "status")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentGateway gateway;

    // Gateway-specific IDs
    @Column(length = 200)
    private String gatewayOrderId;       // Razorpay order_id / Stripe PaymentIntent id

    @Column(length = 200)
    private String gatewayPaymentId;     // Razorpay payment_id / Stripe charge id

    @Column(length = 500)
    private String gatewaySignature;     // Razorpay signature for verification

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.INITIATED;

    @Column(length = 500)
    private String failureReason;

    @Column
    private LocalDateTime paidAt;

    // Refund fields
    @Column(length = 200)
    private String refundId;

    @Column(precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RefundStatus refundStatus;

    @Column
    private LocalDateTime refundedAt;

    // Raw webhook payload (for audit)
    @Column(columnDefinition = "TEXT")
    private String webhookPayload;

    // ── Enums ─────────────────────────────────────────────────────────────────
    public enum PaymentGateway { RAZORPAY, STRIPE, COD, WALLET }

    public enum PaymentStatus {
        INITIATED, PENDING, CAPTURED, FAILED, REFUNDED, PARTIALLY_REFUNDED
    }

    public enum RefundStatus {
        REQUESTED, PROCESSING, SUCCESS, FAILED
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    public boolean isCaptured() {
        return status == PaymentStatus.CAPTURED;
    }

    public boolean isRefundable(int refundWindowDays) {
        if (!isCaptured() || paidAt == null) return false;
        return paidAt.plusDays(refundWindowDays).isAfter(LocalDateTime.now());
    }
}