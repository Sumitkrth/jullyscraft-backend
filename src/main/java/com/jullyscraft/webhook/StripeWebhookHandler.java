package com.jullyscraft.webhook;

import com.jullyscraft.config.PaymentConfig;
import com.jullyscraft.service.PaymentService;
import com.jullyscraft.service.StripeService;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StripeWebhookHandler {

    private final PaymentService paymentService;
    private final StripeService  stripeService;
    private final PaymentConfig  paymentConfig;

    public void handle(String payload, String sigHeader) {
        if (!stripeService.verifyWebhookSignature(payload, sigHeader)) {
            log.warn("Stripe webhook: invalid signature — ignoring");
            return;
        }

        try {
            Event event = Webhook.constructEvent(
                    payload, sigHeader,
                    paymentConfig.getStripeWebhookSecret());

            log.info("Stripe webhook event: {}", event.getType());

            switch (event.getType()) {
                case "payment_intent.succeeded" -> {
                    PaymentIntent intent = (PaymentIntent)
                            event.getDataObjectDeserializer()
                                    .getObject().orElseThrow();
                    paymentService.handleStripeSuccess(intent.getId(), payload);
                }
                case "payment_intent.payment_failed" ->
                        log.warn("Stripe payment_intent.payment_failed: {}", payload);

                case "charge.refunded" -> {
                    Refund refund = (Refund)
                            event.getDataObjectDeserializer()
                                    .getObject().orElseThrow();
                    paymentService.handleRefundWebhook(
                            refund.getId(),
                            refund.getPaymentIntent(),
                            refund.getStatus(),
                            payload);
                }
                default ->
                        log.debug("Stripe unhandled event: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Stripe webhook processing error: {}", e.getMessage());
        }
    }
}