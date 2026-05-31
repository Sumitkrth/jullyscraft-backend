package com.jullyscraft.service;

import com.jullyscraft.dto.response.StructuredDataResponse;

public interface SeoService {
    String              generateSitemap();
    StructuredDataResponse getProductStructuredData(Long productId);
    StructuredDataResponse getCategoryStructuredData(Long categoryId);
    StructuredDataResponse getOrganizationStructuredData();
    String              generateRobotsTxt();
    String              generateCanonicalUrl(String path);
}