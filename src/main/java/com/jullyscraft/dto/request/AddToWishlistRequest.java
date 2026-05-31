package com.jullyscraft.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AddToWishlistRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;
}