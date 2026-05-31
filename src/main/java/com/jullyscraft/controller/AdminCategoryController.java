package com.jullyscraft.controller;

import com.jullyscraft.dto.request.CategoryRequest;
import com.jullyscraft.dto.response.ApiResponse;
import com.jullyscraft.dto.response.CategoryResponse;
import com.jullyscraft.service.CategoryService;
import com.jullyscraft.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(AppConstants.ADMIN_BASE + "/categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin — Categories", description = "Admin category management")
public class AdminCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Get all categories (including inactive)")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAllCategories()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID")
    public ResponseEntity<ApiResponse<CategoryResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getCategoryById(id)));
    }

    @PostMapping
    @Operation(summary = "Create a new category")
    public ResponseEntity<ApiResponse<CategoryResponse>> create(
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Category created successfully",
                        categoryService.createCategory(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a category")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Category updated successfully",
                categoryService.updateCategory(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a category")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Category deleted successfully"));
    }

    @PatchMapping("/{id}/toggle-active")
    @Operation(summary = "Toggle category active/inactive")
    public ResponseEntity<ApiResponse<CategoryResponse>> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Category status updated",
                categoryService.toggleActive(id)));
    }

    @PatchMapping("/{id}/toggle-featured")
    @Operation(summary = "Toggle category featured status")
    public ResponseEntity<ApiResponse<CategoryResponse>> toggleFeatured(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Category featured status updated",
                categoryService.toggleFeatured(id)));
    }
}