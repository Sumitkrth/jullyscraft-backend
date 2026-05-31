package com.jullyscraft.service;

import com.jullyscraft.dto.request.ProductFilterRequest;
import com.jullyscraft.dto.request.ProductRequest;
import com.jullyscraft.dto.response.PageResponse;
import com.jullyscraft.dto.response.ProductResponse;
import com.jullyscraft.dto.response.ProductSummaryResponse;

public interface ProductService {

    // ── Public ────────────────────────────────────────────────────────────────
    PageResponse<ProductSummaryResponse> getProducts(ProductFilterRequest filter);
    PageResponse<ProductSummaryResponse> getFeaturedProducts(int page, int size);
    PageResponse<ProductSummaryResponse> getProductsByCategory(Long categoryId, int page, int size);
    PageResponse<ProductSummaryResponse> searchProducts(String q, int page, int size);
    ProductResponse                      getProductBySlug(String slug);
    ProductResponse                      getProductById(Long id);

    // ── Admin ─────────────────────────────────────────────────────────────────
    PageResponse<ProductSummaryResponse> getAllProductsAdmin(int page, int size, String sortBy, String sortDir);
    ProductResponse                      createProduct(ProductRequest request);
    ProductResponse                      updateProduct(Long id, ProductRequest request);
    void                                 deleteProduct(Long id);
    void                                 restoreProduct(Long id);
    ProductResponse                      toggleActive(Long id);
    ProductResponse                      toggleFeatured(Long id);
}