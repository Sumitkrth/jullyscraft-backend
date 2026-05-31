package com.jullyscraft.controller;

import com.jullyscraft.dto.request.SearchRequest;
import com.jullyscraft.dto.response.ApiResponse;
import com.jullyscraft.dto.response.SearchResponse;
import com.jullyscraft.dto.response.SearchSuggestionResponse;
import com.jullyscraft.service.SearchService;
import com.jullyscraft.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(AppConstants.API_BASE + "/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Product search, autocomplete, and trending")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    @Operation(summary = "Full-featured product search with filters + aggregations")
    public ResponseEntity<ApiResponse<SearchResponse>> search(
            SearchRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(searchService.search(request)));
    }

    @GetMapping("/suggest")
    @Operation(summary = "Autocomplete suggestions + trending searches")
    public ResponseEntity<ApiResponse<SearchSuggestionResponse>> suggest(
            @RequestParam(defaultValue = "") String q) {
        return ResponseEntity.ok(
                ApiResponse.success(searchService.suggest(q)));
    }

    @GetMapping("/trending")
    @Operation(summary = "Get top trending search keywords")
    public ResponseEntity<ApiResponse<List<String>>> trending() {
        return ResponseEntity.ok(
                ApiResponse.success(searchService.getTrendingSearches()));
    }

    @PostMapping("/admin/reindex")
    @Operation(summary = "Reindex all products into Elasticsearch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> reindex() {
        searchService.reindexAll();
        return ResponseEntity.ok(
                ApiResponse.success("Reindex triggered successfully"));
    }
}