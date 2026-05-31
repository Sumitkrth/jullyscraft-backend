package com.jullyscraft.service.impl;

import com.jullyscraft.dto.response.ProductSummaryResponse;
import com.jullyscraft.dto.response.RecommendationResponse;
import com.jullyscraft.entity.Product;
import com.jullyscraft.entity.UserProductInteraction;
import com.jullyscraft.mapper.ProductMapper;
import com.jullyscraft.repository.ProductRepository;
import com.jullyscraft.repository.UserProductInteractionRepository;
import com.jullyscraft.service.RecommendationService;
import com.jullyscraft.util.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final ProductRepository                   productRepository;
    private final UserProductInteractionRepository    interactionRepository;
    private final ProductMapper                       productMapper;
    private final AiRecommendationEngine              aiEngine;
    private final RedisTemplate<String, Object>       redisTemplate;

    private static final String TRENDING_CACHE = "recommendations:trending";
    private static final int    CANDIDATE_POOL = 100;

    // ── Similar products (content-based — same category) ─────────────────────

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "recommendations",
            key = "'similar:' + #productId + ':' + #limit")
    public RecommendationResponse getSimilarProducts(Long productId, int limit) {
        Product seed = findProduct(productId);

        List<Product> similar = productRepository
                .findByCategoryIdAndActiveTrueAndDeletedFalse(
                        seed.getCategory().getId(),
                        PageRequest.of(0, limit + 1))
                .getContent()
                .stream()
                .filter(p -> !p.getId().equals(productId))
                .limit(limit)
                .toList();

        return buildResponse(similar, "SIMILAR",
                "Similar products in " + seed.getCategory().getName());
    }

    // ── Frequently bought together ────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "recommendations",
            key = "'fbt:' + #productId + ':' + #limit")
    public RecommendationResponse getFrequentlyBoughtTogether(
            Long productId, int limit) {

        List<Object[]> rows = interactionRepository
                .findFrequentlyBoughtTogether(
                        productId, PageRequest.of(0, limit));

        List<Long> ids = rows.stream()
                .map(r -> ((Number) r[0]).longValue())
                .toList();

        List<Product> products = fetchProductsOrdered(ids, limit);

        // Fallback to similar if no co-purchase data
        if (products.isEmpty()) {
            return getSimilarProducts(productId, limit);
        }

        return buildResponse(products, "BOUGHT_TOGETHER",
                "Frequently bought together");
    }

    // ── Collaborative filtering ───────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "recommendations",
            key = "'collab:' + #productId + ':' + #limit")
    public RecommendationResponse getCollaborativeRecommendations(
            Long productId, int limit) {

        List<Object[]> rows = interactionRepository
                .findCollaborativeProducts(
                        productId, PageRequest.of(0, limit));

        List<Long> ids = rows.stream()
                .map(r -> ((Number) r[0]).longValue())
                .toList();

        List<Product> products = fetchProductsOrdered(ids, limit);

        if (products.isEmpty()) {
            return getSimilarProducts(productId, limit);
        }

        return buildResponse(products, "COLLABORATIVE",
                "Customers who bought this also bought");
    }

    // ── Personalized (user-based) ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public RecommendationResponse getPersonalizedRecommendations(
            Long userId, int limit) {

        // 1. Get user's top interacted categories
        List<Object[]> catAffinity = interactionRepository
                .findCategoryAffinityByUser(userId, PageRequest.of(0, 3));

        if (catAffinity.isEmpty()) {
            return getTrendingProducts(limit);
        }

        // 2. Collect products from affinity categories
        List<Long> alreadySeen = interactionRepository
                .findProductIdsByUserAndType(
                        userId,
                        UserProductInteraction.InteractionType.PURCHASE,
                        PageRequest.of(0, 200));

        List<Product> candidates = new ArrayList<>();
        for (Object[] row : catAffinity) {
            Long catId = ((Number) row[0]).longValue();
            candidates.addAll(
                    productRepository.findByCategoryIdAndActiveTrueAndDeletedFalse(
                            catId, PageRequest.of(0, 30)).getContent());
        }

        // Remove already purchased / interacted
        List<Product> filtered = candidates.stream()
                .filter(p -> !alreadySeen.contains(p.getId()))
                .distinct()
                .limit(limit)
                .toList();

        return buildResponse(filtered, "PERSONALIZED",
                "Recommended for you");
    }

    // ── Recently viewed ───────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public RecommendationResponse getRecentlyViewedRecommendations(
            Long userId, int limit) {

        List<Long> viewedIds = interactionRepository
                .findProductIdsByUserAndType(
                        userId,
                        UserProductInteraction.InteractionType.VIEW,
                        PageRequest.of(0, limit));

        List<Product> products = fetchProductsOrdered(viewedIds, limit);

        return buildResponse(products, "RECENTLY_VIEWED",
                "Recently viewed");
    }

    // ── Trending ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "recommendations", key = "'trending:' + #limit")
    public RecommendationResponse getTrendingProducts(int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(7);

        List<Object[]> rows = interactionRepository
                .findTrendingProductIds(since, PageRequest.of(0, limit));

        List<Long> ids = rows.stream()
                .map(r -> ((Number) r[0]).longValue())
                .toList();

        List<Product> products = fetchProductsOrdered(ids, limit);

        // Fallback to featured if no interaction data
        if (products.isEmpty()) {
            products = productRepository
                    .findByFeaturedTrueAndActiveTrueAndDeletedFalse(
                            PageRequest.of(0, limit)).getContent();
        }

        return buildResponse(products, "TRENDING", "Trending this week");
    }

    // ── New arrivals ──────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "recommendations", key = "'new-arrivals:' + #limit")
    public RecommendationResponse getNewArrivals(int limit) {
        List<Product> products = productRepository
                .findByActiveTrueAndDeletedFalse(
                        PageRequest.of(0, limit,
                                org.springframework.data.domain.Sort
                                        .by("createdAt").descending()))
                .getContent();

        return buildResponse(products, "NEW_ARRIVALS", "New arrivals");
    }

    // ── AI recommendations ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public RecommendationResponse getAiRecommendations(Long userId,
                                                       Long productId,
                                                       int limit) {
        Product seed = productId != null ? findProduct(productId) : null;

        // Build candidate pool
        List<Product> candidates;
        if (seed != null) {
            candidates = productRepository
                    .findByCategoryIdAndActiveTrueAndDeletedFalse(
                            seed.getCategory().getId(),
                            PageRequest.of(0, CANDIDATE_POOL))
                    .getContent();
        } else {
            candidates = productRepository
                    .findByActiveTrueAndDeletedFalse(
                            PageRequest.of(0, CANDIDATE_POOL))
                    .getContent();
        }

        // Build user context string
        String userContext = buildUserContext(userId);

        // Ask Claude to rank
        List<Long> aiIds = aiEngine.recommend(
                seed != null ? seed : candidates.get(0),
                candidates, userContext, limit);

        List<Product> aiProducts = fetchProductsOrdered(aiIds, limit);

        // Fallback to collaborative if AI fails
        if (aiProducts.isEmpty() && productId != null) {
            return getCollaborativeRecommendations(productId, limit);
        }
        if (aiProducts.isEmpty()) {
            return getTrendingProducts(limit);
        }

        return buildResponse(aiProducts, "AI",
                "Recommended by AI");
    }

    // ── Interaction tracking ──────────────────────────────────────────────────

    @Override
    @Async("notificationExecutor")
    @Transactional
    public void recordInteraction(Long userId, Long productId,
                                  UserProductInteraction.InteractionType type,
                                  String sessionId) {
        try {
            Product product = productRepository.findById(productId).orElse(null);
            if (product == null) return;

            int score = switch (type) {
                case VIEW      -> 1;
                case WISHLIST  -> 2;
                case CART_ADD  -> 3;
                case REVIEW    -> 4;
                case PURCHASE  -> 5;
            };

            UserProductInteraction interaction = UserProductInteraction.builder()
                    .product(product)
                    .type(type)
                    .score(score)
                    .sessionId(sessionId)
                    .build();

            if (userId != null) {
                interaction.setUser(
                        com.jullyscraft.entity.User.builder()
                                .build());
                // Minimal user reference — JPA will resolve
            }

            interactionRepository.save(interaction);

            // Update trending score in Redis
            String key = "interactions:trending";
            redisTemplate.opsForZSet()
                    .incrementScore(key, productId.toString(), score);
            redisTemplate.expire(key, 7, TimeUnit.DAYS);

        } catch (Exception e) {
            log.warn("Interaction recording failed for product {}: {}",
                    productId, e.getMessage());
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .filter(p -> !p.isDeleted() && p.isActive())
                .orElseThrow(() ->
                        new com.jullyscraft.exception.ResourceNotFoundException(
                                "Product", "id", productId));
    }

    private List<Product> fetchProductsOrdered(List<Long> ids, int limit) {
        if (ids.isEmpty()) return List.of();

        Map<Long, Product> productMap = productRepository
                .findAllById(ids).stream()
                .filter(p -> !p.isDeleted() && p.isActive())
                .collect(Collectors.toMap(Product::getId, p -> p));

        // Preserve order from IDs list
        return ids.stream()
                .map(productMap::get)
                .filter(Objects::nonNull)
                .limit(limit)
                .toList();
    }

    private RecommendationResponse buildResponse(List<Product> products,
                                                 String strategy,
                                                 String reason) {
        List<ProductSummaryResponse> summaries = products.stream()
                .map(productMapper::toSummary)
                .toList();

        return RecommendationResponse.builder()
                .strategy(strategy)
                .reason(reason)
                .products(summaries)
                .totalCount(summaries.size())
                .build();
    }

    private String buildUserContext(Long userId) {
        if (userId == null) return null;
        try {
            List<Object[]> affinities = interactionRepository
                    .findCategoryAffinityByUser(userId, PageRequest.of(0, 3));

            if (affinities.isEmpty()) return null;

            StringBuilder sb = new StringBuilder("User prefers categories: ");
            affinities.forEach(row ->
                    sb.append("catId=").append(row[0]).append(" "));
            return sb.toString().trim();
        } catch (Exception e) {
            return null;
        }
    }
}