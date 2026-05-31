package com.jullyscraft.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WishlistItemResponse {
    private Long          id;
    private Long          productId;
    private String        productName;
    private String        productSlug;
    private String        primaryImageUrl;
    private BigDecimal    price;
    private BigDecimal    discountPrice;
    private int           discountPercent;
    private boolean       inStock;
    private double        averageRating;
    private LocalDateTime addedAt;
}