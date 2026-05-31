package com.jullyscraft.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.jullyscraft.document.ProductDocument;
import com.jullyscraft.dto.response.ProductSummaryResponse;
import com.jullyscraft.dto.response.SearchSuggestionResponse;
import com.jullyscraft.entity.Product;
import com.jullyscraft.entity.ProductImage;
import com.jullyscraft.exception.BadRequestException;
import com.jullyscraft.repository.ProductRepository;
import com.jullyscraft.repository.search.ProductSearchRepository;
import com.jullyscraft.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    // ✅ Optional — null when Elasticsearch is disabled (free tier)
    @Autowired(required = false)
    private ProductSearchRepository searchRepository;

    @Autowired(required = false)
    private ElasticsearchClient esClient;

    // ✅ Required — always available
    private final ProductRepository             productRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.search.trending-ttl-hours:24}")
    private int trendingTtlHours;

    @Value("${app.search.trending-top-n:10}")
    private int trendingTopN;

    @Value("${app.search.autocomplete-max:8}")
    private int autocompleteMax;

    // Set to false in application-prod.yml via ELASTICSEARCH_ENABLED=false
    @Value("${app.elasticsearch.enabled:true}")
    private boolean elasticsearchEnabled;

    private static final String TRENDING_KEY = "search:trending";
    private static final String INDEX_NAME   = "products";

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    @Override
    public com.jullyscraft.dto.response.SearchResponse search(
            com.jullyscraft.dto.request.SearchRequest req) {

        long start = System.currentTimeMillis();

        // If ES is disabled (free tier) skip straight to DB
        if (!elasticsearchEnabled) {
            log.debug("Elasticsearch disabled — using DB search");
            return searchWithDatabase(req, start);
        }

        // Try ES, fall back to DB on any failure
        try {
            return searchWithElasticsearch(req, start);
        } catch (Exception e) {
            log.warn("Elasticsearch unavailable, falling back to DB search: {}", e.getMessage());
            return searchWithDatabase(req, start);
        }
    }

    @Override
    public com.jullyscraft.dto.response.SearchResponse advancedSearch(
            com.jullyscraft.dto.request.SearchRequest req) {
        // Same pipeline — advancedSearch uses richer filters via same path
        return search(req);
    }

    @Override
    public SearchSuggestionResponse suggest(String prefix) {
        if (prefix == null || prefix.length() < 2) {
            return SearchSuggestionResponse.builder()
                    .suggestions(List.of())
                    .trending(getTrendingSearches())
                    .topProducts(List.of())
                    .build();
        }

        // If ES disabled, return simple DB-based suggestions
        if (!elasticsearchEnabled) {
            return suggestFromDatabase(prefix);
        }

        try {
            List<String> suggestions = searchRepository
                    .autocomplete(prefix.toLowerCase(), PageRequest.of(0, autocompleteMax))
                    .stream()
                    .map(ProductDocument::getName)
                    .distinct()
                    .limit(autocompleteMax)
                    .toList();

            List<ProductSummaryResponse> topProducts = searchRepository
                    .autocomplete(prefix.toLowerCase(), PageRequest.of(0, 4))
                    .stream()
                    .map(this::toSummaryFromDocument)
                    .toList();

            return SearchSuggestionResponse.builder()
                    .suggestions(suggestions)
                    .trending(getTrendingSearches())
                    .topProducts(topProducts)
                    .build();

        } catch (Exception e) {
            log.warn("ES suggest failed, falling back to DB: {}", e.getMessage());
            return suggestFromDatabase(prefix);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getTrendingSearches() {
        try {
            Set<Object> trending = redisTemplate.opsForZSet()
                    .reverseRange(TRENDING_KEY, 0, trendingTopN - 1);
            if (trending == null) return List.of();
            return trending.stream().map(Object::toString).toList();
        } catch (Exception e) {
            log.warn("Failed to fetch trending searches: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    @Async("notificationExecutor")
    public void recordSearch(String keyword) {
        if (keyword == null || keyword.isBlank()) return;
        try {
            String normalized = keyword.toLowerCase().trim();
            redisTemplate.opsForZSet().incrementScore(TRENDING_KEY, normalized, 1);
            redisTemplate.expire(TRENDING_KEY, trendingTtlHours, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to record trending search: {}", e.getMessage());
        }
    }

    @Override
    @Async("notificationExecutor")
    public void indexProduct(Product product) {
        if (!elasticsearchEnabled) return;
        try {
            searchRepository.save(toDocument(product));
            log.debug("Product indexed: {}", product.getId());
        } catch (Exception e) {
            log.error("Failed to index product {}: {}", product.getId(), e.getMessage());
        }
    }

    @Override
    @Async("notificationExecutor")
    public void removeProduct(Long productId) {
        if (!elasticsearchEnabled) return;
        try {
            searchRepository.deleteById(productId.toString());
            log.debug("Product removed from index: {}", productId);
        } catch (Exception e) {
            log.error("Failed to remove product {} from index: {}", productId, e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void reindexAll() {
        if (!elasticsearchEnabled) {
            log.info("Elasticsearch disabled — skipping reindex");
            return;
        }
        log.info("Starting full product reindex...");
        long count = 0;
        int  page  = 0;
        while (true) {
            var products = productRepository.findByDeletedFalse(PageRequest.of(page++, 100));
            if (products.isEmpty()) break;
            List<ProductDocument> docs = products.getContent().stream()
                    .map(this::toDocument).toList();
            searchRepository.saveAll(docs);
            count += docs.size();
        }
        log.info("Reindex complete — {} products indexed", count);
    }

    // =========================================================================
    // ELASTICSEARCH SEARCH
    // =========================================================================

    private com.jullyscraft.dto.response.SearchResponse searchWithElasticsearch(
            com.jullyscraft.dto.request.SearchRequest req, long start) throws IOException {

        SearchResponse<ProductDocument> response =
                esClient.search(buildEsRequest(req), ProductDocument.class);

        List<ProductSummaryResponse> products = response.hits().hits()
                .stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .map(this::toSummaryFromDocument)
                .toList();

        long total = response.hits().total() != null
                ? response.hits().total().value() : 0;

        List<String>      brands = extractTermAgg(response, "brands");
        List<String>      sizes  = extractTermAgg(response, "sizes");
        List<String>      colors = extractTermAgg(response, "colors");
        Map<String, Long> cats   = extractTermAggWithCount(response, "categories");
        double minP = extractMinAgg(response, "min_price");
        double maxP = extractMaxAgg(response, "max_price");

        if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
            recordSearch(req.getKeyword());
        }

        return com.jullyscraft.dto.response.SearchResponse.builder()
                .products(products)
                .totalElements(total)
                .totalPages((int) Math.ceil((double) total / req.getSize()))
                .pageNumber(req.getPage())
                .pageSize(req.getSize())
                .last(req.getPage() >= (int) Math.ceil((double) total / req.getSize()) - 1)
                .availableBrands(brands)
                .availableSizes(sizes)
                .availableColors(colors)
                .categoryCounts(cats)
                .minAvailablePrice(minP)
                .maxAvailablePrice(maxP)
                .searchTimeMs(System.currentTimeMillis() - start)
                .build();
    }

    // =========================================================================
    // DATABASE FALLBACK SEARCH
    // =========================================================================

    private com.jullyscraft.dto.response.SearchResponse searchWithDatabase(
            com.jullyscraft.dto.request.SearchRequest req, long start) {

        Pageable pageable = PageRequest.of(req.getPage(), req.getSize());
        Page<Product> page;

        String keyword = req.getKeyword();
        if (keyword != null && !keyword.isBlank()) {
            // Search name and brand (case-insensitive)
            page = productRepository
                    .findByDeletedFalseAndNameContainingIgnoreCaseOrDeletedFalseAndBrandContainingIgnoreCase(
                            keyword, keyword, pageable);
        } else {
            page = productRepository.findByDeletedFalse(pageable);
        }

        List<ProductSummaryResponse> products = page.getContent()
                .stream()
                .map(this::toSummaryFromEntity)
                .toList();

        if (keyword != null && !keyword.isBlank()) {
            recordSearch(keyword);
        }

        return com.jullyscraft.dto.response.SearchResponse.builder()
                .products(products)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .pageNumber(req.getPage())
                .pageSize(req.getSize())
                .last(page.isLast())
                // Aggregations not available from DB — return empty
                .availableBrands(List.of())
                .availableSizes(List.of())
                .availableColors(List.of())
                .categoryCounts(Map.of())
                .minAvailablePrice(0.0)
                .maxAvailablePrice(0.0)
                .searchTimeMs(System.currentTimeMillis() - start)
                .build();
    }

    // =========================================================================
    // DATABASE FALLBACK SUGGEST
    // =========================================================================

    private SearchSuggestionResponse suggestFromDatabase(String prefix) {
        Pageable pageable = PageRequest.of(0, autocompleteMax);
        Page<Product> page = productRepository
                .findByDeletedFalseAndNameContainingIgnoreCaseOrDeletedFalseAndBrandContainingIgnoreCase(
                        prefix, prefix, pageable);

        List<String> suggestions = page.getContent().stream()
                .map(Product::getName)
                .distinct()
                .limit(autocompleteMax)
                .toList();

        List<ProductSummaryResponse> topProducts = page.getContent().stream()
                .limit(4)
                .map(this::toSummaryFromEntity)
                .toList();

        return SearchSuggestionResponse.builder()
                .suggestions(suggestions)
                .trending(getTrendingSearches())
                .topProducts(topProducts)
                .build();
    }

    // =========================================================================
    // ELASTICSEARCH REQUEST BUILDER
    // =========================================================================

    private SearchRequest buildEsRequest(com.jullyscraft.dto.request.SearchRequest req) {
        return SearchRequest.of(s -> {
            s.index(INDEX_NAME);
            s.from(req.getPage() * req.getSize());
            s.size(req.getSize());

            s.query(q -> q.bool(b -> {

                // Always filter active, non-deleted products
                b.filter(f -> f.term(t -> t.field("active").value(true)));

                // Keyword — multi-match with fuzziness
                if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
                    b.must(m -> m.multiMatch(mm -> mm
                            .query(req.getKeyword())
                            .fields("name^4", "name.ngram^2",
                                    "brand^3", "shortDescription",
                                    "tags^2", "categoryName")
                            .fuzziness("AUTO")
                            .operator(Operator.Or)));
                }

                // Category
                if (req.getCategoryId() != null) {
                    b.filter(f -> f.term(t ->
                            t.field("categoryId").value(FieldValue.of(req.getCategoryId()))));
                }

                // Brand
                if (req.getBrand() != null && !req.getBrand().isBlank()) {
                    b.filter(f -> f.term(t ->
                            t.field("brand.keyword").value(req.getBrand())));
                }

                // Price range
                if (req.getMinPrice() != null || req.getMaxPrice() != null) {
                    b.filter(f -> f.range(r -> {
                        r.field("effectivePrice");
                        if (req.getMinPrice() != null)
                            r.gte(co.elastic.clients.json.JsonData.of(req.getMinPrice().doubleValue()));
                        if (req.getMaxPrice() != null)
                            r.lte(co.elastic.clients.json.JsonData.of(req.getMaxPrice().doubleValue()));
                        return r;
                    }));
                }

                // Min rating
                if (req.getMinRating() != null) {
                    b.filter(f -> f.range(r -> r
                            .field("averageRating")
                            .gte(co.elastic.clients.json.JsonData.of(req.getMinRating()))));
                }

                // In stock
                if (Boolean.TRUE.equals(req.getInStock())) {
                    b.filter(f -> f.term(t -> t.field("inStock").value(true)));
                }

                // Featured
                if (req.getFeatured() != null) {
                    b.filter(f -> f.term(t -> t.field("featured").value(req.getFeatured())));
                }

                // Sizes
                if (req.getSizes() != null && !req.getSizes().isEmpty()) {
                    b.filter(f -> f.terms(t -> t
                            .field("sizes")
                            .terms(tv -> tv.value(
                                    req.getSizes().stream().map(FieldValue::of).toList()))));
                }

                // Colors
                if (req.getColors() != null && !req.getColors().isEmpty()) {
                    b.filter(f -> f.terms(t -> t
                            .field("colors")
                            .terms(tv -> tv.value(
                                    req.getColors().stream().map(FieldValue::of).toList()))));
                }

                return b;
            }));

            // Sort
            switch (req.getSortBy()) {
                case "price"  -> s.sort(so -> so.field(f -> f
                        .field("effectivePrice")
                        .order("asc".equals(req.getSortDir()) ? SortOrder.Asc : SortOrder.Desc)));
                case "rating" -> s.sort(so -> so.field(f -> f
                        .field("averageRating").order(SortOrder.Desc)));
                case "newest" -> s.sort(so -> so.field(f -> f
                        .field("createdAt").order(SortOrder.Desc)));
                // default = ES relevance score
            }

            // Aggregations (for sidebar filters)
            s.aggregations("brands",     Aggregation.of(a -> a.terms(t -> t.field("brand.keyword").size(50))));
            s.aggregations("sizes",      Aggregation.of(a -> a.terms(t -> t.field("sizes").size(30))));
            s.aggregations("colors",     Aggregation.of(a -> a.terms(t -> t.field("colors").size(30))));
            s.aggregations("categories", Aggregation.of(a -> a.terms(t -> t.field("categoryName").size(20))));
            s.aggregations("min_price",  Aggregation.of(a -> a.min(m -> m.field("effectivePrice"))));
            s.aggregations("max_price",  Aggregation.of(a -> a.max(m -> m.field("effectivePrice"))));

            return s;
        });
    }

    // =========================================================================
    // AGGREGATION EXTRACTORS
    // =========================================================================

    private List<String> extractTermAgg(SearchResponse<ProductDocument> resp, String aggName) {
        try {
            return resp.aggregations().get(aggName)
                    .sterms().buckets().array()
                    .stream()
                    .map(b -> b.key().stringValue())
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private Map<String, Long> extractTermAggWithCount(SearchResponse<ProductDocument> resp, String aggName) {
        try {
            return resp.aggregations().get(aggName)
                    .sterms().buckets().array()
                    .stream()
                    .collect(Collectors.toMap(
                            b -> b.key().stringValue(),
                            b -> b.docCount(),
                            (a, b) -> a,
                            LinkedHashMap::new));
        } catch (Exception e) {
            return Map.of();
        }
    }

    private double extractMinAgg(SearchResponse<ProductDocument> resp, String aggName) {
        try {
            Double val = resp.aggregations().get(aggName).min().value();
            return val != null ? val : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double extractMaxAgg(SearchResponse<ProductDocument> resp, String aggName) {
        try {
            Double val = resp.aggregations().get(aggName).max().value();
            return val != null ? val : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    // =========================================================================
    // MAPPERS
    // =========================================================================

    /** Product entity → ProductDocument (for ES indexing) */
    public ProductDocument toDocument(Product p) {
        String primaryImage = p.getImages().stream()
                .filter(ProductImage::isPrimaryImage)
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElse(p.getImages().isEmpty() ? null : p.getImages().get(0).getImageUrl());

        List<String> sizes = p.getVariants().stream()
                .filter(v -> !v.isDeleted() && v.getSize() != null)
                .map(v -> v.getSize()).distinct().toList();

        List<String> colors = p.getVariants().stream()
                .filter(v -> !v.isDeleted() && v.getColor() != null)
                .map(v -> v.getColor()).distinct().toList();

        BigDecimal effective = p.getDiscountPrice() != null
                ? p.getDiscountPrice() : p.getPrice();

        int discountPct = 0;
        if (p.getDiscountPrice() != null && p.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            discountPct = p.getPrice().subtract(p.getDiscountPrice())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(p.getPrice(), 0, java.math.RoundingMode.HALF_UP)
                    .intValue();
        }

        return ProductDocument.builder()
                .id(p.getId().toString())
                .name(p.getName())
                .shortDescription(p.getShortDescription())
                .fullDescription(p.getFullDescription())
                .brand(p.getBrand())
                .tags(p.getTags())
                .categoryId(p.getCategory().getId())
                .categoryName(p.getCategory().getName())
                .categorySlug(p.getCategory().getSlug())
                .price(p.getPrice())
                .discountPrice(p.getDiscountPrice())
                .effectivePrice(effective)
                .discountPercent(discountPct)
                .stockQuantity(p.getStockQuantity())
                .inStock(p.isInStock())
                .active(p.isActive())
                .featured(p.isFeatured())
                .averageRating(p.getAverageRating())
                .reviewCount(p.getReviewCount())
                .primaryImageUrl(primaryImage)
                .sizes(sizes)
                .colors(colors)
                .slug(p.getSlug())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    /** ProductDocument → ProductSummaryResponse (ES results) */
    private ProductSummaryResponse toSummaryFromDocument(ProductDocument doc) {
        ProductSummaryResponse res = new ProductSummaryResponse();
        res.setId(Long.parseLong(doc.getId()));
        res.setName(doc.getName());
        res.setSlug(doc.getSlug());
        res.setBrand(doc.getBrand());
        res.setPrice(doc.getPrice());
        res.setDiscountPrice(doc.getDiscountPrice());
        res.setDiscountPercent(doc.getDiscountPercent());
        res.setPrimaryImageUrl(doc.getPrimaryImageUrl());
        res.setAverageRating(doc.getAverageRating());
        res.setReviewCount(doc.getReviewCount());
        res.setInStock(doc.isInStock());
        res.setFeatured(doc.isFeatured());
        res.setCategoryName(doc.getCategoryName());
        return res;
    }

    /** Product entity → ProductSummaryResponse (DB fallback results) */
    private ProductSummaryResponse toSummaryFromEntity(Product p) {
        String primaryImage = p.getImages().stream()
                .filter(ProductImage::isPrimaryImage)
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElse(p.getImages().isEmpty() ? null : p.getImages().get(0).getImageUrl());

        int discountPct = 0;
        if (p.getDiscountPrice() != null && p.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            discountPct = p.getPrice().subtract(p.getDiscountPrice())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(p.getPrice(), 0, java.math.RoundingMode.HALF_UP)
                    .intValue();
        }

        ProductSummaryResponse res = new ProductSummaryResponse();
        res.setId(p.getId());
        res.setName(p.getName());
        res.setSlug(p.getSlug());
        res.setBrand(p.getBrand());
        res.setPrice(p.getPrice());
        res.setDiscountPrice(p.getDiscountPrice());
        res.setDiscountPercent(discountPct);
        res.setPrimaryImageUrl(primaryImage);
        res.setAverageRating(p.getAverageRating());
        res.setReviewCount(p.getReviewCount());
        res.setInStock(p.isInStock());
        res.setFeatured(p.isFeatured());
        res.setCategoryName(p.getCategory().getName());
        return res;
    }
}