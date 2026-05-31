package com.jullyscraft.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class RefundRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    // null = full refund
    @DecimalMin(value = "1.0", message = "Refund amount must be at least 1")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal amount;

    @Size(max = 500)
    private String reason;
}