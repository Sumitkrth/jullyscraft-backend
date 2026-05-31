package com.jullyscraft.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class TopProductResponse {
    private Long       productId;
    private String     productName;
    private String     productSlug;
    private String     primaryImageUrl;
    private String     categoryName;
    private long       unitsSold;
    private BigDecimal revenue;
    private double     averageRating;
    private int        reviewCount;
    private int        currentStock;
}