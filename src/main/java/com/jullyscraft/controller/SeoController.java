package com.jullyscraft.controller;

import com.jullyscraft.dto.response.ApiResponse;
import com.jullyscraft.dto.response.StructuredDataResponse;
import com.jullyscraft.service.SeoService;
import com.jullyscraft.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "SEO", description = "Sitemap, structured data, robots.txt")
public class SeoController {

    private final SeoService seoService;

    @GetMapping(value = "/sitemap.xml",
            produces = MediaType.APPLICATION_XML_VALUE)
    @Operation(summary = "XML Sitemap for search engines")
    public ResponseEntity<String> sitemap() {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(seoService.generateSitemap());
    }

    @GetMapping(value = "/robots.txt",
            produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "robots.txt for crawlers")
    public ResponseEntity<String> robotsTxt() {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(seoService.generateRobotsTxt());
    }

    @GetMapping(AppConstants.API_BASE + "/seo/structured-data/product/{productId}")
    @Operation(summary = "JSON-LD structured data for a product page")
    public ResponseEntity<ApiResponse<StructuredDataResponse>> productData(
            @PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(
                seoService.getProductStructuredData(productId)));
    }

    @GetMapping(AppConstants.API_BASE + "/seo/structured-data/category/{categoryId}")
    @Operation(summary = "JSON-LD breadcrumb for a category page")
    public ResponseEntity<ApiResponse<StructuredDataResponse>> categoryData(
            @PathVariable Long categoryId) {
        return ResponseEntity.ok(ApiResponse.success(
                seoService.getCategoryStructuredData(categoryId)));
    }

    @GetMapping(AppConstants.API_BASE + "/seo/structured-data/organization")
    @Operation(summary = "JSON-LD organization data")
    public ResponseEntity<ApiResponse<StructuredDataResponse>> orgData() {
        return ResponseEntity.ok(ApiResponse.success(
                seoService.getOrganizationStructuredData()));
    }
}