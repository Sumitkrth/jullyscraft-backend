package com.jullyscraft.dto.response;

import com.jullyscraft.entity.Order;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
public class OrderSummaryResponse {
    private Long                  id;
    private String                orderNumber;
    private Order.OrderStatus     status;
    private Order.PaymentStatus   paymentStatus;
    private Order.PaymentMethod   paymentMethod;
    private BigDecimal            totalAmount;
    private int                   itemCount;
    private String                shippingCity;
    private String                trackingId;
    private LocalDateTime         createdAt;
}