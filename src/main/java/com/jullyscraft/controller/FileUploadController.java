package com.jullyscraft.controller;

import com.jullyscraft.dto.response.ApiResponse;
import com.jullyscraft.dto.response.FileUploadResponse;
import com.jullyscraft.dto.response.MultiFileUploadResponse;
import com.jullyscraft.security.userdetails.UserPrincipal;
import com.jullyscraft.service.FileStorageService;
import com.jullyscraft.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping(AppConstants.API_BASE + "/files")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "File Upload", description = "Cloudinary-backed file upload endpoints")
public class FileUploadController {

    private final FileStorageService fileStorageService;

    // ── Profile ───────────────────────────────────────────────────────────────

    @PatchMapping(value = "/profile-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload profile image")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadProfileImage(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(
                "Profile image uploaded",
                fileStorageService.uploadProfileImage(file, principal.getId())));
    }

    // ── Products (Admin only) ─────────────────────────────────────────────────

    @PostMapping(value = "/products/{productId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upload a single product image")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadProductImage(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(
                "Product image uploaded",
                fileStorageService.uploadProductImage(file, productId)));
    }

    @PostMapping(value = "/products/{productId}/bulk",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upload multiple product images (max 10)")
    public ResponseEntity<ApiResponse<MultiFileUploadResponse>> uploadProductImages(
            @PathVariable Long productId,
            @RequestParam("files") List<MultipartFile> files) {
        return ResponseEntity.ok(ApiResponse.success(
                "Product images uploaded",
                fileStorageService.uploadProductImages(files, productId)));
    }

    // ── Categories (Admin only) ───────────────────────────────────────────────

    @PostMapping(value = "/categories/{categoryId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upload category image")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadCategoryImage(
            @PathVariable Long categoryId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(
                "Category image uploaded",
                fileStorageService.uploadCategoryImage(file, categoryId)));
    }

    // ── Delete (Admin only) ───────────────────────────────────────────────────

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a file from Cloudinary by public ID")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @RequestParam String publicId) {
        fileStorageService.deleteFile(publicId);
        return ResponseEntity.ok(ApiResponse.success("File deleted successfully"));
    }

    // ── URL helpers ───────────────────────────────────────────────────────────────

    @GetMapping("/url/thumbnail")
    @Operation(summary = "Get thumbnail URL for a Cloudinary public ID")
    public ResponseEntity<ApiResponse<String>> getThumbnailUrl(
            @RequestParam String publicId) {
        String url = fileStorageService.getThumbnailUrl(publicId);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .message("Success")
                .data(url)
                .build());
    }

    @GetMapping("/url/medium")
    @Operation(summary = "Get medium URL for a Cloudinary public ID")
    public ResponseEntity<ApiResponse<String>> getMediumUrl(
            @RequestParam String publicId) {
        String url = fileStorageService.getMediumUrl(publicId);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .message("Success")
                .data(url)
                .build());
    }

    @GetMapping("/url/webp")
    @Operation(summary = "Get WebP URL for a Cloudinary public ID")
    public ResponseEntity<ApiResponse<String>> getWebpUrl(
            @RequestParam String publicId) {
        String url = fileStorageService.getWebpUrl(publicId);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .message("Success")
                .data(url)
                .build());
    }
}