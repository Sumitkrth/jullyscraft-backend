package com.jullyscraft.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SearchSuggestionResponse {
    private List<String> suggestions;      // autocomplete terms
    private List<String> trending;         // trending searches
    private List<ProductSummaryResponse> topProducts;  // quick result preview
}