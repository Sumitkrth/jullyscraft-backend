package com.jullyscraft.service;

import com.jullyscraft.dto.request.InitiatePaymentRequest;
import com.jullyscraft.dto.request.RefundRequest;
import com.jullyscraft.dto.request.VerifyPaymentRequest;
import com.jullyscraft.dto.response.PaymentInitResponse;
import com.jullyscraft.dto.response.PaymentResponse;
import com.jullyscraft.dto.response.RefundResponse;

import java.util.List;

public interface PaymentService {
    PaymentInitResponse initiatePayment(Long userId, InitiatePaymentRequest request);
    PaymentResponse     verifyPayment(Long userId,   VerifyPaymentRequest request);
    RefundResponse      processRefund(Long userId,   RefundRequest request);
    List<PaymentResponse> getPaymentsForOrder(Long userId, Long orderId);

    // Called by webhook handlers
    void handleRazorpaySuccess(String gatewayOrderId, String gatewayPaymentId,
                               String signature,     String rawPayload);
    void handleStripeSuccess(String paymentIntentId, String rawPayload);
    void handleRefundWebhook(String refundId, String gatewayPaymentId,
                             String status, String rawPayload);
}