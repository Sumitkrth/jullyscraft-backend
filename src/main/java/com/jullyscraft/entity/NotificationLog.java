package com.jullyscraft.entity;

import com.jullyscraft.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "notification_logs", indexes = {
        @Index(name = "idx_notif_user",    columnList = "user_id"),
        @Index(name = "idx_notif_type",    columnList = "type"),
        @Index(name = "idx_notif_channel", columnList = "channel"),
        @Index(name = "idx_notif_status",  columnList = "status")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class NotificationLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String recipient;         // email / phone / fcm token

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DeliveryStatus status = DeliveryStatus.PENDING;

    @Column(length = 500)
    private String errorMessage;

    @Column(length = 200)
    private String externalId;        // provider message ID

    @Column(nullable = false)
    @Builder.Default
    private int retryCount = 0;

    // ── Enums ─────────────────────────────────────────────────────────────────
    public enum NotificationChannel { EMAIL, SMS, WHATSAPP, PUSH }

    public enum NotificationType {
        WELCOME, EMAIL_VERIFICATION, PASSWORD_RESET,
        ORDER_PLACED, ORDER_PAID, ORDER_PROCESSING,
        ORDER_PACKED, ORDER_SHIPPED, ORDER_OUT_FOR_DELIVERY,
        ORDER_DELIVERED, ORDER_CANCELLED, ORDER_RETURNED,
        PAYMENT_SUCCESS, PAYMENT_FAILED, REFUND_INITIATED,
        REVIEW_APPROVED, REVIEW_REJECTED,
        WISHLIST_PRICE_DROP, LOW_STOCK_ALERT,
        PROMOTIONAL, OTP
    }

    public enum DeliveryStatus { PENDING, SENT, DELIVERED, FAILED }
}