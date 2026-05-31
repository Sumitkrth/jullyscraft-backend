package com.jullyscraft.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jullyscraft.entity.Shipment;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShipmentResponse {
    private Long                      id;
    private Long                      orderId;
    private String                    orderNumber;
    private Shipment.ShippingProvider provider;
    private Shipment.ShipmentStatus   status;
    private String                    awbNumber;
    private String                    courierName;
    private String                    trackingUrl;
    private String                    labelUrl;
    private BigDecimal                shippingCharge;
    private int                       weightGrams;
    private LocalDateTime             estimatedDelivery;
    private LocalDateTime             shippedAt;
    private LocalDateTime             deliveredAt;
    private LocalDateTime             createdAt;
}