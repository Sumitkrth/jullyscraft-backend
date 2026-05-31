package com.jullyscraft.service;

import com.jullyscraft.dto.NotificationEvent;

public interface EmailService {
    void send(NotificationEvent event);
    void sendWelcome(String email, String firstName);
    void sendEmailVerification(String email, String firstName, String token);
    void sendPasswordReset(String email, String firstName, String token);
    void sendOrderPlaced(String email, String firstName,
                         String orderNumber, String totalAmount,
                         String paymentMethod, String shippingAddress);
    void sendOrderShipped(String email, String firstName,
                          String orderNumber, String courierName,
                          String trackingId, String estimatedDelivery);
    void sendOrderDelivered(String email, String firstName, String orderNumber);
    void sendOrderCancelled(String email, String firstName,
                            String orderNumber, String reason);
    void sendPaymentSuccess(String email, String firstName,
                            String orderNumber, String amount);
    void sendRefundInitiated(String email, String firstName,
                             String orderNumber, String amount);
    void sendOtp(String email, String firstName, String otp);
}