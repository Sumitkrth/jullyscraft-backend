package com.jullyscraft.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class ProductFilterRequest {
    private String  keyword;
    private Long    categoryId;
    private String  brand;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Double  minRating;
    private Boolean inStock;
    private Boolean featured;
    private int     page    = 0;
    private int     size    = 12;
    private String  sortBy  = "createdAt";
    private String  sortDir = "desc";
}