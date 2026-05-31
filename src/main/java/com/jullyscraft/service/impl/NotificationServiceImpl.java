package com.jullyscraft.service.impl;

import com.jullyscraft.entity.Order;
import com.jullyscraft.entity.User;
import com.jullyscraft.service.EmailService;
import com.jullyscraft.service.NotificationService;
import com.jullyscraft.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final EmailService emailService;
    private final SmsService   smsService;

    // ── Auth ──────────────────────────────────────────────────────────────────

    @Override
    public void notifyWelcome(User user) {
        emailService.sendWelcome(user.getEmail(), user.getFirstName());
    }

    @Override
    public void notifyEmailVerification(User user, String token) {
        emailService.sendEmailVerification(
                user.getEmail(), user.getFirstName(), token);
    }

    @Override
    public void notifyPasswordReset(User user, String token) {
        emailService.sendPasswordReset(
                user.getEmail(), user.getFirstName(), token);
    }

    @Override
    public void notifyOtp(User user, String otp) {
        emailService.sendOtp(user.getEmail(), user.getFirstName(), otp);
        if (user.getPhone() != null) {
            smsService.sendOtp(user.getPhone(), otp);
        }
    }

    // ── Order ─────────────────────────────────────────────────────────────────

    @Override
    public void notifyOrderPlaced(Order order) {
        if (order.getUser() == null) return;
        User user = order.getUser();

        emailService.sendOrderPlaced(
                user.getEmail(),
                user.getFirstName(),
                order.getOrderNumber(),
                "₹" + order.getTotalAmount(),
                order.getPaymentMethod().name(),
                buildShippingAddress(order));

        if (user.getPhone() != null) {
            smsService.sendOrderAlert(
                    user.getPhone(),
                    order.getOrderNumber(),
                    "Placed");
        }
    }

    @Override
    public void notifyOrderStatusChange(Order order) {
        if (order.getUser() == null) return;
        User user = order.getUser();

        if (user.getPhone() != null) {
            smsService.sendOrderAlert(
                    user.getPhone(),
                    order.getOrderNumber(),
                    order.getStatus().name().replace("_", " "));
        }
    }

    @Override
    public void notifyOrderShipped(Order order) {
        if (order.getUser() == null) return;
        User user = order.getUser();

        emailService.sendOrderShipped(
                user.getEmail(),
                user.getFirstName(),
                order.getOrderNumber(),
                order.getCourierName() != null ? order.getCourierName() : "Courier",
                order.getTrackingId()  != null ? order.getTrackingId()  : "N/A",
                order.getEstimatedDelivery() != null
                        ? order.getEstimatedDelivery().toLocalDate().toString()
                        : "Soon");

        if (user.getPhone() != null && order.getTrackingId() != null) {
            smsService.sendDeliveryUpdate(
                    user.getPhone(),
                    order.getOrderNumber(),
                    order.getTrackingId(),
                    order.getCourierName() != null ? order.getCourierName() : "Courier");
        }
    }

    @Override
    public void notifyOrderDelivered(Order order) {
        if (order.getUser() == null) return;
        User user = order.getUser();
        emailService.sendOrderDelivered(
                user.getEmail(), user.getFirstName(), order.getOrderNumber());
        if (user.getPhone() != null) {
            smsService.sendOrderAlert(
                    user.getPhone(), order.getOrderNumber(), "Delivered");
        }
    }

    @Override
    public void notifyOrderCancelled(Order order, String reason) {
        if (order.getUser() == null) return;
        User user = order.getUser();
        emailService.sendOrderCancelled(
                user.getEmail(), user.getFirstName(),
                order.getOrderNumber(), reason);
        if (user.getPhone() != null) {
            smsService.sendOrderAlert(
                    user.getPhone(), order.getOrderNumber(), "Cancelled");
        }
    }

    // ── Payment ───────────────────────────────────────────────────────────────

    @Override
    public void notifyPaymentSuccess(Order order) {
        if (order.getUser() == null) return;
        User user = order.getUser();
        emailService.sendPaymentSuccess(
                user.getEmail(),
                user.getFirstName(),
                order.getOrderNumber(),
                "₹" + order.getTotalAmount());
    }

    @Override
    public void notifyRefundInitiated(Order order, String amount) {
        if (order.getUser() == null) return;
        User user = order.getUser();
        emailService.sendRefundInitiated(
                user.getEmail(), user.getFirstName(),
                order.getOrderNumber(), amount);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String buildShippingAddress(Order order) {
        return order.getShippingAddressLine1()
                + ", " + order.getShippingCity()
                + ", " + order.getShippingState()
                + " - " + order.getShippingPincode();
    }
}