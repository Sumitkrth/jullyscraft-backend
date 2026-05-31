package com.jullyscraft.controller;

import com.jullyscraft.dto.request.ProductRequest;
import com.jullyscraft.dto.response.*;
import com.jullyscraft.service.FileStorageService;
import com.jullyscraft.service.ProductService;
import com.jullyscraft.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping(AppConstants.ADMIN_BASE + "/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin — Products", description = "Full product lifecycle management")
public class AdminProductController {

    private final ProductService productService;
    private final FileStorageService fileStorageService;

    @GetMapping
    @Operation(summary = "Get all products (paginated, including inactive)")
    public ResponseEntity<ApiResponse<PageResponse<ProductSummaryResponse>>> getAll(
            @RequestParam(defaultValue = "0")         int    page,
            @RequestParam(defaultValue = "20")        int    size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String sortDir) {
        return ResponseEntity.ok(ApiResponse.success(
                productService.getAllProductsAdmin(page, size, sortBy, sortDir)));
    }

    @PostMapping
    @Operation(summary = "Create a new product")
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Product created successfully",
                        productService.createProduct(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a product")
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Product updated successfully",
                productService.updateProduct(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a product")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully"));
    }

    @PatchMapping("/{id}/restore")
    @Operation(summary = "Restore a soft-deleted product")
    public ResponseEntity<ApiResponse<Void>> restore(@PathVariable Long id) {
        productService.restoreProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product restored successfully"));
    }

    @PatchMapping("/{id}/toggle-active")
    @Operation(summary = "Toggle product active/inactive")
    public ResponseEntity<ApiResponse<ProductResponse>> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Product status updated", productService.toggleActive(id)));
    }

    @PatchMapping("/{id}/toggle-featured")
    @Operation(summary = "Toggle product featured status")
    public ResponseEntity<ApiResponse<ProductResponse>> toggleFeatured(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Product featured status updated", productService.toggleFeatured(id)));
    }

    // Add to AdminProductController
    @PostMapping(value = "/{id}/images",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload product images to Cloudinary")
    public ResponseEntity<ApiResponse<MultiFileUploadResponse>> uploadImages(
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files) {
        return ResponseEntity.ok(ApiResponse.success(
                "Images uploaded successfully",
                fileStorageService.uploadProductImages(files, id)));
    }
}