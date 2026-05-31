package com.jullyscraft.service;

import com.jullyscraft.dto.response.PaymentInitResponse;
import com.jullyscraft.entity.Order;
import com.jullyscraft.entity.Payment;

public interface StripeService {
    PaymentInitResponse createPaymentIntent(Order order, Payment payment);
    boolean             verifyWebhookSignature(String payload, String sigHeader);
    String              processRefund(String gatewayPaymentId,
                                      long   amountCents,
                                      String reason);
}