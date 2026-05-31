package com.jullyscraft.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateReviewRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    @NotBlank(message = "Review title is required")
    @Size(min = 5, max = 200, message = "Title must be 5–200 characters")
    private String title;

    @NotBlank(message = "Review body is required")
    @Size(min = 20, max = 5000, message = "Review must be 20–5000 characters")
    private String body;
}