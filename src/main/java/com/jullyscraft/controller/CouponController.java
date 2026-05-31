package com.jullyscraft.controller;

import com.jullyscraft.dto.request.ApplyCouponRequest;
import com.jullyscraft.dto.response.ApiResponse;
import com.jullyscraft.dto.response.ApplyCouponResponse;
import com.jullyscraft.dto.response.CouponResponse;
import com.jullyscraft.security.userdetails.UserPrincipal;
import com.jullyscraft.service.CouponService;
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
@RequestMapping(AppConstants.API_BASE + "/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupons", description = "Coupon validation and flash sales")
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/apply")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Validate and apply a coupon code")
    public ResponseEntity<ApiResponse<ApplyCouponResponse>> apply(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ApplyCouponRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Coupon applied successfully",
                couponService.validateAndApply(principal.getId(), request)));
    }

    @GetMapping("/flash-sales")
    @Operation(summary = "Get active flash sale coupons")
    public ResponseEntity<ApiResponse<List<CouponResponse>>> flashSales() {
        return ResponseEntity.ok(ApiResponse.success(
                couponService.getActiveFlashSales()));
    }
}