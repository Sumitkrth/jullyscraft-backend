package com.jullyscraft.service.impl;

import com.jullyscraft.dto.request.CreateReviewRequest;
import com.jullyscraft.dto.request.ModerateReviewRequest;
import com.jullyscraft.dto.response.PageResponse;
import com.jullyscraft.dto.response.ReviewResponse;
import com.jullyscraft.dto.response.ReviewStatsResponse;
import com.jullyscraft.entity.Product;
import com.jullyscraft.entity.Review;
import com.jullyscraft.entity.User;
import com.jullyscraft.exception.BadRequestException;
import com.jullyscraft.exception.DuplicateResourceException;
import com.jullyscraft.exception.ForbiddenException;
import com.jullyscraft.exception.ResourceNotFoundException;
import com.jullyscraft.mapper.ReviewMapper;
import com.jullyscraft.repository.ProductRepository;
import com.jullyscraft.repository.ReviewRepository;
import com.jullyscraft.repository.UserRepository;
import com.jullyscraft.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository  reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository    userRepository;
    private final ReviewMapper      reviewMapper;
    private final AiReviewAnalyzer  aiReviewAnalyzer;   // inner component below

    // ── Public ────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getProductReviews(
            Long productId, int page, int size,
            String sortBy, String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        return PageResponse.of(
                reviewRepository.findByProductIdAndStatusAndDeletedFalse(
                                productId, Review.ReviewStatus.APPROVED,
                                PageRequest.of(page, size, sort))
                        .map(reviewMapper::toPublicResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewStatsResponse getProductStats(Long productId) {
        double avg     = reviewRepository.findAverageRating(productId) != null
                ? reviewRepository.findAverageRating(productId) : 0.0;
        long   total   = reviewRepository.countApproved(productId);
        Map<Integer, Long> dist = new HashMap<>();

        reviewRepository.findRatingDistribution(productId)
                .forEach(row -> dist.put(((Number) row[0]).intValue(),
                        ((Number) row[1]).longValue()));

        return ReviewStatsResponse.builder()
                .productId(productId)
                .averageRating(Math.round(avg * 10.0) / 10.0)
                .totalReviews(total)
                .ratingDistribution(dist)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getMyReview(Long userId, Long productId) {
        Review review = reviewRepository
                .findByProductIdAndUserIdAndDeletedFalse(productId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Review", "productId", productId));
        return reviewMapper.toPublicResponse(review);
    }

    // ── User ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ReviewResponse createReview(Long userId, CreateReviewRequest req) {
        if (reviewRepository.existsByProductIdAndUserIdAndDeletedFalse(
                req.getProductId(), userId)) {
            throw new DuplicateResourceException(
                    "You have already reviewed this product");
        }

        Product product = productRepository.findById(req.getProductId())
                .filter(p -> !p.isDeleted() && p.isActive())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product", "id", req.getProductId()));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        boolean verified = reviewRepository.hasVerifiedPurchase(req.getProductId(), userId);

        Review review = Review.builder()
                .product(product)
                .user(user)
                .rating(req.getRating())
                .title(req.getTitle())
                .body(req.getBody())
                .verifiedPurchase(verified)
                .status(Review.ReviewStatus.PENDING)
                .build();

        Review saved = reviewRepository.save(review);

        // Async AI analysis — non-blocking
        aiReviewAnalyzer.analyze(saved.getId(), req.getTitle(), req.getBody());

        log.info("Review created by user {} for product {}", userId, req.getProductId());
        return reviewMapper.toPublicResponse(saved);
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(Long userId, Long reviewId,
                                       CreateReviewRequest req) {
        Review review = findReviewOwnedByUser(reviewId, userId);

        if (review.getStatus() == Review.ReviewStatus.APPROVED) {
            throw new BadRequestException(
                    "Approved reviews cannot be edited — delete and re-submit");
        }

        review.setRating(req.getRating());
        review.setTitle(req.getTitle());
        review.setBody(req.getBody());
        review.setStatus(Review.ReviewStatus.PENDING);  // re-queue for moderation
        review.setSentiment(null);
        review.setSpamFlagged(false);
        review.setSpamReason(null);

        Review saved = reviewRepository.save(review);
        aiReviewAnalyzer.analyze(saved.getId(), req.getTitle(), req.getBody());

        return reviewMapper.toPublicResponse(saved);
    }

    @Override
    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        Review review = findReviewOwnedByUser(reviewId, userId);
        review.softDelete();
        reviewRepository.save(review);
        refreshProductRating(review.getProduct().getId());
        log.info("Review {} deleted by user {}", reviewId, userId);
    }

    @Override
    @Transactional
    public void markHelpful(Long reviewId) {
        reviewRepository.findById(reviewId)
                .filter(r -> !r.isDeleted()
                        && r.getStatus() == Review.ReviewStatus.APPROVED)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Review", "id", reviewId));
        reviewRepository.incrementHelpful(reviewId);
    }

    @Override
    @Transactional
    public void reportReview(Long reviewId) {
        reviewRepository.findById(reviewId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Review", "id", reviewId));
        reviewRepository.incrementReportCount(reviewId);
        log.info("Review {} reported", reviewId);
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getPendingReviews(int page, int size) {
        return PageResponse.of(
                reviewRepository.findByStatusAndDeletedFalse(
                                Review.ReviewStatus.PENDING,
                                PageRequest.of(page, size,
                                        Sort.by("createdAt").ascending()))
                        .map(reviewMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getSpamFlaggedReviews(int page, int size) {
        return PageResponse.of(
                reviewRepository.findBySpamFlaggedTrueAndDeletedFalse(
                                PageRequest.of(page, size,
                                        Sort.by("createdAt").descending()))
                        .map(reviewMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getReportedReviews(
            int threshold, int page, int size) {
        return PageResponse.of(
                reviewRepository.findByReportCountGreaterThanAndDeletedFalse(
                                threshold,
                                PageRequest.of(page, size,
                                        Sort.by("reportCount").descending()))
                        .map(reviewMapper::toResponse));
    }

    @Override
    @Transactional
    public ReviewResponse moderateReview(Long reviewId, ModerateReviewRequest req) {
        Review review = reviewRepository.findById(reviewId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Review", "id", reviewId));

        Review.ReviewStatus previous = review.getStatus();
        review.setStatus(req.getStatus());
        review.setModerationNote(req.getNote());
        Review saved = reviewRepository.save(review);

        // Refresh product rating when approval status changes
        boolean statusChanged = previous != req.getStatus();
        boolean approvalChanged =
                (req.getStatus() == Review.ReviewStatus.APPROVED)
                        || (previous        == Review.ReviewStatus.APPROVED);

        if (statusChanged && approvalChanged) {
            refreshProductRating(review.getProduct().getId());
        }

        log.info("Review {} moderated: {} → {}", reviewId, previous, req.getStatus());
        return reviewMapper.toResponse(saved);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Review findReviewOwnedByUser(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Review", "id", reviewId));
        if (!review.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You can only modify your own reviews");
        }
        return review;
    }

    @Transactional
    public void refreshProductRating(Long productId) {
        Double avg   = reviewRepository.findAverageRating(productId);
        long   count = reviewRepository.countApproved(productId);
        productRepository.updateRating(
                productId,
                avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0,
                (int) count);
        log.debug("Product {} rating refreshed: avg={}, count={}",
                productId, avg, count);
    }
}