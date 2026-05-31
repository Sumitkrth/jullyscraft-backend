package com.jullyscraft.controller;

import com.jullyscraft.webhook.RazorpayWebhookHandler;
import com.jullyscraft.webhook.StripeWebhookHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final RazorpayWebhookHandler razorpayHandler;
    private final StripeWebhookHandler   stripeHandler;

    // Raw body needed for signature verification — must NOT use @RequestBody with any
    // body-consuming filter before this endpoint

    @PostMapping("/razorpay")
    public ResponseEntity<Void> razorpay(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {
        log.info("Razorpay webhook received");
        razorpayHandler.handle(payload, signature);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/stripe")
    public ResponseEntity<Void> stripe(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        log.info("Stripe webhook received");
        stripeHandler.handle(payload, sigHeader);
        return ResponseEntity.ok().build();
    }
}