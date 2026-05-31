package com.jullyscraft.dto.request;

import com.jullyscraft.entity.Review;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ModerateReviewRequest {

    @NotNull(message = "Status is required")
    private Review.ReviewStatus status;

    private String note;
}