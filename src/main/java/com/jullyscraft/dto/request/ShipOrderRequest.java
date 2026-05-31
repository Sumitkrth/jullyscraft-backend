package com.jullyscraft.dto.request;

import com.jullyscraft.entity.Shipment;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ShipOrderRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    // null = use default provider from config
    private Shipment.ShippingProvider provider;

    private Integer weightGrams;     // override default if needed
    private String  courierCode;     // preferred courier (provider-specific)
    private String  pickupLocation;  // warehouse/pickup name
}