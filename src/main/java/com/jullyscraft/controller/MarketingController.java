package com.jullyscraft.controller;

import com.jullyscraft.dto.request.AffiliateRequest;
import com.jullyscraft.dto.request.NewsletterSubscribeRequest;
import com.jullyscraft.dto.response.*;
import com.jullyscraft.entity.AffiliatePartner;
import com.jullyscraft.security.userdetails.UserPrincipal;
import com.jullyscraft.service.MarketingService;
import com.jullyscraft.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(AppConstants.API_BASE + "/marketing")
@RequiredArgsConstructor
@Tag(name = "Marketing", description = "Newsletter, referral, and affiliate")
public class MarketingController {

    private final MarketingService marketingService;

    // ── Newsletter ────────────────────────────────────────────────────────────

    @PostMapping("/newsletter/subscribe")
    @Operation(summary = "Subscribe to newsletter")
    public ResponseEntity<ApiResponse<Void>> subscribe(
            @Valid @RequestBody NewsletterSubscribeRequest request) {
        marketingService.subscribe(request);
        return ResponseEntity.ok(
                ApiResponse.success("Subscribed successfully"));
    }

    @GetMapping("/newsletter/unsubscribe")
    @Operation(summary = "Unsubscribe via token (from email link)")
    public ResponseEntity<ApiResponse<Void>> unsubscribeByToken(
            @RequestParam String token) {
        marketingService.unsubscribeByToken(token);
        return ResponseEntity.ok(
                ApiResponse.success("Unsubscribed successfully"));
    }

    @DeleteMapping("/newsletter/unsubscribe")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Unsubscribe logged-in user")
    public ResponseEntity<ApiResponse<Void>> unsubscribeMe(
            @AuthenticationPrincipal UserPrincipal principal) {
        marketingService.unsubscribeByEmail(principal.getEmail());
        return ResponseEntity.ok(
                ApiResponse.success("Unsubscribed successfully"));
    }

    // ── Referral ──────────────────────────────────────────────────────────────

    @GetMapping("/referral/my-code")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get or create my referral code")
    public ResponseEntity<ApiResponse<ReferralResponse>> getMyReferralCode(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                marketingService.getOrCreateReferralCode(principal.getId())));
    }

    @PostMapping("/referral/apply")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Apply a referral code (on signup / first order)")
    public ResponseEntity<ApiResponse<ReferralResponse>> applyReferral(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String code) {
        return ResponseEntity.ok(ApiResponse.success(
                "Referral code applied",
                marketingService.applyReferralCode(
                        principal.getId(), code)));
    }

    // ── Affiliate ─────────────────────────────────────────────────────────────

    @PostMapping("/affiliate/apply")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Apply to become an affiliate partner")
    public ResponseEntity<ApiResponse<AffiliateResponse>> applyAffiliate(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AffiliateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Affiliate application submitted",
                marketingService.applyAffiliate(
                        principal.getId(), request)));
    }

    @GetMapping("/affiliate/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get my affiliate account details")
    public ResponseEntity<ApiResponse<AffiliateResponse>> getMyAffiliate(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                marketingService.getMyAffiliate(principal.getId())));
    }

    @GetMapping("/affiliate/{code}/click")
    @Operation(summary = "Track affiliate link click")
    public ResponseEntity<ApiResponse<Void>> trackClick(
            @PathVariable String code) {
        marketingService.recordAffiliateClick(code);
        return ResponseEntity.ok(ApiResponse.success("Click recorded"));
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @GetMapping("/admin/affiliates")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Admin — get all affiliate applications")
    public ResponseEntity<ApiResponse<PageResponse<AffiliateResponse>>> getAll(
            @RequestParam(required = false)    AffiliatePartner.AffiliateStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                marketingService.getAllAffiliates(status, page, size)));
    }

    @PatchMapping("/admin/affiliates/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Admin — approve affiliate partner")
    public ResponseEntity<ApiResponse<AffiliateResponse>> approve(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Affiliate approved",
                marketingService.approveAffiliate(id)));
    }

    @PatchMapping("/admin/affiliates/{id}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Admin — suspend affiliate partner")
    public ResponseEntity<ApiResponse<AffiliateResponse>> suspend(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Affiliate suspended",
                marketingService.suspendAffiliate(id)));
    }

    @GetMapping("/admin/newsletter/count")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Admin — total active newsletter subscribers")
    public ResponseEntity<ApiResponse<Long>> subscriberCount() {
        return ResponseEntity.ok(ApiResponse.success(
                marketingService.getSubscriberCount()));
    }
}