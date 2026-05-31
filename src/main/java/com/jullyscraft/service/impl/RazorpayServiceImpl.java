package com.jullyscraft.service.impl;

import com.jullyscraft.config.PaymentConfig;
import com.jullyscraft.dto.response.PaymentInitResponse;
import com.jullyscraft.entity.Order;
import com.jullyscraft.entity.Payment;
import com.jullyscraft.exception.BadRequestException;
import com.jullyscraft.service.RazorpayService;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RazorpayServiceImpl implements RazorpayService {

    private final RazorpayClient razorpayClient;
    private final PaymentConfig  paymentConfig;

    @Override
    public PaymentInitResponse createOrder(Order order, Payment payment) {
        try {
            // Razorpay expects amount in smallest currency unit (paise)
            long amountPaise = order.getTotalAmount()
                    .multiply(java.math.BigDecimal.valueOf(100))
                    .longValue();

            JSONObject options = new JSONObject();
            options.put("amount",   amountPaise);
            options.put("currency", paymentConfig.getRazorpayCurrency());
            options.put("receipt",  order.getOrderNumber());
            options.put("payment_capture", 1);

            JSONObject notes = new JSONObject();
            notes.put("orderId",     order.getId());
            notes.put("orderNumber", order.getOrderNumber());
            options.put("notes", notes);

            com.razorpay.Order rzpOrder = razorpayClient.orders.create(options);
            String gatewayOrderId = rzpOrder.get("id");

            payment.setGatewayOrderId(gatewayOrderId);

            log.info("Razorpay order created: {} for order: {}",
                    gatewayOrderId, order.getOrderNumber());

            return PaymentInitResponse.builder()
                    .paymentId(payment.getId())
                    .gatewayOrderId(gatewayOrderId)
                    .keyId(paymentConfig.getRazorpayKeyId())
                    .amount(order.getTotalAmount())
                    .currency(paymentConfig.getRazorpayCurrency())
                    .gateway(Payment.PaymentGateway.RAZORPAY)
                    .orderNumber(order.getOrderNumber())
                    .build();

        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed: {}", e.getMessage());
            throw new BadRequestException("Payment initiation failed: " + e.getMessage());
        }
    }

    @Override
    public boolean verifySignature(String gatewayOrderId,
                                   String gatewayPaymentId,
                                   String signature) {
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id",   gatewayOrderId);
            attributes.put("razorpay_payment_id", gatewayPaymentId);
            attributes.put("razorpay_signature",  signature);
            Utils.verifyPaymentSignature(attributes, paymentConfig.getRazorpayKeySecret());
            return true;
        } catch (RazorpayException e) {
            log.warn("Razorpay signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String processRefund(String gatewayPaymentId, long amountPaise, String reason) {
        try {
            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", amountPaise);
            refundRequest.put("speed",  "normal");
            refundRequest.put("notes",  new JSONObject().put("reason", reason));

            com.razorpay.Refund refund =
                    razorpayClient.payments.refund(gatewayPaymentId, refundRequest);

            String refundId = refund.get("id");
            log.info("Razorpay refund initiated: {} for payment: {}",
                    refundId, gatewayPaymentId);
            return refundId;

        } catch (RazorpayException e) {
            log.error("Razorpay refund failed: {}", e.getMessage());
            throw new BadRequestException("Refund failed: " + e.getMessage());
        }
    }
}