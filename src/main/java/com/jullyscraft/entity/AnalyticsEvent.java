package com.jullyscraft.entity;

import com.jullyscraft.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Entity
@Table(name = "analytics_events", indexes = {
        @Index(name = "idx_event_type",    columnList = "event_type"),
        @Index(name = "idx_event_user",    columnList = "user_id"),
        @Index(name = "idx_event_session", columnList = "session_id"),
        @Index(name = "idx_event_created", columnList = "created_at")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AnalyticsEvent extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EventType eventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 100)
    private String sessionId;

    // Reference IDs — null if not applicable
    private Long productId;
    private Long orderId;
    private Long categoryId;

    @Column(length = 500)
    private String pageUrl;

    @Column(length = 50)
    private String deviceType;    // mobile | desktop | tablet

    @Column(length = 100)
    private String referrer;

    @Column(columnDefinition = "TEXT")
    private String metadata;      // JSON string — flexible extra data

    public enum EventType {
        PAGE_VIEW, PRODUCT_VIEW, SEARCH,
        ADD_TO_CART, REMOVE_FROM_CART,
        CHECKOUT_START, CHECKOUT_COMPLETE,
        ORDER_PLACED, ORDER_CANCELLED,
        SIGNUP, LOGIN,
        WISHLIST_ADD, WISHLIST_REMOVE,
        COUPON_APPLIED, COUPON_FAILED,
        PAYMENT_START, PAYMENT_SUCCESS, PAYMENT_FAILED
    }
}