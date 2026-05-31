package com.jullyscraft.dto.request;

import com.jullyscraft.entity.Payment;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class InitiatePaymentRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotNull(message = "Gateway is required")
    private Payment.PaymentGateway gateway;
}