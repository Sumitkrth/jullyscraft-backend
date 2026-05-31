package com.jullyscraft.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AddToCartRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    private Long variantId;   // optional

    @NotNull(message = "Quantity is required")
    @Min(value = 1,  message = "Quantity must be at least 1")
    @Max(value = 100, message = "Maximum 100 units per item")
    private Integer quantity;
}