package com.jullyscraft.service.impl;

import com.jullyscraft.dto.NotificationEvent;
import com.jullyscraft.entity.NotificationLog;
import com.jullyscraft.repository.NotificationLogRepository;
import com.jullyscraft.service.EmailService;
import com.jullyscraft.template.EmailTemplateBuilder;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender          mailSender;
    private final EmailTemplateBuilder    templateBuilder;
    private final NotificationLogRepository logRepository;

    @Value("${app.notification.email.from}")
    private String from;

    @Value("${app.notification.email.from-name}")
    private String fromName;

    @Value("${app.notification.email.base-url}")
    private String baseUrl;

    // ── Generic send ──────────────────────────────────────────────────────────

    @Override
    @Async("notificationExecutor")
    public void send(NotificationEvent event) {
        NotificationLog notificationLog = buildLog(event);
        try {
            String html = templateBuilder.build(
                    event.getTemplateName(), event.getTemplateVars());
            sendHtml(event.getRecipientEmail(), event.getSubject(), html);
            notificationLog.setStatus(NotificationLog.DeliveryStatus.SENT);
        } catch (Exception e) {
            notificationLog.setStatus(NotificationLog.DeliveryStatus.FAILED);
            notificationLog.setErrorMessage(e.getMessage());
            log.error("Email send failed to {}: {}", event.getRecipientEmail(), e.getMessage());
        } finally {
            logRepository.save(notificationLog);
        }
    }

    // ── Typed convenience methods ─────────────────────────────────────────────

    @Override
    @Async("notificationExecutor")
    public void sendWelcome(String email, String firstName) {
        sendTemplated(email, "Welcome to Jully's Craft! 🎉", "welcome",
                Map.of("firstName", firstName,
                        "shopUrl",   baseUrl + "/products"),
                NotificationLog.NotificationType.WELCOME);
    }

    @Override
    @Async("notificationExecutor")
    public void sendEmailVerification(String email, String firstName, String token) {
        sendTemplated(email, "Verify your email — Jully's Craft",
                "email-verification",
                Map.of("firstName",  firstName,
                        "verifyUrl",  baseUrl + "/verify-email?token=" + token),
                NotificationLog.NotificationType.EMAIL_VERIFICATION);
    }

    @Override
    @Async("notificationExecutor")
    public void sendPasswordReset(String email, String firstName, String token) {
        sendTemplated(email, "Reset your password — Jully's Craft",
                "password-reset",
                Map.of("firstName", firstName,
                        "resetUrl",  baseUrl + "/reset-password?token=" + token),
                NotificationLog.NotificationType.PASSWORD_RESET);
    }

    @Override
    @Async("notificationExecutor")
    public void sendOrderPlaced(String email, String firstName,
                                String orderNumber, String totalAmount,
                                String paymentMethod, String shippingAddress) {
        sendTemplated(email, "Order Confirmed — " + orderNumber,
                "order-placed",
                Map.of("firstName",       firstName,
                        "orderNumber",     orderNumber,
                        "totalAmount",     totalAmount,
                        "paymentMethod",   paymentMethod,
                        "shippingAddress", shippingAddress,
                        "orderUrl",        baseUrl + "/orders/" + orderNumber),
                NotificationLog.NotificationType.ORDER_PLACED);
    }

    @Override
    @Async("notificationExecutor")
    public void sendOrderShipped(String email, String firstName,
                                 String orderNumber, String courierName,
                                 String trackingId, String estimatedDelivery) {
        sendTemplated(email, "Your order is on its way! 🚚 — " + orderNumber,
                "order-shipped",
                Map.of("firstName",        firstName,
                        "orderNumber",      orderNumber,
                        "courierName",      courierName,
                        "trackingId",       trackingId,
                        "estimatedDelivery",estimatedDelivery,
                        "trackingUrl",      baseUrl + "/orders/" + orderNumber),
                NotificationLog.NotificationType.ORDER_SHIPPED);
    }

    @Override
    @Async("notificationExecutor")
    public void sendOrderDelivered(String email, String firstName,
                                   String orderNumber) {
        sendTemplated(email, "Order Delivered ✅ — " + orderNumber,
                "order-placed",          // reuse template with different vars
                Map.of("firstName",    firstName,
                        "orderNumber",  orderNumber,
                        "orderUrl",     baseUrl + "/orders/" + orderNumber),
                NotificationLog.NotificationType.ORDER_DELIVERED);
    }

    @Override
    @Async("notificationExecutor")
    public void sendOrderCancelled(String email, String firstName,
                                   String orderNumber, String reason) {
        sendTemplated(email, "Order Cancelled — " + orderNumber,
                "order-placed",
                Map.of("firstName",   firstName,
                        "orderNumber", orderNumber,
                        "reason",      reason,
                        "orderUrl",    baseUrl + "/orders/" + orderNumber),
                NotificationLog.NotificationType.ORDER_CANCELLED);
    }

    @Override
    @Async("notificationExecutor")
    public void sendPaymentSuccess(String email, String firstName,
                                   String orderNumber, String amount) {
        sendTemplated(email, "Payment Successful — " + orderNumber,
                "order-placed",
                Map.of("firstName",   firstName,
                        "orderNumber", orderNumber,
                        "amount",      amount,
                        "orderUrl",    baseUrl + "/orders/" + orderNumber),
                NotificationLog.NotificationType.PAYMENT_SUCCESS);
    }

    @Override
    @Async("notificationExecutor")
    public void sendRefundInitiated(String email, String firstName,
                                    String orderNumber, String amount) {
        sendTemplated(email, "Refund Initiated — " + orderNumber,
                "order-placed",
                Map.of("firstName",   firstName,
                        "orderNumber", orderNumber,
                        "amount",      amount),
                NotificationLog.NotificationType.REFUND_INITIATED);
    }

    @Override
    @Async("notificationExecutor")
    public void sendOtp(String email, String firstName, String otp) {
        sendTemplated(email, "Your OTP — Jully's Craft", "otp",
                Map.of("firstName", firstName, "otp", otp),
                NotificationLog.NotificationType.OTP);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void sendTemplated(String email, String subject,
                               String template,
                               Map<String, Object> vars,
                               NotificationLog.NotificationType type) {
        NotificationLog logEntry = NotificationLog.builder()
                .channel(NotificationLog.NotificationChannel.EMAIL)
                .type(type)
                .recipient(email)
                .subject(subject)
                .status(NotificationLog.DeliveryStatus.PENDING)
                .build();

        try {
            String html = templateBuilder.build(template, vars);
            sendHtml(email, subject, html);
            logEntry.setStatus(NotificationLog.DeliveryStatus.SENT);
            log.info("Email [{}] sent to {}", type, email);
        } catch (Exception e) {
            logEntry.setStatus(NotificationLog.DeliveryStatus.FAILED);
            logEntry.setErrorMessage(e.getMessage());
            log.error("Email [{}] failed to {}: {}", type, email, e.getMessage());
        } finally {
            logRepository.save(logEntry);
        }
    }

    private void sendHtml(String to, String subject, String html)
            throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(from, fromName);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        mailSender.send(message);
    }

    private NotificationLog buildLog(NotificationEvent event) {
        return NotificationLog.builder()
                .channel(event.getChannel())
                .type(event.getType())
                .recipient(event.getRecipientEmail())
                .subject(event.getSubject())
                .status(NotificationLog.DeliveryStatus.PENDING)
                .build();
    }
}