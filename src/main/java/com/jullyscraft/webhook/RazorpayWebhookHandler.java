package com.jullyscraft.webhook;

import com.jullyscraft.config.PaymentConfig;
import com.jullyscraft.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Slf4j
@Component
@RequiredArgsConstructor
public class RazorpayWebhookHandler {

    private final PaymentService paymentService;
    private final PaymentConfig  paymentConfig;

    public boolean verifyWebhookSignature(String payload, String receivedSignature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    paymentConfig.getRazorpayWebhookSecret()
                            .getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computed = HexFormat.of().formatHex(digest);
            return computed.equals(receivedSignature);
        } catch (Exception e) {
            log.error("Razorpay webhook signature error: {}", e.getMessage());
            return false;
        }
    }

    public void handle(String payload, String signature) {
        if (!verifyWebhookSignature(payload, signature)) {
            log.warn("Razorpay webhook: invalid signature — ignoring");
            return;
        }

        JSONObject event = new JSONObject(payload);
        String     eventType = event.getString("event");
        log.info("Razorpay webhook event: {}", eventType);

        switch (eventType) {
            case "payment.captured" -> {
                JSONObject paymentEntity = event
                        .getJSONObject("payload")
                        .getJSONObject("payment")
                        .getJSONObject("entity");

                paymentService.handleRazorpaySuccess(
                        paymentEntity.getString("order_id"),
                        paymentEntity.getString("id"),
                        paymentEntity.optString("signature", ""),
                        payload);
            }
            case "payment.failed" ->
                    log.warn("Razorpay payment.failed: {}", payload);

            case "refund.processed", "refund.failed" -> {
                JSONObject refundEntity = event
                        .getJSONObject("payload")
                        .getJSONObject("refund")
                        .getJSONObject("entity");

                paymentService.handleRefundWebhook(
                        refundEntity.getString("id"),
                        refundEntity.getString("payment_id"),
                        refundEntity.getString("status"),
                        payload);
            }
            default ->
                    log.debug("Razorpay unhandled event: {}", eventType);
        }
    }
}