package com.jullyscraft.controller;

import com.jullyscraft.dto.response.ApiResponse;
import com.jullyscraft.dto.response.RecommendationResponse;
import com.jullyscraft.entity.UserProductInteraction;
import com.jullyscraft.security.userdetails.UserPrincipal;
import com.jullyscraft.service.RecommendationService;
import com.jullyscraft.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(AppConstants.API_BASE + "/recommendations")
@RequiredArgsConstructor
@Tag(name = "Recommendations", description = "Product recommendation engine")
public class RecommendationController {

    private final RecommendationService recommendationService;

    // ── Product-based (no auth needed) ────────────────────────────────────────

    @GetMapping("/similar/{productId}")
    @Operation(summary = "Get similar products (same category)")
    public ResponseEntity<ApiResponse<RecommendationResponse>> getSimilar(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "8") int limit) {
        return ResponseEntity.ok(ApiResponse.success(
                recommendationService.getSimilarProducts(productId, limit)));
    }

    @GetMapping("/bought-together/{productId}")
    @Operation(summary = "Frequently bought together")
    public ResponseEntity<ApiResponse<RecommendationResponse>> getBoughtTogether(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "6") int limit) {
        return ResponseEntity.ok(ApiResponse.success(
                recommendationService.getFrequentlyBoughtTogether(productId, limit)));
    }

    @GetMapping("/collaborative/{productId}")
    @Operation(summary = "Customers who bought this also bought")
    public ResponseEntity<ApiResponse<RecommendationResponse>> getCollaborative(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "8") int limit) {
        return ResponseEntity.ok(ApiResponse.success(
                recommendationService.getCollaborativeRecommendations(productId, limit)));
    }

    @GetMapping("/trending")
    @Operation(summary = "Trending products this week")
    public ResponseEntity<ApiResponse<RecommendationResponse>> getTrending(
            @RequestParam(defaultValue = "12") int limit) {
        return ResponseEntity.ok(ApiResponse.success(
                recommendationService.getTrendingProducts(limit)));
    }

    @GetMapping("/new-arrivals")
    @Operation(summary = "Newly added products")
    public ResponseEntity<ApiResponse<RecommendationResponse>> getNewArrivals(
            @RequestParam(defaultValue = "12") int limit) {
        return ResponseEntity.ok(ApiResponse.success(
                recommendationService.getNewArrivals(limit)));
    }

    // ── User-based (auth required) ────────────────────────────────────────────

    @GetMapping("/personalized")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Personalized recommendations for logged-in user")
    public ResponseEntity<ApiResponse<RecommendationResponse>> getPersonalized(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "12") int limit) {
        return ResponseEntity.ok(ApiResponse.success(
                recommendationService.getPersonalizedRecommendations(
                        principal.getId(), limit)));
    }

    @GetMapping("/recently-viewed")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Recently viewed products")
    public ResponseEntity<ApiResponse<RecommendationResponse>> getRecentlyViewed(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "8") int limit) {
        return ResponseEntity.ok(ApiResponse.success(
                recommendationService.getRecentlyViewedRecommendations(
                        principal.getId(), limit)));
    }

    // ── AI-powered ────────────────────────────────────────────────────────────

    @GetMapping("/ai")
    @Operation(summary = "AI-powered recommendations (Claude)")
    public ResponseEntity<ApiResponse<RecommendationResponse>> getAi(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false)    Long   productId,
            @RequestParam(defaultValue = "8")  int    limit) {
        Long userId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(ApiResponse.success(
                recommendationService.getAiRecommendations(
                        userId, productId, limit)));
    }

    // ── Interaction tracking ──────────────────────────────────────────────────

    @PostMapping("/interact/{productId}")
    @Operation(summary = "Record a product interaction (view, cart, wishlist)")
    public ResponseEntity<ApiResponse<Void>> recordInteraction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long productId,
            @RequestParam UserProductInteraction.InteractionType type,
            @RequestParam(required = false) String sessionId) {
        Long userId = principal != null ? principal.getId() : null;
        recommendationService.recordInteraction(userId, productId, type, sessionId);
        return ResponseEntity.ok(ApiResponse.success("Interaction recorded"));
    }
}