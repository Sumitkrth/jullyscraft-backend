package com.jullyscraft.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
public class SearchRequest {

    private String        keyword;
    private Long          categoryId;
    private String        brand;
    private BigDecimal    minPrice;
    private BigDecimal    maxPrice;
    private Double        minRating;
    private Boolean       inStock;
    private Boolean       featured;
    private List<String>  sizes;
    private List<String>  colors;

    private int    page    = 0;
    private int    size    = 12;
    private String sortBy  = "relevance";   // relevance | price | rating | newest
    private String sortDir = "desc";
}