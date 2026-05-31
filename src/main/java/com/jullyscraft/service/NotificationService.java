package com.jullyscraft.service;

import com.jullyscraft.entity.Order;
import com.jullyscraft.entity.User;

public interface NotificationService {

    // ── Auth ──────────────────────────────────────────────────────────────────
    void notifyWelcome(User user);
    void notifyEmailVerification(User user, String token);
    void notifyPasswordReset(User user, String token);
    void notifyOtp(User user, String otp);

    // ── Order ─────────────────────────────────────────────────────────────────
    void notifyOrderPlaced(Order order);
    void notifyOrderStatusChange(Order order);
    void notifyOrderShipped(Order order);
    void notifyOrderDelivered(Order order);
    void notifyOrderCancelled(Order order, String reason);

    // ── Payment ───────────────────────────────────────────────────────────────
    void notifyPaymentSuccess(Order order);
    void notifyRefundInitiated(Order order, String amount);
}