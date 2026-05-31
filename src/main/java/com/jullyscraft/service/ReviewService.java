package com.jullyscraft.service;

import com.jullyscraft.dto.request.CreateReviewRequest;
import com.jullyscraft.dto.request.ModerateReviewRequest;
import com.jullyscraft.dto.response.PageResponse;
import com.jullyscraft.dto.response.ReviewResponse;
import com.jullyscraft.dto.response.ReviewStatsResponse;

public interface ReviewService {

    // ── Public ────────────────────────────────────────────────────────────────
    PageResponse<ReviewResponse> getProductReviews(Long productId, int page, int size,
                                                   String sortBy, String sortDir);
    ReviewStatsResponse          getProductStats(Long productId);
    ReviewResponse               getMyReview(Long userId, Long productId);

    // ── User ──────────────────────────────────────────────────────────────────
    ReviewResponse               createReview(Long userId, CreateReviewRequest request);
    ReviewResponse               updateReview(Long userId, Long reviewId,
                                              CreateReviewRequest request);
    void                         deleteReview(Long userId, Long reviewId);
    void                         markHelpful(Long reviewId);
    void                         reportReview(Long reviewId);

    // ── Admin ─────────────────────────────────────────────────────────────────
    PageResponse<ReviewResponse> getPendingReviews(int page, int size);
    PageResponse<ReviewResponse> getSpamFlaggedReviews(int page, int size);
    PageResponse<ReviewResponse> getReportedReviews(int threshold, int page, int size);
    ReviewResponse               moderateReview(Long reviewId,
                                                ModerateReviewRequest request);
}