package com.jullyscraft.service;

import com.jullyscraft.dto.response.RecommendationResponse;
import com.jullyscraft.entity.UserProductInteraction;

public interface RecommendationService {

    // ── Product-based ─────────────────────────────────────────────────────────
    RecommendationResponse getSimilarProducts(Long productId, int limit);
    RecommendationResponse getFrequentlyBoughtTogether(Long productId, int limit);
    RecommendationResponse getCollaborativeRecommendations(Long productId, int limit);

    // ── User-based ────────────────────────────────────────────────────────────
    RecommendationResponse getPersonalizedRecommendations(Long userId, int limit);
    RecommendationResponse getRecentlyViewedRecommendations(Long userId, int limit);

    // ── Global ────────────────────────────────────────────────────────────────
    RecommendationResponse getTrendingProducts(int limit);
    RecommendationResponse getNewArrivals(int limit);

    // ── AI-powered ────────────────────────────────────────────────────────────
    RecommendationResponse getAiRecommendations(Long userId, Long productId, int limit);

    // ── Interaction tracking ──────────────────────────────────────────────────
    void recordInteraction(Long userId, Long productId,
                           UserProductInteraction.InteractionType type,
                           String sessionId);
}