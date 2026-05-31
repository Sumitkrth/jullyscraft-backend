package com.jullyscraft.controller;

import com.jullyscraft.dto.request.ProductFilterRequest;
import com.jullyscraft.dto.response.ApiResponse;
import com.jullyscraft.dto.response.PageResponse;
import com.jullyscraft.dto.response.ProductResponse;
import com.jullyscraft.dto.response.ProductSummaryResponse;
import com.jullyscraft.entity.AnalyticsEvent;
import com.jullyscraft.entity.UserProductInteraction;
import com.jullyscraft.security.userdetails.UserPrincipal;
import com.jullyscraft.service.AnalyticsService;
import com.jullyscraft.service.ProductService;
import com.jullyscraft.service.RecommendationService;
import com.jullyscraft.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(AppConstants.PRODUCT_BASE)
@RequiredArgsConstructor
@Tag(name = "Products", description = "Public product browsing & search")
public class ProductController {

    private final ProductService productService;
    private final AnalyticsService analyticsService;
    private final RecommendationService recommendationService;

    @GetMapping
    @Operation(summary = "Browse products with filters, pagination, sorting")
    public ResponseEntity<ApiResponse<PageResponse<ProductSummaryResponse>>> getProducts(
            ProductFilterRequest filter) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProducts(filter)));
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured products")
    public ResponseEntity<ApiResponse<PageResponse<ProductSummaryResponse>>> getFeatured(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(
                ApiResponse.success(productService.getFeaturedProducts(page, size)));
    }

    @GetMapping("/search")
    @Operation(summary = "Search products by keyword")
    public ResponseEntity<ApiResponse<PageResponse<ProductSummaryResponse>>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(
                ApiResponse.success(productService.searchProducts(q, page, size)));
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get products by category")
    public ResponseEntity<ApiResponse<PageResponse<ProductSummaryResponse>>> getByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                productService.getProductsByCategory(categoryId, page, size)));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get full product details by slug")
    public ResponseEntity<ApiResponse<ProductResponse>> getBySlug(
            @PathVariable String slug,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {

        ProductResponse product = productService.getProductBySlug(slug);

        analyticsService.track(
                principal != null ? principal.getId() : null,
                request.getSession().getId(),
                AnalyticsEvent.EventType.PRODUCT_VIEW,
                product.getId(), null,
                request.getHeader("User-Agent") != null
                        && request.getHeader("User-Agent").contains("Mobile")
                        ? "mobile" : "desktop",
                null);

        recommendationService.recordInteraction(
                principal != null ? principal.getId() : null,
                product.getId(),
                UserProductInteraction.InteractionType.VIEW,
                null);

        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @GetMapping("/id/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ApiResponse<ProductResponse>> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        ProductResponse product = productService.getProductById(id);

        recommendationService.recordInteraction(
                principal != null ? principal.getId() : null,
                product.getId(),
                UserProductInteraction.InteractionType.VIEW,
                null);

        return ResponseEntity.ok(ApiResponse.success(product));
    }
}