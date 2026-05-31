package com.jullyscraft.service;

import com.jullyscraft.dto.response.PaymentInitResponse;
import com.jullyscraft.entity.Order;
import com.jullyscraft.entity.Payment;

public interface RazorpayService {
    PaymentInitResponse createOrder(Order order, Payment payment);
    boolean             verifySignature(String gatewayOrderId,
                                        String gatewayPaymentId,
                                        String signature);
    String              processRefund(String gatewayPaymentId,
                                      long   amountPaise,
                                      String reason);
}