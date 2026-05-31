package com.jullyscraft.dto.request;

import com.jullyscraft.entity.Order;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class OrderStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private Order.OrderStatus status;

    private String note;
    private String trackingId;
    private String courierName;
}