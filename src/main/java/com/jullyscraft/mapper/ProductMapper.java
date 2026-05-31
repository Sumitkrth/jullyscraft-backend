package com.jullyscraft.mapper;

import com.jullyscraft.dto.response.ProductImageResponse;
import com.jullyscraft.dto.response.ProductResponse;
import com.jullyscraft.dto.response.ProductSummaryResponse;
import com.jullyscraft.dto.response.ProductVariantResponse;
import com.jullyscraft.entity.Product;
import com.jullyscraft.entity.ProductImage;
import com.jullyscraft.entity.ProductSpecification;
import com.jullyscraft.entity.ProductVariant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    // ── Full response ─────────────────────────────────────────────────────────
    @Mapping(target = "categoryId",      source = "category.id")
    @Mapping(target = "categoryName",    source = "category.name")
    @Mapping(target = "discountPercent", expression = "java(product.getDiscountPercent())")
    @Mapping(target = "effectivePrice",  expression = "java(product.getEffectivePrice())")
    @Mapping(target = "inStock",         expression = "java(product.isInStock())")
    @Mapping(target = "lowStock",        expression = "java(product.isLowStock())")
    @Mapping(target = "specifications",  expression = "java(mapSpecs(product.getSpecifications()))")
    ProductResponse toResponse(Product product);

    // ── Summary (list / search results) ──────────────────────────────────────
    @Mapping(target = "categoryName",    source = "category.name")
    @Mapping(target = "discountPercent", expression = "java(product.getDiscountPercent())")
    @Mapping(target = "inStock",         expression = "java(product.isInStock())")
    @Mapping(target = "primaryImageUrl", expression = "java(extractPrimaryImage(product))")
    ProductSummaryResponse toSummary(Product product);

    // ── Variant ───────────────────────────────────────────────────────────────
    @Mapping(target = "inStock",  expression = "java(variant.isInStock())")
    @Mapping(target = "lowStock", expression = "java(variant.isLowStock())")
    ProductVariantResponse toVariantResponse(ProductVariant variant);

    // ── Image ─────────────────────────────────────────────────────────────────
    ProductImageResponse toImageResponse(ProductImage image);

    // ── Helpers ───────────────────────────────────────────────────────────────
    default String extractPrimaryImage(Product product) {
        return product.getImages().stream()
                .filter(ProductImage::isPrimaryImage)
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElse(product.getImages().isEmpty()
                        ? null
                        : product.getImages().get(0).getImageUrl());
    }

    default List<Map<String, String>> mapSpecs(List<ProductSpecification> specs) {
        if (specs == null) return List.of();
        return specs.stream()
                .map(s -> Map.of("key", s.getSpecKey(), "value", s.getSpecValue()))
                .toList();
    }
}