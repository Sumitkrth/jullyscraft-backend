package com.jullyscraft.service.impl;

import com.jullyscraft.entity.NotificationLog;
import com.jullyscraft.repository.NotificationLogRepository;
import com.jullyscraft.service.SmsService;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmsServiceImpl implements SmsService {

    @Value("${app.notification.twilio.account-sid:}")
    private String accountSid;

    @Value("${app.notification.twilio.auth-token:}")
    private String authToken;

    @Value("${app.notification.twilio.from-number:}")
    private String fromNumber;

    @Value("${app.notification.twilio.whatsapp-from:}")
    private String whatsappFrom;

    private final NotificationLogRepository logRepository;
    private boolean twilioEnabled = false;

    public SmsServiceImpl(NotificationLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @PostConstruct
    public void init() {
        if (accountSid != null && !accountSid.isBlank()
                && authToken != null && !authToken.isBlank()) {
            Twilio.init(accountSid, authToken);
            twilioEnabled = true;
            log.info("Twilio SMS initialized");
        } else {
            log.warn("Twilio not configured — SMS notifications disabled");
        }
    }

    // ── SMS ───────────────────────────────────────────────────────────────────

    @Override
    @Async("notificationExecutor")
    public void sendSms(String phone, String body) {
        if (!twilioEnabled) {
            log.debug("SMS skipped (Twilio disabled): {}", phone);
            return;
        }
        NotificationLog logEntry = buildLog(phone, body,
                NotificationLog.NotificationChannel.SMS,
                NotificationLog.NotificationType.PROMOTIONAL);
        try {
            Message msg = Message.creator(
                    new PhoneNumber(phone),
                    new PhoneNumber(fromNumber),
                    body).create();
            logEntry.setExternalId(msg.getSid());
            logEntry.setStatus(NotificationLog.DeliveryStatus.SENT);
            log.info("SMS sent to {}: {}", phone, msg.getSid());
        } catch (Exception e) {
            logEntry.setStatus(NotificationLog.DeliveryStatus.FAILED);
            logEntry.setErrorMessage(e.getMessage());
            log.error("SMS failed to {}: {}", phone, e.getMessage());
        } finally {
            logRepository.save(logEntry);
        }
    }

    // ── WhatsApp ──────────────────────────────────────────────────────────────

    @Override
    @Async("notificationExecutor")
    public void sendWhatsApp(String phone, String body) {
        if (!twilioEnabled) {
            log.debug("WhatsApp skipped (Twilio disabled): {}", phone);
            return;
        }
        String whatsappTo = phone.startsWith("whatsapp:")
                ? phone : "whatsapp:" + phone;

        NotificationLog logEntry = buildLog(phone, body,
                NotificationLog.NotificationChannel.WHATSAPP,
                NotificationLog.NotificationType.PROMOTIONAL);
        try {
            Message msg = Message.creator(
                    new PhoneNumber(whatsappTo),
                    new PhoneNumber(whatsappFrom),
                    body).create();
            logEntry.setExternalId(msg.getSid());
            logEntry.setStatus(NotificationLog.DeliveryStatus.SENT);
            log.info("WhatsApp sent to {}: {}", phone, msg.getSid());
        } catch (Exception e) {
            logEntry.setStatus(NotificationLog.DeliveryStatus.FAILED);
            logEntry.setErrorMessage(e.getMessage());
            log.error("WhatsApp failed to {}: {}", phone, e.getMessage());
        } finally {
            logRepository.save(logEntry);
        }
    }

    // ── Typed convenience methods ─────────────────────────────────────────────

    @Override
    @Async("notificationExecutor")
    public void sendOtp(String phone, String otp) {
        String body = "Your Jully's Craft OTP is: " + otp
                + ". Valid for 10 minutes. Do not share with anyone.";
        sendSms(phone, body);
    }

    @Override
    @Async("notificationExecutor")
    public void sendOrderAlert(String phone, String orderNumber, String status) {
        String body = "Jully's Craft: Your order " + orderNumber
                + " status has been updated to: " + status
                + ". Track at jullyscraft.com/orders/" + orderNumber;
        sendSms(phone, body);
        sendWhatsApp(phone, body);
    }

    @Override
    @Async("notificationExecutor")
    public void sendDeliveryUpdate(String phone, String orderNumber,
                                   String trackingId, String courier) {
        String body = "Jully's Craft: Order " + orderNumber
                + " has been shipped via " + courier
                + ". Tracking ID: " + trackingId;
        sendSms(phone, body);
        sendWhatsApp(phone, body);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private NotificationLog buildLog(String phone, String body,
                                     NotificationLog.NotificationChannel channel,
                                     NotificationLog.NotificationType type) {
        return NotificationLog.builder()
                .channel(channel)
                .type(type)
                .recipient(phone)
                .subject(type.name())
                .body(body)
                .status(NotificationLog.DeliveryStatus.PENDING)
                .build();
    }
}