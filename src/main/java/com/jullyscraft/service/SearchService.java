package com.jullyscraft.service;

import com.jullyscraft.dto.request.SearchRequest;
import com.jullyscraft.dto.response.SearchResponse;
import com.jullyscraft.dto.response.SearchSuggestionResponse;
import com.jullyscraft.document.ProductDocument;

import java.util.List;

public interface SearchService {

    // ── Core search ───────────────────────────────────────────────────────────
    SearchResponse           search(SearchRequest request);
    SearchResponse           advancedSearch(SearchRequest request);
    SearchSuggestionResponse suggest(String prefix);

    // ── Trending ──────────────────────────────────────────────────────────────
    List<String>             getTrendingSearches();
    void                     recordSearch(String keyword);

    // ── Index management ──────────────────────────────────────────────────────
    void indexProduct(com.jullyscraft.entity.Product product);
    void removeProduct(Long productId);
    void reindexAll();
}