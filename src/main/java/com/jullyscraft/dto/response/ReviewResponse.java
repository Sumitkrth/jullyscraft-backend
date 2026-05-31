package com.jullyscraft.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jullyscraft.entity.Review;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReviewResponse {
    private Long                 id;
    private Long                 productId;
    private Long                 userId;
    private String               userName;
    private String               userAvatar;
    private int                  rating;
    private String               title;
    private String               body;
    private boolean              verifiedPurchase;
    private Review.ReviewStatus  status;
    private Review.Sentiment     sentiment;
    private boolean              spamFlagged;
    private int                  helpfulCount;
    private int                  reportCount;
    private LocalDateTime        createdAt;

    // Admin-only fields — set via @JsonView or excluded in public mapper
    private String               moderationNote;
    private String               spamReason;
}