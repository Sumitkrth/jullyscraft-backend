package com.jullyscraft.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jullyscraft.entity.Payment;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponse {
    private Long                    id;
    private Long                    orderId;
    private Payment.PaymentGateway  gateway;
    private String                  gatewayOrderId;
    private String                  gatewayPaymentId;
    private BigDecimal              amount;
    private String                  currency;
    private Payment.PaymentStatus   status;
    private String                  failureReason;
    private LocalDateTime           paidAt;
    private String                  refundId;
    private BigDecimal              refundAmount;
    private Payment.RefundStatus    refundStatus;
    private LocalDateTime           refundedAt;
    private LocalDateTime           createdAt;
}