package com.jullyscraft.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductSummaryResponse {
    private Long       id;
    private String     name;
    private String     slug;
    private String     brand;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private int        discountPercent;
    private String     primaryImageUrl;
    private double     averageRating;
    private int        reviewCount;
    private boolean    inStock;
    private boolean    featured;
    private String     categoryName;
}