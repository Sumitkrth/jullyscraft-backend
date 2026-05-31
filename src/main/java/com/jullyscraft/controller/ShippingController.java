package com.jullyscraft.controller;

import com.jullyscraft.dto.request.ShippingRateRequest;
import com.jullyscraft.dto.response.ApiResponse;
import com.jullyscraft.dto.response.ShipmentResponse;
import com.jullyscraft.dto.response.ShippingRateResponse;
import com.jullyscraft.dto.response.TrackingResponse;
import com.jullyscraft.service.ShippingService;
import com.jullyscraft.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.jullyscraft.security.userdetails.UserPrincipal;

@RestController
@RequestMapping(AppConstants.API_BASE + "/shipping")
@RequiredArgsConstructor
@Tag(name = "Shipping", description = "Shipping rates, tracking, and shipment info")
public class ShippingController {

    private final ShippingService shippingService;

    @PostMapping("/rates")
    @Operation(summary = "Get shipping rates for a route")
    public ResponseEntity<ApiResponse<ShippingRateResponse>> getRates(
            @Valid @RequestBody ShippingRateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                shippingService.getRates(request)));
    }

    @PostMapping("/rates/all")
    @Operation(summary = "Get rates from ALL enabled providers")
    public ResponseEntity<ApiResponse<ShippingRateResponse>> getAllRates(
            @Valid @RequestBody ShippingRateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                shippingService.getAllProviderRates(request)));
    }

    @GetMapping("/track/{awbNumber}")
    @Operation(summary = "Track shipment by AWB number (public)")
    public ResponseEntity<ApiResponse<TrackingResponse>> trackByAwb(
            @PathVariable String awbNumber) {
        return ResponseEntity.ok(ApiResponse.success(
                shippingService.trackByAwb(awbNumber)));
    }

    @GetMapping("/order/{orderId}/track")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Track shipment by order ID")
    public ResponseEntity<ApiResponse<TrackingResponse>> trackByOrder(
            @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(
                shippingService.trackByOrderId(orderId)));
    }

    @GetMapping("/order/{orderId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get shipment details for an order")
    public ResponseEntity<ApiResponse<ShipmentResponse>> getShipment(
            @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(
                shippingService.getShipmentByOrderId(orderId)));
    }
}