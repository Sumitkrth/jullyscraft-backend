package com.jullyscraft.controller;

import com.jullyscraft.dto.request.ModerateReviewRequest;
import com.jullyscraft.dto.response.ApiResponse;
import com.jullyscraft.dto.response.PageResponse;
import com.jullyscraft.dto.response.ReviewResponse;
import com.jullyscraft.service.ReviewService;
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
@RequestMapping(AppConstants.ADMIN_BASE + "/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin — Reviews", description = "Review moderation queue")
public class AdminReviewController {

    private final ReviewService reviewService;

    @GetMapping("/pending")
    @Operation(summary = "Get reviews pending moderation")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getPending(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewService.getPendingReviews(page, size)));
    }

    @GetMapping("/spam")
    @Operation(summary = "Get AI-flagged spam reviews")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getSpam(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewService.getSpamFlaggedReviews(page, size)));
    }

    @GetMapping("/reported")
    @Operation(summary = "Get user-reported reviews")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getReported(
            @RequestParam(defaultValue = "3")  int threshold,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewService.getReportedReviews(threshold, page, size)));
    }

    @PatchMapping("/{reviewId}/moderate")
    @Operation(summary = "Approve or reject a review")
    public ResponseEntity<ApiResponse<ReviewResponse>> moderate(
            @PathVariable Long reviewId,
            @Valid @RequestBody ModerateReviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Review moderated",
                reviewService.moderateReview(reviewId, request)));
    }
}