package com.jullyscraft.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class SearchResponse {
    private List<ProductSummaryResponse> products;
    private long                          totalElements;
    private int                           totalPages;
    private int                           pageNumber;
    private int                           pageSize;
    private boolean                       last;

    // Aggregations for sidebar filters
    private List<String>       availableBrands;
    private List<String>       availableSizes;
    private List<String>       availableColors;
    private Map<String, Long>  categoryCounts;    // category name → count
    private Double             minAvailablePrice;
    private Double             maxAvailablePrice;

    private String             correctedQuery;    // did-you-mean suggestion
    private long               searchTimeMs;
}