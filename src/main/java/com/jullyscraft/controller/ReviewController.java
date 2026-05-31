package com.jullyscraft.controller;

import com.jullyscraft.dto.request.CreateReviewRequest;
import com.jullyscraft.dto.response.ApiResponse;
import com.jullyscraft.dto.response.PageResponse;
import com.jullyscraft.dto.response.ReviewResponse;
import com.jullyscraft.dto.response.ReviewStatsResponse;
import com.jullyscraft.security.userdetails.UserPrincipal;
import com.jullyscraft.service.ReviewService;
import com.jullyscraft.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(AppConstants.API_BASE + "/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Product reviews and ratings")
public class ReviewController {

    private final ReviewService reviewService;

    // ── Public ────────────────────────────────────────────────────────────────

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get approved reviews for a product")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0")           int    page,
            @RequestParam(defaultValue = "10")          int    size,
            @RequestParam(defaultValue = "createdAt")   String sortBy,
            @RequestParam(defaultValue = "desc")        String sortDir) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewService.getProductReviews(productId, page, size, sortBy, sortDir)));
    }

    @GetMapping("/product/{productId}/stats")
    @Operation(summary = "Get rating stats and distribution for a product")
    public ResponseEntity<ApiResponse<ReviewStatsResponse>> getStats(
            @PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewService.getProductStats(productId)));
    }

    // ── Authenticated ─────────────────────────────────────────────────────────

    @GetMapping("/product/{productId}/my-review")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get my review for a product")
    public ResponseEntity<ApiResponse<ReviewResponse>> getMyReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewService.getMyReview(principal.getId(), productId)));
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a review")
    public ResponseEntity<ApiResponse<ReviewResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateReviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Review submitted — pending moderation",
                reviewService.createReview(principal.getId(), request)));
    }

    @PutMapping("/{reviewId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update my review")
    public ResponseEntity<ApiResponse<ReviewResponse>> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long reviewId,
            @Valid @RequestBody CreateReviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Review updated — pending moderation",
                reviewService.updateReview(principal.getId(), reviewId, request)));
    }

    @DeleteMapping("/{reviewId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete my review")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long reviewId) {
        reviewService.deleteReview(principal.getId(), reviewId);
        return ResponseEntity.ok(ApiResponse.success("Review deleted"));
    }

    @PostMapping("/{reviewId}/helpful")
    @Operation(summary = "Mark review as helpful")
    public ResponseEntity<ApiResponse<Void>> markHelpful(@PathVariable Long reviewId) {
        reviewService.markHelpful(reviewId);
        return ResponseEntity.ok(ApiResponse.success("Marked as helpful"));
    }

    @PostMapping("/{reviewId}/report")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Report a review")
    public ResponseEntity<ApiResponse<Void>> report(@PathVariable Long reviewId) {
        reviewService.reportReview(reviewId);
        return ResponseEntity.ok(ApiResponse.success("Review reported"));
    }
}