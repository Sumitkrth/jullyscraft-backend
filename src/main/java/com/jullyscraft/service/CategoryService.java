package com.jullyscraft.service;

import com.jullyscraft.dto.request.CategoryRequest;
import com.jullyscraft.dto.response.CategoryResponse;
import com.jullyscraft.dto.response.CategoryTreeResponse;

import java.util.List;

public interface CategoryService {

    // ── Public ────────────────────────────────────────────────────────────────
    List<CategoryTreeResponse> getCategoryTree();
    List<CategoryResponse>     getFeaturedCategories();
    List<CategoryResponse>     getAllActiveFlat();
    CategoryResponse           getCategoryBySlug(String slug);
    List<CategoryResponse>     getChildrenByParentId(Long parentId);

    // ── Admin ─────────────────────────────────────────────────────────────────
    List<CategoryResponse>     getAllCategories();
    CategoryResponse           getCategoryById(Long id);
    CategoryResponse           createCategory(CategoryRequest request);
    CategoryResponse           updateCategory(Long id, CategoryRequest request);
    void                       deleteCategory(Long id);
    CategoryResponse           toggleActive(Long id);
    CategoryResponse           toggleFeatured(Long id);
}