package com.jullyscraft.controller;

import com.jullyscraft.dto.response.ApiResponse;
import com.jullyscraft.dto.response.CategoryResponse;
import com.jullyscraft.dto.response.CategoryTreeResponse;
import com.jullyscraft.service.CategoryService;
import com.jullyscraft.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(AppConstants.CATEGORY_BASE)
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Public category browsing endpoints")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Get all active categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAllActiveFlat()));
    }

    @GetMapping("/tree")
    @Operation(summary = "Get full category tree (nested)")
    public ResponseEntity<ApiResponse<List<CategoryTreeResponse>>> getTree() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getCategoryTree()));
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getFeatured() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getFeaturedCategories()));
    }

    @GetMapping("/flat")
    @Operation(summary = "Get all active categories (flat list — for dropdowns)")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getFlat() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAllActiveFlat()));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get category by slug")
    public ResponseEntity<ApiResponse<CategoryResponse>> getBySlug(
            @PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getCategoryBySlug(slug)));
    }

    @GetMapping("/{parentId}/children")
    @Operation(summary = "Get subcategories of a parent category")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getChildren(
            @PathVariable Long parentId) {
        return ResponseEntity.ok(
                ApiResponse.success(categoryService.getChildrenByParentId(parentId)));
    }
}