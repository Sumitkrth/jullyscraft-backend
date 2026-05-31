package com.jullyscraft.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class VerifyPaymentRequest {

    @NotNull
    private Long orderId;

    @NotBlank(message = "Gateway order ID is required")
    private String gatewayOrderId;

    @NotBlank(message = "Gateway payment ID is required")
    private String gatewayPaymentId;

    // Razorpay only
    private String gatewaySignature;
}