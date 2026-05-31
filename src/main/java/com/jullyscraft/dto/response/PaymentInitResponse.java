package com.jullyscraft.dto.response;

import com.jullyscraft.entity.Payment;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PaymentInitResponse {
    private Long                   paymentId;       // internal Payment entity id
    private String                 gatewayOrderId;  // Razorpay order_id / Stripe client_secret
    private String                 keyId;           // Razorpay public key (safe to expose)
    private BigDecimal             amount;
    private String                 currency;
    private Payment.PaymentGateway gateway;
    private String                 orderNumber;
}