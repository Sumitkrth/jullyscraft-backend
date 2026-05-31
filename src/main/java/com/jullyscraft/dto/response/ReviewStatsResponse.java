package com.jullyscraft.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class ReviewStatsResponse {
    private Long              productId;
    private double            averageRating;
    private long              totalReviews;
    private Map<Integer, Long> ratingDistribution;  // {5: 42, 4: 18, ...}
}