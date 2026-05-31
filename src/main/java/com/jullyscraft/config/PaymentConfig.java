package com.jullyscraft.config;

import com.razorpay.RazorpayClient;
import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@Getter
public class PaymentConfig {

    @Value("${app.payment.razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${app.payment.razorpay.key-secret}")
    private String razorpayKeySecret;

    @Value("${app.payment.razorpay.webhook-secret}")
    private String razorpayWebhookSecret;

    @Value("${app.payment.razorpay.currency:INR}")
    private String razorpayCurrency;

    @Value("${app.payment.stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${app.payment.stripe.webhook-secret}")
    private String stripeWebhookSecret;

    @Value("${app.payment.stripe.currency:inr}")
    private String stripeCurrency;

    @Value("${app.payment.refund-window-days:7}")
    private int refundWindowDays;

    @Bean
    public RazorpayClient razorpayClient() throws Exception {
        log.info("Initializing Razorpay client");
        return new RazorpayClient(razorpayKeyId, razorpayKeySecret);
    }

    @PostConstruct
    public void initStripe() {
        Stripe.apiKey = stripeSecretKey;
        log.info("Stripe SDK initialized");
    }
}