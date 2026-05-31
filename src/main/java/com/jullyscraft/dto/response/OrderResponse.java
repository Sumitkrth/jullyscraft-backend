package com.jullyscraft.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jullyscraft.entity.Order;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderResponse {

    private Long                             id;
    private String                           orderNumber;
    private Order.OrderStatus                status;
    private Order.PaymentMethod              paymentMethod;
    private Order.PaymentStatus              paymentStatus;

    // Pricing
    private BigDecimal                       subtotal;
    private BigDecimal                       shippingFee;
    private BigDecimal                       tax;
    private BigDecimal                       discount;
    private BigDecimal                       totalAmount;
    private String                           couponCode;

    // Shipping
    private String                           shippingFullName;
    private String                           shippingPhone;
    private String                           shippingAddressLine1;
    private String                           shippingAddressLine2;
    private String                           shippingCity;
    private String                           shippingState;
    private String                           shippingPincode;
    private String                           shippingCountry;

    // Tracking
    private String                           trackingId;
    private String                           courierName;
    private LocalDateTime                    estimatedDelivery;
    private LocalDateTime                    deliveredAt;
    private LocalDateTime                    paidAt;

    // Meta
    private String                           customerNote;
    private String                           guestEmail;
    private Long                             userId;
    private boolean                          cancellable;
    private boolean                          returnable;
    private LocalDateTime                    createdAt;

    private List<OrderItemResponse>          items;
    private List<OrderStatusHistoryResponse> statusHistory;
}