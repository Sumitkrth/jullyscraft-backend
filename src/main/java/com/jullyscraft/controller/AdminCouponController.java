package com.jullyscraft.controller;

import com.jullyscraft.dto.request.CreateCouponRequest;
import com.jullyscraft.dto.response.ApiResponse;
import com.jullyscraft.dto.response.CouponResponse;
import com.jullyscraft.dto.response.PageResponse;
import com.jullyscraft.service.CouponService;
import com.jullyscraft.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(AppConstants.ADMIN_BASE + "/coupons")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin — Coupons", description = "Full coupon lifecycle management")
public class AdminCouponController {

    private final CouponService couponService;

    @GetMapping
    @Operation(summary = "Get all coupons (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<CouponResponse>>> getAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                couponService.getAll(page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get coupon by ID")
    public ResponseEntity<ApiResponse<CouponResponse>> getById(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(couponService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Create a coupon")
    public ResponseEntity<ApiResponse<CouponResponse>> create(
            @Valid @RequestBody CreateCouponRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Coupon created successfully",
                        couponService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a coupon")
    public ResponseEntity<ApiResponse<CouponResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateCouponRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Coupon updated successfully",
                couponService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a coupon")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        couponService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Coupon deleted"));
    }

    @PatchMapping("/{id}/toggle-active")
    @Operation(summary = "Toggle coupon active/inactive")
    public ResponseEntity<ApiResponse<CouponResponse>> toggleActive(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Coupon status updated",
                couponService.toggleActive(id)));
    }

    @PostMapping("/deactivate-expired")
    @Operation(summary = "Manually trigger expired coupon cleanup")
    public ResponseEntity<ApiResponse<Void>> deactivateExpired() {
        couponService.deactivateExpired();
        return ResponseEntity.ok(ApiResponse.success(
                "Expired coupons deactivated"));
    }
}