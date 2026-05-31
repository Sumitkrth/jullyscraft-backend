package com.jullyscraft.controller;

import com.jullyscraft.dto.request.InitiatePaymentRequest;
import com.jullyscraft.dto.request.RefundRequest;
import com.jullyscraft.dto.request.VerifyPaymentRequest;
import com.jullyscraft.dto.response.ApiResponse;
import com.jullyscraft.dto.response.PaymentInitResponse;
import com.jullyscraft.dto.response.PaymentResponse;
import com.jullyscraft.dto.response.RefundResponse;
import com.jullyscraft.security.userdetails.UserPrincipal;
import com.jullyscraft.service.PaymentService;
import com.jullyscraft.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(AppConstants.API_BASE + "/payments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Payments", description = "Payment initiation, verification, and refunds")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initiate")
    @Operation(summary = "Initiate payment — get gateway order/intent details")
    public ResponseEntity<ApiResponse<PaymentInitResponse>> initiate(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody InitiatePaymentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Payment initiated",
                paymentService.initiatePayment(principal.getId(), request)));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify payment after gateway callback")
    public ResponseEntity<ApiResponse<PaymentResponse>> verify(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody VerifyPaymentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Payment verified successfully",
                paymentService.verifyPayment(principal.getId(), request)));
    }

    @PostMapping("/refund")
    @Operation(summary = "Request refund for a paid order")
    public ResponseEntity<ApiResponse<RefundResponse>> refund(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RefundRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Refund initiated",
                paymentService.processRefund(principal.getId(), request)));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get all payments for an order")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getByOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.getPaymentsForOrder(principal.getId(), orderId)));
    }
}