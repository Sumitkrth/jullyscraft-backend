package com.jullyscraft.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecommendationResponse {

    private String                       reason;       // why these are recommended
    private String                       strategy;     // SIMILAR | BOUGHT_TOGETHER | TRENDING | PERSONALIZED | AI
    private List<ProductSummaryResponse> products;
    private int                          totalCount;
}