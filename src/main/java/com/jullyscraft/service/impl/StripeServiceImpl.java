package com.jullyscraft.service.impl;

import com.jullyscraft.config.PaymentConfig;
import com.jullyscraft.dto.response.PaymentInitResponse;
import com.jullyscraft.entity.Order;
import com.jullyscraft.entity.Payment;
import com.jullyscraft.exception.BadRequestException;
import com.jullyscraft.service.StripeService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeServiceImpl implements StripeService {

    private final PaymentConfig paymentConfig;

    @Override
    public PaymentInitResponse createPaymentIntent(Order order, Payment payment) {
        try {
            // Stripe expects amount in smallest unit (paise for INR)
            long amountPaise = order.getTotalAmount()
                    .multiply(java.math.BigDecimal.valueOf(100))
                    .longValue();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountPaise)
                    .setCurrency(paymentConfig.getStripeCurrency())
                    .putMetadata("orderId",     order.getId().toString())
                    .putMetadata("orderNumber", order.getOrderNumber())
                    .setDescription("Order: " + order.getOrderNumber())
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);
            payment.setGatewayOrderId(intent.getId());

            log.info("Stripe PaymentIntent created: {} for order: {}",
                    intent.getId(), order.getOrderNumber());

            return PaymentInitResponse.builder()
                    .paymentId(payment.getId())
                    .gatewayOrderId(intent.getClientSecret()) // client_secret for frontend
                    .amount(order.getTotalAmount())
                    .currency(paymentConfig.getStripeCurrency())
                    .gateway(Payment.PaymentGateway.STRIPE)
                    .orderNumber(order.getOrderNumber())
                    .build();

        } catch (StripeException e) {
            log.error("Stripe PaymentIntent creation failed: {}", e.getMessage());
            throw new BadRequestException("Payment initiation failed: " + e.getMessage());
        }
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String sigHeader) {
        try {
            Webhook.constructEvent(payload, sigHeader,
                    paymentConfig.getStripeWebhookSecret());
            return true;
        } catch (SignatureVerificationException e) {
            log.warn("Stripe webhook signature invalid: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String processRefund(String gatewayPaymentId, long amountCents, String reason) {
        try {
            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(gatewayPaymentId)
                    .setAmount(amountCents)
                    .putMetadata("reason", reason)
                    .build();

            Refund refund = Refund.create(params);
            log.info("Stripe refund initiated: {} for payment: {}",
                    refund.getId(), gatewayPaymentId);
            return refund.getId();

        } catch (StripeException e) {
            log.error("Stripe refund failed: {}", e.getMessage());
            throw new BadRequestException("Refund failed: " + e.getMessage());
        }
    }
}