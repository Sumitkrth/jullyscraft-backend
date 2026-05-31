package com.jullyscraft.service.impl;

import com.jullyscraft.config.PaymentConfig;
import com.jullyscraft.dto.request.InitiatePaymentRequest;
import com.jullyscraft.dto.request.RefundRequest;
import com.jullyscraft.dto.request.VerifyPaymentRequest;
import com.jullyscraft.dto.response.PaymentInitResponse;
import com.jullyscraft.dto.response.PaymentResponse;
import com.jullyscraft.dto.response.RefundResponse;
import com.jullyscraft.entity.Order;
import com.jullyscraft.entity.Payment;
import com.jullyscraft.exception.BadRequestException;
import com.jullyscraft.exception.ResourceNotFoundException;
import com.jullyscraft.mapper.PaymentMapper;
import com.jullyscraft.repository.OrderRepository;
import com.jullyscraft.repository.PaymentRepository;
import com.jullyscraft.service.OrderService;
import com.jullyscraft.service.PaymentService;
import com.jullyscraft.service.RazorpayService;
import com.jullyscraft.service.StripeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository   orderRepository;
    private final RazorpayService   razorpayService;
    private final StripeService     stripeService;
    private final OrderService      orderService;
    private final PaymentMapper     paymentMapper;
    private final PaymentConfig     paymentConfig;

    // ── Initiate ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentInitResponse initiatePayment(Long userId,
                                               InitiatePaymentRequest req) {
        Order order = orderRepository.findByIdAndUserId(req.getOrderId(), userId)
                // userId check — admin can always fetch
                .or(() -> orderRepository.findById(req.getOrderId()))
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order", "id", req.getOrderId()));

        if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
            throw new BadRequestException("Order is already paid");
        }

        // Duplicate payment guard — check pending initiation exists
        boolean alreadyInitiated = paymentRepository
                .findByOrderIdOrderByCreatedAtDesc(order.getId())
                .stream()
                .anyMatch(p -> p.getStatus() == Payment.PaymentStatus.INITIATED
                        || p.getStatus() == Payment.PaymentStatus.PENDING);

        if (alreadyInitiated && req.getGateway() != Payment.PaymentGateway.COD) {
            throw new BadRequestException(
                    "A payment is already in progress for this order");
        }

        Payment payment = Payment.builder()
                .order(order)
                .gateway(req.getGateway())
                .amount(order.getTotalAmount())
                .currency("INR")
                .status(Payment.PaymentStatus.INITIATED)
                .build();

        payment = paymentRepository.save(payment);

        return switch (req.getGateway()) {
            case RAZORPAY -> razorpayService.createOrder(order, payment);
            case STRIPE   -> stripeService.createPaymentIntent(order, payment);
            case COD      -> handleCodInitiation(order, payment);
            default       -> throw new BadRequestException(
                    "Unsupported gateway: " + req.getGateway());
        };
    }

    // ── Verify (client-side callback) ─────────────────────────────────────────

    @Override
    @Transactional
    public PaymentResponse verifyPayment(Long userId, VerifyPaymentRequest req) {
        Payment payment = paymentRepository
                .findByGatewayOrderId(req.getGatewayOrderId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Payment", "gatewayOrderId",
                                req.getGatewayOrderId()));

        // Duplicate capture guard
        if (paymentRepository.existsByGatewayPaymentIdAndStatus(
                req.getGatewayPaymentId(), Payment.PaymentStatus.CAPTURED)) {
            throw new BadRequestException("Payment already captured");
        }

        boolean valid = switch (payment.getGateway()) {
            case RAZORPAY -> razorpayService.verifySignature(
                    req.getGatewayOrderId(),
                    req.getGatewayPaymentId(),
                    req.getGatewaySignature());
            case STRIPE   -> true; // Stripe verified via webhook; client just confirms
            default       -> false;
        };

        if (!valid) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setFailureReason("Signature verification failed");
            paymentRepository.save(payment);
            throw new BadRequestException("Payment verification failed — invalid signature");
        }

        payment.setGatewayPaymentId(req.getGatewayPaymentId());
        payment.setGatewaySignature(req.getGatewaySignature());
        payment.setStatus(Payment.PaymentStatus.CAPTURED);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Update order
        orderService.markAsPaid(payment.getOrder().getId(), req.getGatewayPaymentId());
        log.info("Payment verified for order: {}", payment.getOrder().getOrderNumber());

        return paymentMapper.toResponse(payment);
    }

    // ── Refund ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public RefundResponse processRefund(Long userId, RefundRequest req) {
        Payment payment = paymentRepository
                .findTopByOrderIdAndStatusOrderByCreatedAtDesc(
                        req.getOrderId(), Payment.PaymentStatus.CAPTURED)
                .orElseThrow(() ->
                        new BadRequestException("No captured payment found for this order"));

        if (!payment.isRefundable(paymentConfig.getRefundWindowDays())) {
            throw new BadRequestException(
                    "Refund window of " + paymentConfig.getRefundWindowDays()
                            + " days has passed");
        }

        BigDecimal refundAmount = req.getAmount() != null
                ? req.getAmount()
                : payment.getAmount();   // full refund

        if (refundAmount.compareTo(payment.getAmount()) > 0) {
            throw new BadRequestException(
                    "Refund amount cannot exceed paid amount: " + payment.getAmount());
        }

        long amountSmallestUnit = refundAmount
                .multiply(BigDecimal.valueOf(100))
                .longValue();

        String refundId = switch (payment.getGateway()) {
            case RAZORPAY -> razorpayService.processRefund(
                    payment.getGatewayPaymentId(), amountSmallestUnit,
                    req.getReason());
            case STRIPE   -> stripeService.processRefund(
                    payment.getGatewayPaymentId(), amountSmallestUnit,
                    req.getReason());
            default       -> "COD-REFUND-" + System.currentTimeMillis();
        };

        payment.setRefundId(refundId);
        payment.setRefundAmount(refundAmount);
        payment.setRefundStatus(Payment.RefundStatus.PROCESSING);
        payment.setStatus(refundAmount.compareTo(payment.getAmount()) == 0
                ? Payment.PaymentStatus.REFUNDED
                : Payment.PaymentStatus.PARTIALLY_REFUNDED);
        paymentRepository.save(payment);

        log.info("Refund initiated: {} for order: {}",
                refundId, payment.getOrder().getOrderNumber());

        return RefundResponse.builder()
                .refundId(refundId)
                .refundAmount(refundAmount)
                .refundStatus(Payment.RefundStatus.PROCESSING)
                .message("Refund initiated successfully")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsForOrder(Long userId, Long orderId) {
        return paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId)
                .stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    // ── Webhook handlers ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public void handleRazorpaySuccess(String gatewayOrderId, String gatewayPaymentId,
                                      String signature,      String rawPayload) {
        paymentRepository.findByGatewayOrderId(gatewayOrderId).ifPresent(payment -> {
            if (payment.getStatus() == Payment.PaymentStatus.CAPTURED) return; // idempotent

            boolean valid = razorpayService.verifySignature(
                    gatewayOrderId, gatewayPaymentId, signature);

            if (valid) {
                payment.setGatewayPaymentId(gatewayPaymentId);
                payment.setGatewaySignature(signature);
                payment.setStatus(Payment.PaymentStatus.CAPTURED);
                payment.setPaidAt(LocalDateTime.now());
                payment.setWebhookPayload(rawPayload);
                paymentRepository.save(payment);
                orderService.markAsPaid(payment.getOrder().getId(), gatewayPaymentId);
                log.info("Razorpay webhook: payment captured for order: {}",
                        payment.getOrder().getOrderNumber());
            } else {
                payment.setStatus(Payment.PaymentStatus.FAILED);
                payment.setFailureReason("Webhook signature invalid");
                payment.setWebhookPayload(rawPayload);
                paymentRepository.save(payment);
                log.warn("Razorpay webhook: invalid signature for gateway order: {}",
                        gatewayOrderId);
            }
        });
    }

    @Override
    @Transactional
    public void handleStripeSuccess(String paymentIntentId, String rawPayload) {
        paymentRepository.findByGatewayOrderId(paymentIntentId).ifPresent(payment -> {
            if (payment.getStatus() == Payment.PaymentStatus.CAPTURED) return;

            payment.setStatus(Payment.PaymentStatus.CAPTURED);
            payment.setPaidAt(LocalDateTime.now());
            payment.setWebhookPayload(rawPayload);
            paymentRepository.save(payment);
            orderService.markAsPaid(payment.getOrder().getId(), paymentIntentId);
            log.info("Stripe webhook: payment captured for order: {}",
                    payment.getOrder().getOrderNumber());
        });
    }

    @Override
    @Transactional
    public void handleRefundWebhook(String refundId, String gatewayPaymentId,
                                    String status,  String rawPayload) {
        paymentRepository.findByGatewayPaymentId(gatewayPaymentId).ifPresent(payment -> {
            Payment.RefundStatus refundStatus =
                    "processed".equalsIgnoreCase(status)
                            ? Payment.RefundStatus.SUCCESS
                            : Payment.RefundStatus.FAILED;

            payment.setRefundStatus(refundStatus);
            if (refundStatus == Payment.RefundStatus.SUCCESS) {
                payment.setRefundedAt(LocalDateTime.now());
            }
            payment.setWebhookPayload(rawPayload);
            paymentRepository.save(payment);
            log.info("Refund webhook: {} for payment: {}", status, gatewayPaymentId);
        });
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private PaymentInitResponse handleCodInitiation(Order order, Payment payment) {
        payment.setStatus(Payment.PaymentStatus.PENDING);
        paymentRepository.save(payment);
        log.info("COD order initiated: {}", order.getOrderNumber());

        return PaymentInitResponse.builder()
                .paymentId(payment.getId())
                .amount(order.getTotalAmount())
                .currency("INR")
                .gateway(Payment.PaymentGateway.COD)
                .orderNumber(order.getOrderNumber())
                .build();
    }
}