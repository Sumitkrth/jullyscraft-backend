package com.jullyscraft.controller;

import com.jullyscraft.dto.request.ShipOrderRequest;
import com.jullyscraft.dto.response.ApiResponse;
import com.jullyscraft.dto.response.PageResponse;
import com.jullyscraft.dto.response.ShipmentResponse;
import com.jullyscraft.entity.Shipment;
import com.jullyscraft.service.ShippingService;
import com.jullyscraft.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(AppConstants.ADMIN_BASE + "/shipping")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin — Shipping", description = "Admin shipment management")
public class AdminShippingController {

    private final ShippingService shippingService;

    @GetMapping
    @Operation(summary = "Get all shipments (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<ShipmentResponse>>> getAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                shippingService.getAllShipments(page, size)));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get shipments by status")
    public ResponseEntity<ApiResponse<PageResponse<ShipmentResponse>>> getByStatus(
            @PathVariable Shipment.ShipmentStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                shippingService.getShipmentsByStatus(status, page, size)));
    }

    @PostMapping("/ship")
    @Operation(summary = "Book shipment for an order")
    public ResponseEntity<ApiResponse<ShipmentResponse>> ship(
            @Valid @RequestBody ShipOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Shipment booked successfully",
                shippingService.shipOrder(request)));
    }

    @PostMapping("/order/{orderId}/cancel")
    @Operation(summary = "Cancel a shipment")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable Long orderId) {
        boolean cancelled = shippingService.cancelShipment(orderId);
        return ResponseEntity.ok(ApiResponse.success(
                cancelled ? "Shipment cancelled" : "Cancellation failed"));
    }

    @PostMapping("/order/{orderId}/label")
    @Operation(summary = "Generate / refresh shipping label")
    public ResponseEntity<ApiResponse<ShipmentResponse>> generateLabel(
            @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Label generated",
                shippingService.generateLabel(orderId)));
    }

    @PatchMapping("/order/{orderId}/manual-tracking")
    @Operation(summary = "Update tracking info manually (for manual couriers)")
    public ResponseEntity<ApiResponse<ShipmentResponse>> manualTracking(
            @PathVariable Long orderId,
            @RequestParam String awbNumber,
            @RequestParam String courierName,
            @RequestParam(required = false) String trackingUrl) {
        return ResponseEntity.ok(ApiResponse.success(
                "Tracking updated",
                shippingService.updateTrackingManually(
                        orderId, awbNumber, courierName, trackingUrl)));
    }
}