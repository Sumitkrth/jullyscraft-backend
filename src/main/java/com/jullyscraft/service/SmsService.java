package com.jullyscraft.service;

public interface SmsService {
    void sendSms(String phone, String body);
    void sendWhatsApp(String phone, String body);
    void sendOtp(String phone, String otp);
    void sendOrderAlert(String phone, String orderNumber, String status);
    void sendDeliveryUpdate(String phone, String orderNumber,
                            String trackingId, String courier);
}