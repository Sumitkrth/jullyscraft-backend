package com.jullyscraft.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter @Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductResponse {
    private Long                         id;
    private String                       name;
    private String                       slug;
    private String                       shortDescription;
    private String                       fullDescription;
    private String                       brand;
    private String                       tags;
    private BigDecimal                   price;
    private BigDecimal                   discountPrice;
    private int                          discountPercent;
    private BigDecimal                   effectivePrice;
    private int                          stockQuantity;
    private int                          lowStockThreshold;
    private boolean                      inStock;
    private boolean                      lowStock;
    private boolean                      active;
    private boolean                      featured;
    private double                       averageRating;
    private int                          reviewCount;
    private String                       metaTitle;
    private String                       metaDescription;
    private Long                         categoryId;
    private String                       categoryName;
    private List<ProductImageResponse>   images;
    private List<ProductVariantResponse> variants;
    private List<Map<String, String>>    specifications;
    private LocalDateTime                createdAt;
    private LocalDateTime                updatedAt;
}